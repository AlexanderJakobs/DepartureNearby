package vsp.client;

import java.time.Instant;
import java.util.List;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import vsp.Ack;
import vsp.DepartureStation;
import vsp.DisplaymanagerIngressGrpc;
import vsp.RequestMeta;
import vsp.ShowDeparturesRequest;
import vsp.TransportplanController;

@Component
public class DisplayClient {
    
    private static final Logger log = LoggerFactory.getLogger(DisplayClient.class);

    @GrpcClient("displaymanager")
    private DisplaymanagerIngressGrpc.DisplaymanagerIngressStub asyncStub;


    /**
     * Sendet DepartureStations mit Timestamp an den DisplayManager via gRPC.
     */
    public void sendDeparturesToDisplayManager(List<DepartureStation> stations, String correlationId) {
        try {
            RequestMeta meta = RequestMeta.newBuilder()
                    .setCorrelationId(correlationId)
                    .setCaller("Transportplan")
                    .build();

            ShowDeparturesRequest.Builder requestBuilder = ShowDeparturesRequest.newBuilder()
                    .setMeta(meta)
                    .addAllStations(stations);

            ShowDeparturesRequest request = requestBuilder.build();

            log.info("Sending {} stations to DisplayManager, correlationId={}",
                    stations.size(), correlationId);

            // Fork context to prevent cancellation when parent gRPC call completes!!
            Context forkedContext = Context.current().fork();
            forkedContext.run(() ->{
                asyncStub.showDepartures(request, new StreamObserver<Ack>() {
                    @Override
                    public void onNext(vsp.Ack ack) {
                        log.info("Received Ack from DisplayManager at: {} [correlationId={}]",
                                ack.hasAcceptedAt() ? formatTimestamp(ack.getAcceptedAt()) : "N/A",
                                correlationId);
                    }
    
                    @Override
                    public void onError(Throwable t) {
                        log.error("Error from DisplayManager [correlationId={}]: {}",
                                correlationId, t.getMessage());
    
                        // ErrorStatus aus Metadata extrahieren
                        if (t instanceof io.grpc.StatusRuntimeException sre) {
                            io.grpc.Metadata metadata = sre.getTrailers();
                            if (metadata != null) {
                                extractErrorStatus(metadata, correlationId);
                            }
                        }
                    }
    
                    @Override
                    public void onCompleted() {
                        log.debug("DisplayManager call completed [correlationId={}]",
                                correlationId);
                    }
                });
            });
            // Methode kehrt SOFORT zurück - wartet NICHT auf Antwort!
            log.debug("Request sent, continuing without waiting for response [correlationId={}]",
                    correlationId);

        } catch (Exception e) {
            log.error("Failed to send departures to DisplayManager, correlationId={}",
                    correlationId, e);
            throw new RuntimeException("Failed to send to DisplayManager: " + e.getMessage(), e);
        }
    }
    /**
     * Extrahiert ErrorStatus aus gRPC Metadata
     */
    private void extractErrorStatus(io.grpc.Metadata metadata, String correlationId) {
        try {
            io.grpc.Metadata.Key<byte[]> errorKey =
                    io.grpc.Metadata.Key.of("error-details-bin",
                            io.grpc.Metadata.BINARY_BYTE_MARSHALLER);

            byte[] errorBytes = metadata.get(errorKey);
            if (errorBytes != null) {
                vsp.ErrorStatus errorStatus = vsp.ErrorStatus.parseFrom(errorBytes);
                log.error("Structured error from Locationhandler [correlationId={}]: Code={}, Message={}, Details={}",
                        correlationId,
                        errorStatus.getCode(),
                        errorStatus.getMessage(),
                        errorStatus.getDetails());
            }
        } catch (Exception e) {
            log.debug("Could not extract ErrorStatus from metadata [correlationId={}]",
                    correlationId, e);
        }
    }
        /**
     * Helper: Formatiert Timestamp für Logging
     */
    private String formatTimestamp(com.google.protobuf.Timestamp timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return instant.toString();
    }
}
