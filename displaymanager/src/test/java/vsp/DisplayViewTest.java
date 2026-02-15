package vsp;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DisplayViewTest {

    private DisplayView displayView;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        displayView = new DisplayView();
        // Capture System.out und System.err
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void showDeparturesToMonitor_WithValidDepartures_DisplaysFormattedOutput() {
        List<DepartureStation> departureStations = createSampleDepartureStations();

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("DEPARTURE MONITOR"));
        assertTrue(output.contains("LINE"));
        assertTrue(output.contains("IN"));
        assertTrue(output.contains("TIME"));
        assertTrue(output.contains("STOP"));
        assertTrue(output.contains("U2 Niendorf Nord"));
        assertTrue(output.contains("Jungfernstieg"));
        assertTrue(output.contains("min"));

    }

    @Test
    void showDeparturesToMonitor_WithEmptyList_ShowsError() {
        List<DepartureStation> departureStations = new ArrayList<>();

        displayView.showDeparturesToMonitor(departureStations);
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("ERROR"));
        assertTrue(errorOutput.contains("No departures found"));
    }

    @Test
    void showDeparturesToMonitor_WithNull_ShowsError() {
        displayView.showDeparturesToMonitor(null);

        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("ERROR"));
        assertTrue(errorOutput.contains("No departures found"));
    }


    @Test
    void showDeparturesToMonitor_WithLongLineName_TruncatesCorrectly() {
        List<DepartureStation> departureStations = new ArrayList<>();
        String longLineName = "U4 This is a very long destination name that should be truncated";

        Departure dep = Departure.newBuilder()
                .setLineName(longLineName)
                .setDepartureTime(createTimestamp(Instant.now()))
                .build();
        DepartureStation departureStation = DepartureStation.newBuilder()
                .setStationName("Stop")
                .addDepartures(dep)
                .build();

        departureStations.add(departureStation);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("..."));
        assertFalse(output.contains(longLineName)); // Full text shouldn't appear
    }

    @Test
    void showDeparturesToMonitor_WithSpecialCharacters() {
        List<DepartureStation> departureStations = new ArrayList<>();
        String longLineName = "18 Gurlittstraße";

        Departure dep = Departure.newBuilder()
                .setLineName(longLineName)
                .setDepartureTime(createTimestamp(Instant.now()))
                .build();
        DepartureStation departureStation = DepartureStation.newBuilder()
                .setStationName("Böckmannstraße")
                .addDepartures(dep)
                .build();

        departureStations.add(departureStation);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("18 Gurlittstraße"));
        assertTrue(output.contains("Böckmannstraße"));
    }

    @Test
    void showDeparturesToMonitor_WithLongStopName_TruncatesCorrectly() {
        List<DepartureStation> departureStations = new ArrayList<>();
        String longStopName = "This is a very long stop name that exceeds the limit";

        Departure dep = Departure.newBuilder()
                .setLineName("109 Alsterdorf")
                .setDepartureTime(createTimestamp(Instant.now()))
                .build();
        DepartureStation departureStation = DepartureStation.newBuilder()
                .setStationName(longStopName)
                .addDepartures(dep)
                .build();

        departureStations.add(departureStation);
        departureStations.add(departureStation);
        displayView.showDeparturesToMonitor(departureStations);

        String output = outContent.toString();
        assertTrue(output.contains("..."));
    }

    @Test
    void showDeparturesToMonitor_WithMultipleDepartures_DisplaysAll() {
        List<DepartureStation> departureStations = createSampleDepartureStations();

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("U2"));
        assertTrue(output.contains("S1"));
        assertTrue(output.contains("U4"));
        assertTrue(output.contains("U3"));
    }

    @Test
    void showDeparturesToMonitor_DisplaysTotalCount() {
        List<DepartureStation> departureStations = createSampleDepartureStations();

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("Total: 4 departures")); // 4 Departures in Sample-Daten
    }

    @Test
    void showDeparturesToMonitor_WithDepartureInPast_ShowsDeparted() {
        List<DepartureStation> departureStations = new ArrayList<>();
        
        // Abfahrt in der Vergangenheit (5 Minuten her)
        Departure dep = Departure.newBuilder()
                .setLineName("U1")
                .setDepartureTime(createTimestamp(Instant.now().minusSeconds(300)))
                .build();
        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Test Station")
                .addDepartures(dep)
                .build();

        departureStations.add(station);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("departed"));
    }

    @Test
    void showDeparturesToMonitor_WithDepartureNow_ShowsNow() {
        List<DepartureStation> departureStations = new ArrayList<>();
        
        // Abfahrt jetzt (innerhalb der nächsten 30 Sekunden)
        Departure dep = Departure.newBuilder()
                .setLineName("U1")
                .setDepartureTime(createTimestamp(Instant.now().plusSeconds(15)))
                .build();
        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Test Station")
                .addDepartures(dep)
                .build();

        departureStations.add(station);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("now") || output.contains("0 min")); // Je nach genauem Timing
    }

    @Test
    void showError_DisplaysErrorMessage() {
        String errorMessage = "Test error message";

        displayView.showError(errorMessage);
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("ERROR"));
        assertTrue(errorOutput.contains(errorMessage));
        assertTrue(errorOutput.contains("!".repeat(80)));
    }

    @Test
    void showDeparturesToMonitor_WithDepartureInHour_ShowsHoursAndMinutes() {
        List<DepartureStation> departureStations = new ArrayList<>();
        
        // Abfahrt in 1 Stunde 30 Minuten (mit Buffer für Testausführung)
        Departure dep = Departure.newBuilder()
                .setLineName("U1")
                .setDepartureTime(createTimestamp(Instant.now().plusSeconds(5430))) // 90+ Minuten
                .build();
        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Test Station")
                .addDepartures(dep)
                .build();

        departureStations.add(station);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("1h 30m"));
    }

    @Test
    void showError_WithNullMessage_HandlesGracefully() {
        assertDoesNotThrow(() -> displayView.showError(null));
    }

    @Test
    void showError_WithEmptyMessage_DisplaysError() {
        displayView.showError("");

        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("ERROR"));
    }

    @Test
    void showLoading_DisplaysLoadingMessage() {
        String addressString = "Jungfernstieg 3";
        Address address = Address.newBuilder().setStreet("Jungfernstieg").setHouseNumber("3").build();

        displayView.showLoading(address);
        String output = outContent.toString();
        assertTrue(output.contains("[MONITOR] Searching departures near: " + addressString));
        assertTrue(output.contains("[MONITOR] Please wait..."));
    }

    @Test
    void showLoading_WithNullAddress_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> displayView.showLoading(null));
    }

    @Test
    void truncate_WithShortText_ReturnsOriginal() {
        // Note: truncate is private, but we can test it indirectly
        // through showDeparturesToMonitor

        List<DepartureStation> departureStations = new ArrayList<>();
        Departure dep = Departure.newBuilder()
                .setLineName("U2 Hamburg")
                .setDepartureTime(createTimestamp(Instant.now()))
                .build();
        DepartureStation departureStation = DepartureStation.newBuilder()
                .setStationName("Stop")
                .addDepartures(dep)
                .build();

        departureStations.add(departureStation);
        departureStations.add(departureStation);

        displayView.showDeparturesToMonitor(departureStations);
        String output = outContent.toString();
        assertTrue(output.contains("U2 Hamburg"));
        assertFalse(output.contains("..."));
    }

    // ========== Helper Methods ==========

    private List<vsp.DepartureStation> createSampleDepartureStations() {
        Instant now = Instant.now();

        // Station 1: Jungfernstieg
        vsp.Departure dep1 = vsp.Departure.newBuilder()
                .setLineName("U2 Niendorf Nord")
                .setDepartureTime(createTimestamp(now.plusSeconds(180)))
                .build();

        vsp.Departure dep2 = vsp.Departure.newBuilder()
                .setLineName("S1 Wedel")
                .setDepartureTime(createTimestamp(now.plusSeconds(300)))
                .build();

        vsp.DepartureStation station1 = vsp.DepartureStation.newBuilder()
                .setStationName("Jungfernstieg")
                .addDepartures(dep1)
                .addDepartures(dep2)
                .build();

        // Station 2: Hauptbahnhof
        vsp.Departure dep3 = vsp.Departure.newBuilder()
                .setLineName("U4 Billstedt")
                .setDepartureTime(createTimestamp(now.plusSeconds(600)))
                .build();

        vsp.DepartureStation station2 = vsp.DepartureStation.newBuilder()
                .setStationName("Hauptbahnhof")
                .addDepartures(dep3)
                .build();

        // Station 3: Rathausmarkt
        vsp.Departure dep4 = vsp.Departure.newBuilder()
                .setLineName("U3 Barmbek")
                .setDepartureTime(createTimestamp(now.plusSeconds(900)))
                .build();

        vsp.DepartureStation station3 = vsp.DepartureStation.newBuilder()
                .setStationName("Rathausmarkt")
                .addDepartures(dep4)
                .build();

        return Arrays.asList(station1, station2, station3);
    }
    /**
     * Helper: Erstellt Proto Timestamp aus Instant
     */
    private Timestamp createTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}