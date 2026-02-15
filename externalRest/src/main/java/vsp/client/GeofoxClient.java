package vsp.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.Coordinates;
import vsp.Departure;
import vsp.DepartureStation;
import vsp.app.ExternalRestApplicationConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HTTP-Client für die Geofox API (HVV).
 * Sendet Anfragen an hvv.
 * Verwendet HMAC-SHA1 Authentifizierung.
 */
@Component
public class GeofoxClient {

    private static final Logger log = LoggerFactory.getLogger(GeofoxClient.class);
    private static final String HMAC_SHA1 = "HmacSHA1";

    // Konfiguration
    private static final int MAX_DISTANCE_METERS = 1000;  // Nicht weiter als


    private final String baseUrl;
    private final String apiUser;
    private final String apiPassword;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int timeout;
    private Timestamp lastInteraction;

    public GeofoxClient(ExternalRestApplicationConfig config) {
        this.baseUrl = config.getExternalApis().getGeofoxBaseUrl();
        this.apiUser = config.getExternalApis().getGeofoxApiUser();
        this.apiPassword = config.getExternalApis().getGeofoxApiPassword();
        this.timeout = config.getExternalApis().getGeofoxTimeout();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        this.objectMapper = new ObjectMapper();

        this.lastInteraction = null;

        log.info("GeofoxClient initialized with baseUrl={}, user={}, timeout={}ms",
                baseUrl, apiUser, timeout);
    }


    public List<DepartureStation> findNearbyStations(Coordinates coordinates, int maxStations) throws Exception {

        Map<String, Object> request = Map.of(
                "version", 63,
                "theName", Map.of(
                        "type", "STATION",
                        "coordinate", Map.of(
                                "x", coordinates.getLongitude(),
                                "y", coordinates.getLatitude()
                        )
                ),
                "maxDistance", MAX_DISTANCE_METERS,
                "maxList", maxStations,
                "coordinateType", "EPSG_4326",
                "allowTypeSwitch", false
        );

        CheckNameResponse response = sendRequest("/checkName", request, CheckNameResponse.class);

        if (response.results == null || response.results.isEmpty()) {
            return List.of();
        }

        List<DepartureStation> stations = new ArrayList<>();

        for (RegionalSDName r : response.results) {
            stations.add(
                    DepartureStation.newBuilder()
                            .setStationId(r.id)
                            .setStationName(r.name)
                            .setDistance(r.distance) // direkt aus API!
                            .build()
            );
        }

        return stations;
    }


    public List<DepartureStation> getDeparturesForStations(List<DepartureStation> originalStations) throws Exception {

        LocalDateTime queryTime = LocalDateTime.now().plusMinutes(1);

        // --- Meta aus originalStations: stationId -> (name, distance) + Reihenfolge merken
        Map<String, String> idToName = new HashMap<>();
        Map<String, Double> idToDistance = new HashMap<>();
        List<String> requestedOrder = new ArrayList<>();

        for (DepartureStation s : originalStations) {
            String id = s.getStationId();
            requestedOrder.add(id);

            idToName.putIfAbsent(id, s.getStationName());
            idToDistance.putIfAbsent(id, s.getDistance());
        }

        // --- Request bauen
        List<Map<String, Object>> stationList = new ArrayList<>();
        for (DepartureStation s : originalStations) {
            stationList.add(Map.of(
                    "name", s.getStationName(),
                    "id", s.getStationId(),
                    "type", "STATION"
            ));
        }

        Map<String, Object> request = Map.of(
                "version", 63,
                "stations", stationList,
                "time", Map.of(
                        "date", queryTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "time", queryTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                ),
                "maxList", 30,
                "maxTimeOffset", 200,
                "useRealtime", true
        );

        DepartureListResponse response =
                sendRequest("/departureList", request, DepartureListResponse.class);

        if (response.departures == null || response.departures.isEmpty()) {
            log.info("No departures found for given stations");
            return List.of();
        }

        // --- Gruppierung nach stationId (statt stationName)
        Map<String, List<Departure>> departuresByStationId = new HashMap<>();

        for (DepartureInfo dep : response.departures) {

            if (dep.station == null || dep.station.id == null || dep.station.id.isBlank()) {
                continue;
            }

            String stationId = dep.station.id;

            String lineName = "Unbekannt";
            if (dep.line != null && dep.line.name != null) {
                String dir = dep.line.direction != null ? dep.line.direction : "";
                lineName = dir.isBlank() ? dep.line.name : dep.line.name + " " + dir;
            }

            int offset = (dep.timeOffset != null) ? dep.timeOffset : 0;
            Timestamp departureTime = parseGeofoxTime(offset);

            Departure departure = Departure.newBuilder()
                    .setLineName(lineName)
                    .setDepartureTime(departureTime)
                    .build();

            departuresByStationId
                    .computeIfAbsent(stationId, k -> new ArrayList<>())
                    .add(departure);
        }

        // --- Ergebnisliste in der Reihenfolge von originalStations bauen
        List<DepartureStation> resultList = new ArrayList<>();

        for (String stationId : requestedOrder) {
            List<Departure> deps = departuresByStationId.get(stationId);
            if (deps == null || deps.isEmpty()) {
                continue;
            }

            String name = idToName.getOrDefault(stationId, "Unbekannt");

            double distance = idToDistance.getOrDefault(stationId, 0.0d);

            resultList.add(
                    DepartureStation.newBuilder()
                            .setStationId(stationId)
                            .setStationName(name)
                            .setDistance(distance)
                            .addAllDepartures(deps)
                            .build()
            );
        }

        log.info("Found {} stations with departures (grouped by stationId)", resultList.size());
        return resultList;
    }



    public <T> T sendRequest(String endpoint, Map<String, Object> body, Class<T> responseType) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);
        String signature = generateSignature(jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Accept", "application/json;charset=UTF-8")
                .header("geofox-auth-user", apiUser)
                .header("geofox-auth-signature", signature)
                .header("X-Platform", "web")
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        log.debug("Sending request to {}: {}", endpoint, jsonBody);


        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            log.error("Geofox API error: status={}, body={}", response.statusCode(), response);
            throw new RuntimeException("Geofox API returned status " + response.statusCode());
        }

        byte[] raw = response.body().readAllBytes();

        String json = new String(raw, StandardCharsets.UTF_8);

        // Always update the Timestamp 'lastInteraction' when data is received
        Instant instant = Instant.now();
        this.lastInteraction = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond()).
                setNanos(instant.getNano()).
                build();

        return objectMapper.readValue(json, responseType);
    }

    private String generateSignature(String body) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA1);
        SecretKeySpec secretKey = new SecretKeySpec(
                apiPassword.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA1
        );
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private Timestamp parseGeofoxTime(Integer timeOffset) {
        Instant departureInstant = Instant.now().plusSeconds(timeOffset * 60L);
        return Timestamp.newBuilder()
                .setSeconds(departureInstant.getEpochSecond())
                .setNanos(departureInstant.getNano())
                .build();
    }

    // For statusUpdate-Call
    public Timestamp getLastInteraction(){
        return this.lastInteraction;
    }

    // --- Inner classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CheckNameResponse {
        @JsonProperty("results")
        public List<RegionalSDName> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RegionalSDName {
        public String id;
        public String name;
        public Double distance; // Meter
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StationResult {
        @JsonProperty("id")
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DepartureListResponse {
        @JsonProperty("departures")
        public List<DepartureInfo> departures;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DepartureInfo {
        @JsonProperty("line")
        public LineInfo line;
        @JsonProperty("timeOffset")
        public Integer timeOffset;
        @JsonProperty("station")
        public StationResult station;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LineInfo {
        @JsonProperty("name")
        public String name;
        @JsonProperty("direction")
        public String direction;
    }

    // Custom Exception
    public static class GeofoxApiException extends RuntimeException {
        public GeofoxApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}