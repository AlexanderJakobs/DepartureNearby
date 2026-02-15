package vsp;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.TransportplanClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransportplanClientTest {

    @Mock
    private TransportplanIngressGrpc.TransportplanIngressStub asyncStub;

    @InjectMocks
    private TransportplanClient transportplanClient;

    @Test
    void sendCoordinates_WithProvidedCorrelationId_CallsAsyncStubWithCorrectMeta() {
        Coordinates coords = Coordinates.newBuilder()
                .setLatitude(53.0)
                .setLongitude(10.0)
                .build();

        transportplanClient.sendCoordinates(coords, "cid-123");

        ArgumentCaptor<GetDeparturesRequest> requestCaptor = ArgumentCaptor.forClass(GetDeparturesRequest.class);
        ArgumentCaptor<StreamObserver<Ack>> observerCaptor = ArgumentCaptor.forClass(StreamObserver.class);

        verify(asyncStub, times(1)).getDepartures(requestCaptor.capture(), observerCaptor.capture());

        GetDeparturesRequest sent = requestCaptor.getValue();
        assertTrue(sent.hasMeta());
        assertEquals("cid-123", sent.getMeta().getCorrelationId());
        assertEquals("locationhandler", sent.getMeta().getCaller());
        assertEquals(coords, sent.getCoordinates());

        assertNotNull(observerCaptor.getValue());
    }

    @Test
    void sendCoordinates_WhenAsyncStubThrows_DoesNotThrow() {
        Coordinates coords = Coordinates.newBuilder()
                .setLatitude(53.0)
                .setLongitude(10.0)
                .build();

        doThrow(new RuntimeException("boom"))
                .when(asyncStub)
                .getDepartures(any(GetDeparturesRequest.class), any());

        assertDoesNotThrow(() -> transportplanClient.sendCoordinates(coords, "cid-err"));
    }
}
