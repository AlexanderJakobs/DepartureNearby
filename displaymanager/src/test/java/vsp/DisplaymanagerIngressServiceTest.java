package vsp;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.controller.DisplayController;
import vsp.service.DisplaymanagerIngressService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisplaymanagerIngressServiceTest {

    @Mock
    private DisplayController displayController;

    @Mock
    private StreamObserver<Ack> responseObserver;

    private DisplaymanagerIngressService displaymanagerIngressService;

    @BeforeEach
    void setUp() {
        displaymanagerIngressService = new DisplaymanagerIngressService(displayController);
    }

    @Test
    void userPassLocation_WithValidRequest_SendsAckAndForwardsAsync() {
        ExternalInput request = ExternalInput.newBuilder()
                .setAddress("Jungfernstieg 1")
                .build();

        ArgumentCaptor<Ack> ackCaptor = ArgumentCaptor.forClass(Ack.class);

        displaymanagerIngressService.userPassLocation(request, responseObserver);

        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        Ack ack = ackCaptor.getValue();
        assertTrue(ack.hasAcceptedAt());
        assertTrue(ack.getAcceptedAt().getSeconds() > 0);

        // Forwarding happens in a separate thread
        verify(displayController, timeout(1000)).userPassLocation("Jungfernstieg 1");
    }

    @Test
    void showDepartures_WithValidRequest_SendsAckAndForwardsAsync_WithTimestamp() {
        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Jungfernstieg")
                .build();

        Timestamp dataFetchedAt = createTimestamp(Instant.now().minusSeconds(15));

        ShowDeparturesRequest request = ShowDeparturesRequest.newBuilder()
                .addStations(station)
                .setDataFetchedAt(dataFetchedAt)
                .build();

        ArgumentCaptor<Ack> ackCaptor = ArgumentCaptor.forClass(Ack.class);
        ArgumentCaptor<List<DepartureStation>> stationsCaptor = ArgumentCaptor.forClass(List.class);

        displaymanagerIngressService.showDepartures(request, responseObserver);

        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        Ack ack = ackCaptor.getValue();
        assertTrue(ack.hasAcceptedAt());

        // Forwarding happens in a separate thread
        verify(displayController, timeout(1000)).displayDepartures(stationsCaptor.capture(), eq(dataFetchedAt));
        assertEquals(1, stationsCaptor.getValue().size());
        assertEquals("Jungfernstieg", stationsCaptor.getValue().get(0).getStationName());
    }

    @Test
    void showDepartures_WithMultipleStations_ForwardsAllToController() {
        DepartureStation station1 = DepartureStation.newBuilder().setStationName("Jungfernstieg").build();
        DepartureStation station2 = DepartureStation.newBuilder().setStationName("Hauptbahnhof").build();

        ShowDeparturesRequest request = ShowDeparturesRequest.newBuilder()
                .addStations(station1)
                .addStations(station2)
                .build();

        ArgumentCaptor<List<DepartureStation>> stationsCaptor = ArgumentCaptor.forClass(List.class);

        displaymanagerIngressService.showDepartures(request, responseObserver);

        verify(displayController, timeout(1000)).displayDepartures(stationsCaptor.capture(), isNull());
        List<DepartureStation> captured = stationsCaptor.getValue();
        assertEquals(2, captured.size());
        assertEquals("Jungfernstieg", captured.get(0).getStationName());
        assertEquals("Hauptbahnhof", captured.get(1).getStationName());
    }

    @Test
    void showDepartures_WithEmptyStationsList_SendsErrorResponse() {
        ShowDeparturesRequest request = ShowDeparturesRequest.newBuilder().build();

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);

        displaymanagerIngressService.showDepartures(request, responseObserver);

        verify(displayController, never()).displayDepartures(anyList(), any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(errorCaptor.capture());

        Throwable t = errorCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, t);
        StatusRuntimeException sre = (StatusRuntimeException) t;
        assertEquals(Status.INVALID_ARGUMENT.getCode(), sre.getStatus().getCode());
    }

    @Test
    void showDepartures_WhenControllerThrows_AckStillSent() {
        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Test")
                .build();

        ShowDeparturesRequest request = ShowDeparturesRequest.newBuilder()
                .addStations(station)
                .build();

        doThrow(new RuntimeException("boom"))
                .when(displayController)
                .displayDepartures(anyList(), any());

        displaymanagerIngressService.showDepartures(request, responseObserver);

        verify(responseObserver).onNext(any(Ack.class));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        verify(displayController, timeout(1000)).displayDepartures(anyList(), isNull());
    }

    private Timestamp createTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
