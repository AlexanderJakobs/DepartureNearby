package vsp;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.LocationClient;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocationClientTest {

    @Mock
    private LocationhandlerIngressGrpc.LocationhandlerIngressStub asyncStub;

    private LocationClient locationClient;

    @BeforeEach
    void setUp() throws Exception {
        locationClient = new LocationClient();

        // Inject mock asyncStub via reflection
        Field stubField = LocationClient.class.getDeclaredField("asyncStub");
        stubField.setAccessible(true);
        stubField.set(locationClient, asyncStub);
    }

    @Test
    void sendUserPassLocation_WithValidAddress_CallsAsyncStub() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        UserPassLocationRequest request = requestCaptor.getValue();
        assertEquals(address, request.getAddress());
        assertTrue(request.hasMeta());

        RequestMeta meta = request.getMeta();
        assertFalse(meta.getCorrelationId().isEmpty());
        assertEquals("Displaymanager", meta.getCaller());
    }

    @Test
    void sendUserPassLocation_WithoutCorrelationId_GeneratesOne() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, null, null);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        String correlationId = meta.getCorrelationId();

        assertNotNull(correlationId);
        assertFalse(correlationId.isEmpty());
        // UUID Format: 8-4-4-4-12 characters
        assertTrue(correlationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void sendUserPassLocation_WithCorrelationId_UsesProvidedId() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();
        String providedCorrelationId = "test-correlation-123";

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, providedCorrelationId, null);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        assertEquals(providedCorrelationId, meta.getCorrelationId());
    }

    @Test
    void sendUserPassLocation_WithSessionId_IncludesSessionIdInMeta() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();
        String sessionId = "session-456";

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, "correlation-123", sessionId);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        assertEquals(sessionId, meta.getSessionId());
        assertFalse(meta.getSessionId().isEmpty());
    }

    @Test
    void sendUserPassLocation_WithoutSessionId_SessionIdIsEmpty() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, "correlation-123", null);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        // Proto String fields sind niemals null, sondern empty string
        assertTrue(meta.getSessionId().isEmpty());
    }

    @Test
    void sendUserPassLocation_MetaContainsAllRequiredFields() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();
        String correlationId = "test-correlation";
        String sessionId = "test-session";

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, correlationId, sessionId);

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        assertEquals(correlationId, meta.getCorrelationId());
        assertEquals("Displaymanager", meta.getCaller());
        assertEquals(sessionId, meta.getSessionId());
    }

    @Test
    void sendUserPassLocation_OnSuccessfulAck_LogsTimestamp() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<StreamObserver<Ack>> observerCaptor =
                ArgumentCaptor.forClass(StreamObserver.class);

        // When
        locationClient.sendUserPassLocation(address);

        verify(asyncStub).userPassLocation(any(), observerCaptor.capture());

        StreamObserver<Ack> observer = observerCaptor.getValue();

        // Simulate successful Ack
        Ack ack = Ack.newBuilder()
                .setAcceptedAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(System.currentTimeMillis() / 1000)
                        .setNanos(0)
                        .build())
                .build();

        // Then - should not throw exception
        assertDoesNotThrow(() -> observer.onNext(ack));
        assertDoesNotThrow(() -> observer.onCompleted());
    }

    @Test
    void sendUserPassLocation_OnError_LogsError() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<StreamObserver<Ack>> observerCaptor =
                ArgumentCaptor.forClass(StreamObserver.class);

        // When
        locationClient.sendUserPassLocation(address);

        verify(asyncStub).userPassLocation(any(), observerCaptor.capture());

        StreamObserver<Ack> observer = observerCaptor.getValue();

        // Simulate error
        StatusRuntimeException error = Status.UNAVAILABLE
                .withDescription("Service unavailable")
                .asRuntimeException();

        // Then - should not throw exception (fire-and-forget)
        assertDoesNotThrow(() -> observer.onError(error));
    }

    @Test
    void sendUserPassLocation_OnErrorWithMetadata_ExtractsErrorStatus() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<StreamObserver<Ack>> observerCaptor =
                ArgumentCaptor.forClass(StreamObserver.class);

        // When
        locationClient.sendUserPassLocation(address);

        verify(asyncStub).userPassLocation(any(), observerCaptor.capture());

        StreamObserver<Ack> observer = observerCaptor.getValue();

        // Create ErrorStatus in metadata
        ErrorStatus errorStatus = ErrorStatus.newBuilder()
                .setCode(ErrorStatus.Code.INVALID_ARGUMENT)
                .setMessage("Invalid address")
                .setDetails("Street is required")
                .build();

        Metadata metadata = new Metadata();
        Metadata.Key<byte[]> errorKey = Metadata.Key.of(
                "error-details-bin",
                Metadata.BINARY_BYTE_MARSHALLER);
        metadata.put(errorKey, errorStatus.toByteArray());

        StatusRuntimeException error = Status.INVALID_ARGUMENT
                .withDescription("Invalid request")
                .asRuntimeException(metadata);

        // Then - should not throw exception
        assertDoesNotThrow(() -> observer.onError(error));
    }

    @Test
    void sendUserPassLocation_ReturnsImmediately_FireAndForget() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        // When
        long startTime = System.nanoTime();
        locationClient.sendUserPassLocation(address);
        long endTime = System.nanoTime();

        // Then - should return very quickly (< 500ms)
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 500,
                String.format("Method should return immediately (fire-and-forget), but took %dms", durationMs));


        verify(asyncStub).userPassLocation(any(), any());
    }

    @Test
    void sendUserPassLocation_WithEmptySessionId_DoesNotSetSessionId() {
        // Given
        Address address = Address.newBuilder()
                .setStreet("Test")
                .setHouseNumber("1")
                .build();

        ArgumentCaptor<UserPassLocationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserPassLocationRequest.class);

        // When
        locationClient.sendUserPassLocation(address, "correlation", "");

        // Then
        verify(asyncStub).userPassLocation(requestCaptor.capture(), any());

        RequestMeta meta = requestCaptor.getValue().getMeta();
        assertTrue(meta.getSessionId().isEmpty());
    }
}
