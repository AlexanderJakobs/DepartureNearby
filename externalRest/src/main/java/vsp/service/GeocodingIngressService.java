package vsp.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vsp.*;
import vsp.client.NominatimClient;
import java.time.Instant;

/**
 * gRPC Service Implementation für Geocoding (Nominatim API).
 * Wird von locationhandler aufgerufen.
 */
@GrpcService
public class GeocodingIngressService extends GeocodingServiceGrpc.GeocodingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GeocodingIngressService.class);

    private final NominatimClient nominatimClient;

    public GeocodingIngressService(NominatimClient nominatimClient) {
        this.nominatimClient = nominatimClient;
        log.info("GeocodingServiceImpl initialized");
    }

    @Override
    public void geocode(GeocodeRequest request, StreamObserver<GeocodeResponse> responseObserver) {
        String correlationId = request.hasMeta() ? request.getMeta().getCorrelationId() : "unknown";
        log.info("[{}] Geocode request for address: {} {}",
                correlationId,
                request.getAddress().getStreet(),
                request.getAddress().getHouseNumber());

        try {
            Coordinates coordinates = nominatimClient.getCoordinatesForAddress(request.getAddress());

            GeocodeResponse response = GeocodeResponse.newBuilder()
                    .setCoordinates(coordinates)
                    .setResultMeta(ResultMeta.newBuilder()
                            .setGeneratedAt(Timestamp.newBuilder()
                                    .setSeconds(Instant.now().getEpochSecond())
                                    .build())
                            .setSource("Nominatim/OpenStreetMap")
                            .build())
                    .build();

            log.info("[{}] Geocode success: lat={}, lon={}",
                    correlationId, coordinates.getLatitude(), coordinates.getLongitude());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NominatimClient.GeocodingException e) {
            log.warn("[{}] Geocoding failed: {}", correlationId, e.getMessage());

            GeocodeResponse errorResponse = GeocodeResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.NOT_FOUND)
                            .setMessage("Address not found")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("[{}] Geocoding error: {}", correlationId, e.getMessage(), e);

            GeocodeResponse errorResponse = GeocodeResponse.newBuilder()
                    .setError(ErrorStatus.newBuilder()
                            .setCode(ErrorStatus.Code.INTERNAL)
                            .setMessage("Internal geocoding error")
                            .setDetails(e.getMessage())
                            .build())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}