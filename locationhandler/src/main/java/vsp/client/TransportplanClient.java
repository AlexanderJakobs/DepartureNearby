package vsp.client;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.*;

import java.util.UUID;

/**
 * gRPC Client für den TransportplanIngress Service.
 * Sendet Coordinates an Transportplan zur Abfahrtsabfrage.
 */
@Component
public class TransportplanClient {

    private static final Logger log = LoggerFactory.getLogger(TransportplanClient.class);

    @GrpcClient("transportplan")
    private TransportplanIngressGrpc.TransportplanIngressStub asyncStub;

    /**
     * Sendet Coordinates an Transportplan (ASYNCHRON).
     * Fire-and-Forget: Wartet NICHT auf Antwort, kehrt sofort zurück.
     *
     * @param coordinates Die Koordinaten für die Abfahrtssuche
     */
    public void sendCoordinates(Coordinates coordinates) {
        sendCoordinates(coordinates, null);
    }

    /**
     * Sendet Coordinates an Transportplan (ASYNCHRON).
     *
     * @param coordinates Die Koordinaten für die Abfahrtssuche
     * @param correlationId Correlation-ID für Tracing (optional, wird generiert falls null)
     */
    public void sendCoordinates(Coordinates coordinates, String correlationId) {
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        log.info("[{}] Sending coordinates to Transportplan: lat={}, lon={}",
                finalCorrelationId, coordinates.getLatitude(), coordinates.getLongitude());

        try {
            GetDeparturesRequest request = GetDeparturesRequest.newBuilder()
                    .setMeta(RequestMeta.newBuilder()
                            .setCorrelationId(finalCorrelationId)
                            .setCaller("locationhandler")
                            .build())
                    .setCoordinates(coordinates)
                    .build();

            // Fork context to prevent cancellation when parent gRPC call completes
            Context forkedContext = Context.current().fork();

            // Run async call in forked context
            forkedContext.run(() -> {
                asyncStub.getDepartures(request, new StreamObserver<Ack>() {
                    @Override
                    public void onNext(Ack ack) {
                        log.info("[{}] Transportplan acknowledged request at: {}",
                                finalCorrelationId,
                                ack.hasAcceptedAt() ? ack.getAcceptedAt().getSeconds() : "N/A");
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("[{}] Error calling Transportplan: {}", finalCorrelationId, t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        log.debug("[{}] Transportplan call completed", finalCorrelationId);
                    }
                });
            });

            log.debug("[{}] Request sent to Transportplan, continuing without waiting",
                    finalCorrelationId);

        } catch (Exception e) {
            log.error("[{}] Error sending coordinates to Transportplan", finalCorrelationId, e);
        }
    }
}