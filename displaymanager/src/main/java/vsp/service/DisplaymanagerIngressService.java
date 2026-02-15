package vsp.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vsp.*;
import vsp.controller.DisplayController;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * gRPC Service Implementation für DisplayManager
 * Dieser Service empfängt gRPC-Calls von Transportplan
 * und leitet sie an den DisplayController weiter
 */
@GrpcService
public class DisplaymanagerIngressService extends DisplaymanagerIngressGrpc.DisplaymanagerIngressImplBase {

    private static final Logger log = LoggerFactory.getLogger(DisplaymanagerIngressService.class);

    private final DisplayController displayController;

    @Autowired
    public DisplaymanagerIngressService(DisplayController displayController) {
        this.displayController = displayController;
        log.info("DisplaymanagerIngressService initialized");
    }

    /**
     * gRPC Endpoint: userPassLocation
     * Wird vom Externalrest aufgerufen
     * @param request UserPassLocationRequest mit RequestMeta und Address
     * @param responseObserver StreamObserver für die Ack-Antwort
     */
    @Override
    public void userPassLocation(ExternalInput request,
                                 StreamObserver<Ack> responseObserver) {
        log.info("=== Received gRPC call: userPassLocation ===");
        try {
            // Addresse extrahieren
            String address = request.getAddress();
            log.info("Request contains address: {}", address);
 
            // Ack mit Timestamp erstellen
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            Ack ack = Ack.newBuilder()
                    .setAcceptedAt(timestamp)
                    .build();

            
            // Zuerst Ack zurück
            responseObserver.onNext(ack);
            responseObserver.onCompleted();

            // An Controller weiterleiten ABER in SEPARATEM THREAD
            // so ist die Weiterleitung unabhängig vom schon existierendem gRPC-Kontext
            CompletableFuture.runAsync(() -> {
                try {
                    displayController.userPassLocation(address);
                } catch (Exception e) {
                    log.error("Error in async processing (ACK already sent)", e);
                }
            });

            log.info("Successfully processed userPassLocation request");
        } catch (IllegalArgumentException e) {
            log.error("Invalid request in userPassLocation: {}", e.getMessage());
            log.debug("Exception details:", e);
            sendErrorResponse(responseObserver,
                    vsp.ErrorStatus.Code.INVALID_ARGUMENT,
                    "Invalid request",
                    e.getMessage());

        } catch (Exception e) {
            log.error("Error processing userPassLocation request: {}", e.getMessage(), e);
            sendErrorResponse(responseObserver,
                    vsp.ErrorStatus.Code.INTERNAL,
                    "Internal server error",
                    e.getMessage());
        }
    }

    /**
     * gRPC Endpoint: showDepartures
     * Wird vom Transportplan aufgerufen (Direct Response Pattern)
     * @param request ShowDeparturesRequest mit RequestMeta und Liste von Stations
     * @param responseObserver StreamObserver für die Ack-Antwort
     */
    @Override
    public void showDepartures(ShowDeparturesRequest request,
                               StreamObserver<Ack> responseObserver) {
        log.info("=== Received gRPC call: showDepartures ===");
        try {
            // Stationen extrahieren
            List<DepartureStation> stations = request.getStationsList();
            log.info("Request contains {} station(s)", stations.size());

            // Validierung
            if (stations.isEmpty()) {
                log.warn("Empty stations list received");
                sendErrorResponse(responseObserver,
                        vsp.ErrorStatus.Code.INVALID_ARGUMENT,
                        "Stations list is empty",
                        "At least one station is required");
                return;
            }

            // Ack mit Timestamp erstellen
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            Ack ack = Ack.newBuilder()
                    .setAcceptedAt(timestamp)
                    .build();

            responseObserver.onNext(ack);
            responseObserver.onCompleted();
            

            // Timestamp extrahieren (wann die Daten von Geofox geholt wurden)
            Timestamp dataFetchedAt = request.hasDataFetchedAt() ? request.getDataFetchedAt() : null;

            // An Controller weiterleiten
            CompletableFuture.runAsync(() -> {
                try {
                    displayController.displayDepartures(stations, dataFetchedAt);
                } catch (Exception e) {
                    log.error("Error in async processing (ACK already sent)", e);
                }
            });


            log.info("Successfully processed showDepartures request");
        } catch (IllegalArgumentException e) {
            log.error("Invalid request in showDepartures: {}", e.getMessage());
            log.debug("Exception details:", e);
            sendErrorResponse(responseObserver,
                    vsp.ErrorStatus.Code.INVALID_ARGUMENT,
                    "Invalid request",
                    e.getMessage());

        } catch (Exception e) {
            log.error("Error processing showDepartures request: {}", e.getMessage(), e);
            sendErrorResponse(responseObserver,
                    vsp.ErrorStatus.Code.INTERNAL,
                    "Internal server error",
                    e.getMessage());
        }
    }
    /**
     * Helper-Methode: Sendet strukturierte Fehler-Antwort mit ErrorStatus
     *
     * @param responseObserver StreamObserver für die Antwort
     * @param errorCode ErrorStatus.Code (z.B. INVALID_ARGUMENT, INTERNAL)
     * @param message Kurze Fehlerbeschreibung
     * @param details Detaillierte Fehlerinformation
     */
    private void sendErrorResponse(StreamObserver<vsp.Ack> responseObserver,
                                   vsp.ErrorStatus.Code errorCode,
                                   String message,
                                   String details) {
        log.error("Sending error response: {} - {} - {}", errorCode, message, details);

        // ErrorStatus erstellen
        vsp.ErrorStatus errorStatus = vsp.ErrorStatus.newBuilder()
                .setCode(errorCode)
                .setMessage(message)
                .setDetails(details != null ? details : "")
                .build();

        // gRPC Status mit ErrorStatus als Metadata
        io.grpc.Metadata metadata = new io.grpc.Metadata();
        io.grpc.Metadata.Key<byte[]> errorKey =
                io.grpc.Metadata.Key.of("error-details-bin",
                        io.grpc.Metadata.BINARY_BYTE_MARSHALLER);
        metadata.put(errorKey, errorStatus.toByteArray());

        // Map ErrorStatus.Code zu gRPC Status Code
        io.grpc.Status grpcStatus = mapErrorCodeToGrpcStatus(errorCode)
                .withDescription(message + ": " + details);

        // Fehler mit Metadata senden
        responseObserver.onError(grpcStatus.asRuntimeException(metadata));

        log.debug("Error response sent to client");
    }

    /**
     * Mapped ErrorStatus.Code zu entsprechendem gRPC Status
     */
    private io.grpc.Status mapErrorCodeToGrpcStatus(vsp.ErrorStatus.Code errorCode) {
        switch (errorCode) {
            case INVALID_ARGUMENT:
                return io.grpc.Status.INVALID_ARGUMENT;
            case NOT_FOUND:
                return io.grpc.Status.NOT_FOUND;
            case TIMEOUT:
                return io.grpc.Status.DEADLINE_EXCEEDED;
            case UNAVAILABLE:
                return io.grpc.Status.UNAVAILABLE;
            case RATE_LIMITED:
                return io.grpc.Status.RESOURCE_EXHAUSTED;
            case INTERNAL:
                return io.grpc.Status.INTERNAL;
            case CODE_UNSPECIFIED:
            default:
                return io.grpc.Status.UNKNOWN;
        }
    }
}
