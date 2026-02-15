package vsp;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.service.LocationhandlerIngressService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationhandlerIngressServiceTest {

    @Mock
    private LocationController locationController;

    @Mock
    private StreamObserver<Ack> responseObserver;

    private LocationhandlerIngressService ingressService;

    @BeforeEach
    void setUp() {
        ingressService = new LocationhandlerIngressService(locationController);
    }

    @Test
    void userPassLocation_WithValidRequest_SendsAckAndForwardsAsync() {
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .setCity("Hamburg")
                .build();

        UserPassLocationRequest request = UserPassLocationRequest.newBuilder()
                .setMeta(RequestMeta.newBuilder()
                        .setCorrelationId("cid-1")
                        .setCaller("displaymanager")
                        .build())
                .setAddress(address)
                .build();

        ArgumentCaptor<Ack> ackCaptor = ArgumentCaptor.forClass(Ack.class);

        ingressService.userPassLocation(request, responseObserver);

        verify(responseObserver).onNext(ackCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        Ack ack = ackCaptor.getValue();
        assertTrue(ack.hasAcceptedAt());
        assertTrue(ack.getAcceptedAt().getSeconds() > 0);

        verify(locationController, timeout(1000)).onResolveLocationRequest(address);
    }

    @Test
    void userPassLocation_WhenControllerThrows_AckStillSent() {
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .build();

        UserPassLocationRequest request = UserPassLocationRequest.newBuilder()
                .setAddress(address)
                .build();

        doThrow(new RuntimeException("boom"))
                .when(locationController)
                .onResolveLocationRequest(any(Address.class));

        ingressService.userPassLocation(request, responseObserver);

        verify(responseObserver).onNext(any(Ack.class));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        verify(locationController, timeout(1000)).onResolveLocationRequest(address);
    }

    @Test
    void userPassLocation_WithNullRequest_SendsInternalError() {
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);

        ingressService.userPassLocation(null, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(responseObserver).onError(errorCaptor.capture());

        Throwable t = errorCaptor.getValue();
        assertInstanceOf(StatusRuntimeException.class, t);
        StatusRuntimeException sre = (StatusRuntimeException) t;
        assertEquals(Status.INTERNAL.getCode(), sre.getStatus().getCode());
    }
}
