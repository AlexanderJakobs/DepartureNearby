package vsp.client;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.*;
import java.time.Instant;
import java.util.UUID;

@Component
public class DisplaymanagerClient {
    private static final Logger log = LoggerFactory.getLogger(DisplaymanagerClient.class);
    private static final String COMPONENT_NAME = "Externalrest";

    /**
     * Asynchroner gRPC Client Stub
     * Konfiguration via application.properties: grpc.client.displaymanager.address
     */
    @GrpcClient("displaymanager")
    private DisplaymanagerIngressGrpc.DisplaymanagerIngressStub asyncStub;
    /**
     * Sendet userPassLocation Request an Displaymanager (ASYNCHRON)
     *
     * Fire-and-Forget: Wartet NICHT auf Antwort, kehrt sofort zurück
     *
     * @param input the address
     */
    public void sendUserPassLocation(String input) {
        sendUserPassLocation(input, null, null);
    }

    /**
     * Sendet ExternalInput Request an Displaymanager (ASYNCHRON)
     *
     * @param input the address
     * @param correlationId Correlation-ID für Tracing (optional, wird generiert falls null)
     * @param sessionId Session-ID (optional)
     */
    public void sendUserPassLocation(String input,
                                     String correlationId,
                                     String sessionId) {
        // Generiere Correlation-ID falls nicht vorhanden
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        log.info("Sending userPassLocation to Displaymanager (async): {} [correlationId={}]",
                input, finalCorrelationId);

        try {

            // RequestMeta erstellen
            vsp.RequestMeta.Builder metaBuilder = vsp.RequestMeta.newBuilder()
                    .setCorrelationId(finalCorrelationId)
                    .setCaller(COMPONENT_NAME);

            // Optional: Session-ID hinzufügen
            if (sessionId != null && !sessionId.isEmpty()) {
                metaBuilder.setSessionId(sessionId);
            }

            vsp.RequestMeta meta = metaBuilder.build();

            // Request erstellen
            vsp.ExternalInput request = vsp.ExternalInput.newBuilder()
                    .setAddress(input)
                    .build();

            log.debug("Sending async request to Displaymanager [correlationId={}]",
                    finalCorrelationId);

            // Asynchroner Call - Fire and Forget!
            asyncStub.userPassLocation(request, new StreamObserver<Ack>() {
                @Override
                public void onNext(vsp.Ack ack) {
                    log.info("Received Ack from Displaymanager at: {} [correlationId={}]",
                            ack.hasAcceptedAt() ? formatTimestamp(ack.getAcceptedAt()) : "N/A",
                            finalCorrelationId);
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Error from Displaymanager [correlationId={}]: {}",
                            finalCorrelationId, t.getMessage());

                    // ErrorStatus aus Metadata extrahieren
                    if (t instanceof io.grpc.StatusRuntimeException) {
                        io.grpc.StatusRuntimeException sre = (io.grpc.StatusRuntimeException) t;
                        io.grpc.Metadata metadata = sre.getTrailers();
                        if (metadata != null) {
                            extractErrorStatus(metadata, finalCorrelationId);
                        }
                    }
                }

                @Override
                public void onCompleted() {
                    log.debug("Displaymanager call completed [correlationId={}]",
                            finalCorrelationId);
                }
            });

            // Methode kehrt SOFORT zurück - wartet NICHT auf Antwort!
            log.debug("Request sent, continuing without waiting for response [correlationId={}]",
                    finalCorrelationId);

        } catch (Exception e) {
            log.error("Error sending userPassLocation to Displaymanager [correlationId={}]",
                    finalCorrelationId, e);
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
                log.error("Structured error from Displaymanager [correlationId={}]: Code={}, Message={}, Details={}",
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
