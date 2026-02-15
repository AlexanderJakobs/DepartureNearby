package vsp.client;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.*;

import java.util.UUID;

/**
 * gRPC Client für den GeocodingService in externalRest.
 * Ersetzt den direkten Aufruf von Nominatim.
 */
@Component
public class GeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GeocodingClient.class);

    @GrpcClient("externalrest")
    private GeocodingServiceGrpc.GeocodingServiceBlockingStub geocodingStub;

    /**
     * Ermittelt Coordinates für eine Address via externalRest GeocodingService.
     *
     * @param address Die Address mit Straße, Hausnummer und optional Stadt
     * @return Die aufgelösten Coordinates
     * @throws GeocodingException wenn die Adresse nicht gefunden wird
     */
    public Coordinates getCoordinatesForAddress(Address address) {
        String correlationId = UUID.randomUUID().toString();

        log.debug("[{}] Geocoding address via externalRest: {} {}",
                correlationId, address.getStreet(), address.getHouseNumber());

        GeocodeRequest request = GeocodeRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder()
                        .setCorrelationId(correlationId)
                        .setCaller("locationhandler")
                        .build())
                .setAddress(address)
                .build();

        try {
            GeocodeResponse response = geocodingStub.geocode(request);

            if (response.hasError()) {
                ErrorStatus error = response.getError();
                log.warn("[{}] Geocoding failed: {} - {}",
                        correlationId, error.getCode(), error.getMessage());
                throw new GeocodingException(error.getMessage());
            }

            Coordinates coordinates = response.getCoordinates();
            log.debug("[{}] Geocoding result: lat={}, lon={}",
                    correlationId, coordinates.getLatitude(), coordinates.getLongitude());

            return coordinates;

        } catch (StatusRuntimeException e) {
            log.error("[{}] gRPC error calling GeocodingService: {}", correlationId, e.getStatus());
            throw new GeocodingException("Geocoding service unavailable: " + e.getStatus().getDescription());
        }
    }

    public static class GeocodingException extends RuntimeException {
        public GeocodingException(String message) {
            super(message);
        }
    }
}
