package vsp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import vsp.Address;
import vsp.Coordinates;
import vsp.app.ExternalRestApplicationConfig;

import java.time.Duration;

/**
 * HTTP-Client für die Nominatim Geocoding API (OpenStreetMap).
 * Wandelt Address in Coordinates um.
 */
@Component
public class NominatimClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public NominatimClient(ExternalRestApplicationConfig config) {
        String baseUrl = config.getExternalApis().getNominatimBaseUrl();
        int timeoutMs = config.getExternalApis().getNominatimTimeout();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "VSP-Departure-System/1.0")
                .build();
        this.timeout = Duration.ofMillis(timeoutMs);
        log.info("NominatimClient initialized with baseUrl={}, timeout={}ms", baseUrl, timeoutMs);
    }

    /**
     * Ermittelt Coordinates für eine Address via Nominatim API.
     *
     * @param address Die Address mit Straße, Hausnummer und optional Stadt
     * @return Die aufgelösten Coordinates
     * @throws GeocodingException wenn die Adresse nicht gefunden wird
     */
    public Coordinates getCoordinatesForAddress(Address address) {
        String query = buildSearchQuery(address);
        log.debug("Geocoding address: {}", query);

        NominatimResponse[] results = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .bodyToMono(NominatimResponse[].class)
                .timeout(timeout)
                .block();

        if (results == null || results.length == 0) {
            throw new GeocodingException("Address not found: " + query);
        }

        NominatimResponse result = results[0];

        Coordinates coordinates = Coordinates.newBuilder()
                .setLatitude(Double.parseDouble(result.lat()))
                .setLongitude(Double.parseDouble(result.lon()))
                .build();

        log.debug("Geocoding result: {} -> (lat={}, lon={})",
                query, coordinates.getLatitude(), coordinates.getLongitude());
        return coordinates;
    }

    /**
     * Baut die Suchanfrage aus der Address.
     */
    private String buildSearchQuery(Address address) {
        StringBuilder query = new StringBuilder();
        query.append(address.getStreet());
        query.append(" ").append(address.getHouseNumber());

        if (!address.getCity().isEmpty()) {
            query.append(", ").append(address.getCity());
        }
        if (!address.getCountry().isEmpty()) {
            query.append(", ").append(address.getCountry());
        }
        return query.toString();
    }

    // Response DTO für Nominatim API
    private record NominatimResponse(
            String lat,
            String lon,
            String display_name
    ) {}

    // Custom Exception
    public static class GeocodingException extends RuntimeException {
        public GeocodingException(String message) {
            super(message);
        }
    }
}