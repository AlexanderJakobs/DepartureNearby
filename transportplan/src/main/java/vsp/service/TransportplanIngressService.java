package vsp.service;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vsp.*;

import java.time.Instant;

/**
 * gRPC Service Implementation für TransportplanIngress.
 * Empfängt GetDeparturesRequest vom Locationhandler.
 */
@GrpcService
public class TransportplanIngressService extends TransportplanIngressGrpc.TransportplanIngressImplBase {

    private static final Logger log = LoggerFactory.getLogger(TransportplanIngressService.class);

    private final TransportplanController transportplanController;

    public TransportplanIngressService(TransportplanController transportplanController) {
        this.transportplanController = transportplanController;
    }

    @Override
    public void getDepartures(GetDeparturesRequest request, StreamObserver<Ack> responseObserver) {
        String correlationId = request.getMeta().getCorrelationId();
        Coordinates coordinates = request.getCoordinates();

        log.info("gRPC getDepartures called: correlationId={}, lat={}, lon={}",
                correlationId, coordinates.getLatitude(), coordinates.getLongitude());

        try {
            // Delegiere an Controller (asynchron im Hintergrund verarbeiten)
            // Der Controller sendet das Ergebnis direkt an DisplayManager
            transportplanController.onGetDeparturesRequest(coordinates, correlationId);

            // Sofortiges ACK zurück an Locationhandler
            Ack ack = Ack.newBuilder()
                    .setAcceptedAt(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            responseObserver.onNext(ack);
            responseObserver.onCompleted();

            log.debug("ACK sent for correlationId={}", correlationId);

        } catch (Exception e) {
            log.error("Error processing getDepartures request, correlationId={}", correlationId, e);
            responseObserver.onError(e);
        }
    }
}
