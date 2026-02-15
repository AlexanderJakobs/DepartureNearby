package vsp.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC Client für den DeparturesService in externalRest.
 * Ersetzt den direkten Aufruf von Geofox.
 */
@Component
public class DeparturesClient {

    private static final Logger log = LoggerFactory.getLogger(DeparturesClient.class);

    @GrpcClient("externalrest")
    private DeparturesServiceGrpc.DeparturesServiceBlockingStub departuresStub;

    /**
     * Ermittelt Abfahrten für Coordinates via externalRest DeparturesService.
     *
     * @param coordinates Die Koordinaten für die Suche
     * @return Liste von DepartureStation mit Abfahrten
     * @throws DeparturesException wenn die Abfrage fehlschlägt
     */
    public List<DepartureStation> getNearbyStations(Coordinates coordinates) {
        return getNearbyStations(coordinates, 3);
    }


    /**
     * Ermittelt Abfahrten für Coordinates via externalRest DeparturesService.
     *
     * @param coordinates Die Koordinaten für die Suche
     * @param maxStations Maximale Anzahl der Stationen
     * @return DeparturesResult mit Stationen und Timestamp
     * @throws DeparturesException wenn die Abfrage fehlschlägt
     */
    public List<DepartureStation> getNearbyStations(Coordinates coordinates, int maxStations) {
        String correlationId = UUID.randomUUID().toString();

        log.debug("[{}] Getting stations via externalRest: lat={}, lon={}, maxStations={}",
                correlationId, coordinates.getLatitude(), coordinates.getLongitude(), maxStations);

        GetNearbyStationsRequest request = GetNearbyStationsRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder()
                        .setCorrelationId(correlationId)
                        .setCaller("transportplan")
                        .build())
                .setCoordinates(coordinates)
                .setMaxStations(maxStations)
                .build();

        try {
            GetNearbyStationsResponse response = departuresStub.getNearbyStations(request);

            if (response.hasError()) {
                ErrorStatus error = response.getError();
                log.warn("[{}] Stations request failed: {} - {}",
                        correlationId, error.getCode(), error.getMessage());
                throw new DeparturesException(error.getMessage());
            }

            List<DepartureStation> stations = response.getStations().getStationsList();
                    ;
            log.debug("[{}] Got {} stations",
                    correlationId, stations.size());

            return stations;

        } catch (StatusRuntimeException e) {
            log.error("[{}] gRPC error calling DeparturesService: {}", correlationId, e.getStatus());
            throw new DeparturesException("Departures service unavailable: " + e.getStatus().getDescription());
        }
    }


    public List<DepartureStation> getDepartures(List<DepartureStation> stations) {
        String correlationId = UUID.randomUUID().toString();
        log.debug("[{}] Getting departures via externalRest", correlationId);

        GetDeparturesForStationsRequest request = GetDeparturesForStationsRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder()
                        .setCorrelationId(correlationId)
                        .setCaller("transportplan")
                        .build()).
                addAllStations(stations).
                build();

        try {
            GetDeparturesForStationsResponse response = departuresStub.getDeparturesForStations(request);
            if (response.hasError()) {
                ErrorStatus error = response.getError();
                log.warn("[{}] Departures request failed: {} - {}",
                        correlationId, error.getCode(), error.getMessage());
                throw new DeparturesException(error.getMessage());
            }
            List<DepartureStation> stationsList = response.getStations().getStationsList();
            log.debug("[{}] Got {} stations",
                    correlationId, stationsList.size());

            return stationsList;

        } catch (StatusRuntimeException e) {
            log.error("[{}] gRPC error calling DeparturesService: {}", correlationId, e.getStatus());
            throw new DeparturesException("Departures service unavailable: " + e.getStatus().getDescription());
        }

    }

    public static class DeparturesException extends RuntimeException {
        public DeparturesException(String message) {
            super(message);
        }
    }
}
