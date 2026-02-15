package vsp;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.LocationClient;
import vsp.controller.DisplayController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisplayControllerTest {

    @Mock
    private DisplayView view;

    @Mock
    private DisplayModel model;

    @Mock
    private LocationClient locationClient;

    @InjectMocks
    private DisplayController displayController;

    private List<DepartureStation> sampleDepartureStations;

    @BeforeEach
    void setUp() {
        sampleDepartureStations = createSampleDepartureStations();
    }

    // ========== userPassLocation Tests ==========

    @Test
    void userPassLocation_ValidStreetAndHouseNumber_CallsAllComponents() {
        Address address = Address.newBuilder().setStreet("Jungfernstieg").setHouseNumber("1").build();

        displayController.userPassLocation("Jungfernstieg 1");

        verify(model, times(1)).saveAddress(address);
        verify(locationClient, times(1)).sendUserPassLocation(address);
        verify(view, times(1)).showLoading(address);
    }

    @Test
    void userPassLocation_AllowsStreetWithSpaces() {
        String input = "Am Alten Wall 1";
        Address expected = Address.newBuilder().setStreet("Am Alten Wall").setHouseNumber("1").build();

        displayController.userPassLocation(input);

        verify(model).saveAddress(expected);
        verify(locationClient).sendUserPassLocation(expected);
        verify(view).showLoading(expected);
    }

    @Test
    void userPassLocation_ConcatenatesAddressCorrectly() {
        String input = "Mönckebergstraße 5";
        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

        displayController.userPassLocation(input);

        verify(model).saveAddress(addressCaptor.capture());
        Address captured = addressCaptor.getValue();
        assertEquals("Mönckebergstraße 5", captured.getStreet() + " " + captured.getHouseNumber());
    }

    @Test
    void userPassLocation_CallsModelBeforeCallerStub() {
        String input = "Hauptstraße 10";
        Address address = Address.newBuilder().setStreet("Hauptstraße").setHouseNumber("10").build();

        displayController.userPassLocation(input);

        var inOrder = inOrder(model, locationClient, view);
        inOrder.verify(model).saveAddress(address);
        inOrder.verify(locationClient).sendUserPassLocation(address);
        inOrder.verify(view).showLoading(address);
    }

    @Test
    void userPassLocation_WithEmptyStreet_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.userPassLocation("   5"));
    }

    @Test
    void userPassLocation_WithEmptyHouseNumber_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.userPassLocation("Teststraße   "));
    }

    @Test
    void userPassLocation_WithBothEmpty_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.userPassLocation(""));
    }

    @Test
    void userPassLocation_WithNull_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.userPassLocation(null));
    }

    @Test
    void userPassLocation_WithSpecialCharacters_HandlesCorrectly() {
        String input = "Mönckebergstraße 123a";
        Address expected = Address.newBuilder().setStreet("Mönckebergstraße").setHouseNumber("123a").build();

        displayController.userPassLocation(input);

        verify(model).saveAddress(expected);
        verify(locationClient).sendUserPassLocation(expected);
        verify(view).showLoading(expected);
    }

    @Test
    void userPassLocation_WithLongStreetName_HandlesCorrectly() {
        String input = "Thisisaverylongaddressnamewithmultiplewords 13";
        Address expected = Address.newBuilder()
                .setStreet("Thisisaverylongaddressnamewithmultiplewords")
                .setHouseNumber("13")
                .build();

        displayController.userPassLocation(input);

        verify(model).saveAddress(expected);
        verify(locationClient).sendUserPassLocation(expected);
        verify(view).showLoading(expected);
    }

    @Test
    void userPassLocation_PassesParametersToCallerStubUnchanged() {
        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);

        displayController.userPassLocation("Jungfernstieg 1");

        verify(locationClient).sendUserPassLocation(addressCaptor.capture());
        assertEquals("Jungfernstieg", addressCaptor.getValue().getStreet());
        assertEquals("1", addressCaptor.getValue().getHouseNumber());
    }

    // ========== displayDepartures Tests ==========

    @Test
    void displayDepartures_ValidList_CallsModelMethodsWithNullTimestamp() {
        displayController.displayDepartures(sampleDepartureStations);

        verify(model, times(1)).saveDepartures(sampleDepartureStations);
        verify(model, times(1)).displayDepartures(sampleDepartureStations, null);
    }

    @Test
    void displayDepartures_WithTimestamp_ForwardsTimestampToModel() {
        Timestamp fetchedAt = createTimestamp(Instant.now());

        displayController.displayDepartures(sampleDepartureStations, fetchedAt);

        verify(model).saveDepartures(sampleDepartureStations);
        verify(model).displayDepartures(sampleDepartureStations, fetchedAt);
    }

    @Test
    void displayDepartures_CallsModelInCorrectOrder() {
        displayController.displayDepartures(sampleDepartureStations);

        var inOrder = inOrder(model);
        inOrder.verify(model).saveDepartures(sampleDepartureStations);
        inOrder.verify(model).displayDepartures(sampleDepartureStations, null);
    }

    @Test
    void displayDepartures_WithEmptyList_StillCallsModel() {
        List<DepartureStation> emptyList = new ArrayList<>();

        displayController.displayDepartures(emptyList);

        verify(model).saveDepartures(emptyList);
        verify(model).displayDepartures(emptyList, null);
    }

    @Test
    void displayDepartures_WithNullInput_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> displayController.displayDepartures(null));
        verifyNoInteractions(model, view, locationClient);
    }

    @Test
    void displayDepartures_DoesNotCallViewDirectly() {
        displayController.displayDepartures(sampleDepartureStations);

        verifyNoInteractions(view);
    }

    @Test
    void displayDepartures_DoesNotCallCallerStub() {
        displayController.displayDepartures(sampleDepartureStations);

        verifyNoInteractions(locationClient);
    }

    // ========== Helper Methods ==========

    private List<DepartureStation> createSampleDepartureStations() {
        Instant now = Instant.now();

        Departure dep1 = Departure.newBuilder()
                .setLineName("U2 Niendorf Nord")
                .setDepartureTime(createTimestamp(now))
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
