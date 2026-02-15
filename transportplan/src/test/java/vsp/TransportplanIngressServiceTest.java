package vsp;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vsp.service.TransportplanIngressService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransportplanIngressServiceTest {

    @Test
    @DisplayName("getDepartures: delegiert an Controller und antwortet sofort mit ACK")
    void getDepartures_DelegatesAndAcks() {
        // Arrange
        TransportplanController controller = mock(TransportplanController.class);
        TransportplanIngressService service = new TransportplanIngressService(controller);

        Coordinates coordinates = Coordinates.newBuilder().setLatitude(53.55).setLongitude(9.99).build();
        String correlationId = "corr-1";

        GetDeparturesRequest request = GetDeparturesRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder().setCorrelationId(correlationId).setCaller("locationhandler").build())
                .setCoordinates(coordinates)
                .build();

        @SuppressWarnings("unchecked")
        StreamObserver<Ack> responseObserver = mock(StreamObserver.class);

        // Act
        service.getDepartures(request, responseObserver);

        // Assert: Controller aufgerufen
        verify(controller).onGetDeparturesRequest(eq(coordinates), eq(correlationId));

        // Assert: ACK sofort zurueck
        ArgumentCaptor<Ack> ackCaptor = ArgumentCaptor.forClass(Ack.class);
        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        Ack ack = ackCaptor.getValue();
        assertTrue(ack.hasAcceptedAt(), "ACK sollte acceptedAt enthalten");

        long now = Instant.now().getEpochSecond();
        long accepted = ack.getAcceptedAt().getSeconds();
        assertTrue(accepted > 0, "acceptedAt.seconds sollte gesetzt sein");
        assertTrue(Math.abs(now - accepted) < 10, "acceptedAt sollte in der Naehe von 'now' liegen");
    }

    @Test
    @DisplayName("getDepartures: wenn Controller wirft, wird onError aufgerufen")
    void getDepartures_WhenControllerThrows_CallsOnError() {
        // Arrange
        TransportplanController controller = mock(TransportplanController.class);
        TransportplanIngressService service = new TransportplanIngressService(controller);

        Coordinates coordinates = Coordinates.newBuilder().setLatitude(53.55).setLongitude(9.99).build();
        String correlationId = "corr-err";

        GetDeparturesRequest request = GetDeparturesRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder().setCorrelationId(correlationId).setCaller("locationhandler").build())
                .setCoordinates(coordinates)
                .build();

        @SuppressWarnings("unchecked")
        StreamObserver<Ack> responseObserver = mock(StreamObserver.class);

        doThrow(new RuntimeException("boom"))
                .when(controller).onGetDeparturesRequest(eq(coordinates), eq(correlationId));

        // Act
        service.getDepartures(request, responseObserver);

        // Assert
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(any(RuntimeException.class));
    }
}
