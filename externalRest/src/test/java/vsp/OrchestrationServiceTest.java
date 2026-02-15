package vsp;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.DisplaymanagerClient;
import vsp.client.GeofoxClient;
import vsp.service.OrchestrationService;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

    @Mock
    private DisplaymanagerClient displaymanagerClient;

    @Mock
    private GeofoxClient geofoxClient;

    private OrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new OrchestrationService(displaymanagerClient, geofoxClient);
    }

    @Test
    void addRequest_ForwardsToDisplaymanagerClient() {
        String address = "Hauptstraße 12";

        boolean accepted = orchestrationService.addRequest(address);

        assertTrue(accepted);
        verify(displaymanagerClient, times(1)).sendUserPassLocation(address);
    }

    @Test
    void getLastInteraction_Success() {
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(12).setNanos(0).build();
        when(geofoxClient.getLastInteraction()).thenReturn(timestamp);

        Timestamp result = orchestrationService.getLastInteraction();
        assertNotNull(result);
        assertEquals(timestamp, result);
    }

    @Test
    void getLastInteraction_WhenNoLastInteraction_Throws() {
        when(geofoxClient.getLastInteraction()).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> orchestrationService.getLastInteraction());
    }
}
