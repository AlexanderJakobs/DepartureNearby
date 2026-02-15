package vsp.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import vsp.*;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * gRPC Service Implementation für LocationhandlerIngress.
 * Empfängt UserPassLocationRequest vom DisplayManager.
 */
@GrpcService
public class LocationhandlerIngressService extends LocationhandlerIngressGrpc.LocationhandlerIngressImplBase {

    private static final Logger log = LoggerFactory.getLogger(LocationhandlerIngressService.class);

    private final LocationController locationController;

    @Autowired
    public LocationhandlerIngressService(LocationController locationController) {
        this.locationController = locationController;
        log.info("LocationhandlerIngressImpl initialized");
    }

    @Override
    public void userPassLocation(UserPassLocationRequest request, StreamObserver<Ack> responseObserver) {
        log.info("=== Received gRPC call: userPassLocation ===");

        try {
            String correlationId = request.hasMeta() ? request.getMeta().getCorrelationId() : "unknown";
            Address address = request.getAddress();

            log.info("[{}] gRPC userPassLocation called: street={}, houseNumber={}, city={}",
                    correlationId,
                    address.getStreet(),
                    address.getHouseNumber(),
                    address.getCity());


            // Sofortiges ACK zurück an DisplayManager
            Ack ack = Ack.newBuilder()
                    .setAcceptedAt(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            responseObserver.onNext(ack);
            responseObserver.onCompleted();

            log.debug("[{}] ACK sent to DisplayManager", correlationId);


            // Delegiere an Controller, aber in neuem Thread
            CompletableFuture.runAsync(() -> {
                try {
                    locationController.onResolveLocationRequest(address);
                } catch (Exception e) {
                    log.error("Error in async processing (ACK already sent)", e);
                }
            });
            

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("Error processing userPassLocation request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}