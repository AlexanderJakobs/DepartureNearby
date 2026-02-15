package vsp;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.LocationClient;
import vsp.controller.DisplayController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisplayIntegrationTestWithoutGRPC {

    private DisplayView displayView;
    private DisplayModel displayModel;

    @Mock
    private LocationClient locationClient;

    private DisplayController displayController;

    private List<DepartureStation> sampleDepartureStations;

    @BeforeEach
    void setUp() {
        displayView = spy(new DisplayView());
        // avoid a lot of console output during tests
        lenient().doNothing().when(displayView).showDeparturesToMonitor(anyList(), any());
        lenient().doNothing().when(displayView).showLoading(any());

        displayModel = spy(new DisplayModel(displayView));
        displayController = new DisplayController(displayView, displayModel, locationClient);

        sampleDepartureStations = createSampleDepartureStations();
    }

    @Test
    void fullFlow_userPassLocationThenDisplayDepartures() {
        String addressString = "Jungfernstieg 1";
        Address address = Address.newBuilder().setStreet("Jungfernstieg").setHouseNumber("1").build();

        displayController.userPassLocation(addressString);

        assertEquals(address, displayModel.getAddress());
        verify(displayView, times(1)).notifyAddressSaved();
        verify(displayView, times(1)).showLoading(address);
        verify(locationClient, times(1)).sendUserPassLocation(address);

        displayController.displayDepartures(sampleDepartureStations);

        assertEquals(sampleDepartureStations, displayModel.getDepartures());
        verify(displayView, times(1)).notifyDeparturesSaved();
        verify(displayView, times(1)).showDeparturesToMonitor(sampleDepartureStations, null);
    }

    @Test
    void userPassLocation_TrimsWhitespace() {
        String addressString = "     Jungfernstieg      1";
        Address expected = Address.newBuilder().setStreet("Jungfernstieg").setHouseNumber("1").build();

        displayController.userPassLocation(addressString);

        assertEquals(expected, displayModel.getAddress());
        verify(locationClient).sendUserPassLocation(expected);
    }

    @Test
    void userPassLocation_MultipleTimes_OverwritesPreviousAddress() {
        displayController.userPassLocation("Jungfernstieg 1");
        displayController.userPassLocation("Jungfernstieg 2");
        displayController.userPassLocation("Jungfernstieg 3");

        Address expectedLast = Address.newBuilder().setStreet("Jungfernstieg").setHouseNumber("3").build();
        assertEquals(expectedLast, displayModel.getAddress());

        verify(displayView, times(3)).notifyAddressSaved();
        verify(displayView, times(3)).showLoading(any(Address.class));

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(locationClient, times(3)).sendUserPassLocation(addressCaptor.capture());
        assertEquals(expectedLast, addressCaptor.getValue());
    }

    @Test
    void displayDepartures_WithTimestamp_DisplaysWithTimestamp() {
        Timestamp fetchedAt = createTimestamp(Instant.now().minusSeconds(30));

        displayController.displayDepartures(sampleDepartureStations, fetchedAt);

        verify(displayView).showDeparturesToMonitor(sampleDepartureStations, fetchedAt);
    }

    @Test
    void displayDepartures_WithNullInput_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.displayDepartures(null));
    }

    // ========== Helper Methods ==========

    private List<DepartureStation> createSampleDepartureStations() {
        Instant now = Instant.now();

        Departure dep1 = Departure.newBuilder()
                .setLineName("U2 Niendorf Nord")
                .setDepartureTime(createTimestamp(now.plusSeconds(180)))
                .build();

        Departure dep2 = Departure.newBuilder()
                .setLineName("S1 Wedel")
                .setDepartureTime(createTimestamp(now.plusSeconds(300)))
                .build();

        DepartureStation station1 = DepartureStation.newBuilder()
                .setStationName("Jungfernstieg")
                .addDepartures(dep1)
                .addDepartures(dep2)
                .build();

        Departure dep3 = Departure.newBuilder()
                .setLineName("U4 Billstedt")
                .setDepartureTime(createTimestamp(now.plusSeconds(600)))
                .build();

        DepartureStation station2 = DepartureStation.newBuilder()
                .setStationName("Hauptbahnhof")
                .addDepartures(dep3)
                .build();

        Departure dep4 = Departure.newBuilder()
                .setLineName("U3 Barmbek")
                .setDepartureTime(createTimestamp(now.plusSeconds(900)))
                .build();

        DepartureStation station3 = DepartureStation.newBuilder()
                .setStationName("Rathausmarkt")
                .addDepartures(dep4)
                .build();

        return Arrays.asList(station1, station2, station3);
    }

    private Timestamp createTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
