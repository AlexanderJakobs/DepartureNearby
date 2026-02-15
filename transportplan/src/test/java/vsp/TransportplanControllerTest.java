package vsp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vsp.client.DeparturesClient;
import vsp.client.DisplayClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests fuer {@link TransportplanController}.
 *
 * Fokus:
 * - Controller speichert Coordinates
 * - Controller limitiert Stationen auf 3 und sortiert nach Distanz
 * - Controller delegiert Abfahrts-Ermittlung an DeparturesClient
 * - Controller sendet Ergebnis (asynchron) an DisplayClient
 * - Fehlerpfad: sendet leere Liste an DisplayClient
 */
class TransportplanControllerTest {

    @Test
    @DisplayName("onGetDeparturesRequest: sortiert nach Distanz, limitiert auf 3, holt Departures und sendet an Display")
    void onGetDeparturesRequest_SortsAndLimitsAndSends() {
        // Arrange
        TransportplanModel model = mock(TransportplanModel.class);
        DeparturesClient departuresClient = mock(DeparturesClient.class);
        DisplayClient displayClient = mock(DisplayClient.class);

        TransportplanController controller = new TransportplanController(model, departuresClient, displayClient);

        Coordinates coordinates = Coordinates.newBuilder()
                .setLatitude(53.5531)
                .setLongitude(9.9927)
                .build();
        String correlationId = "corr-123";

        DepartureStation s1 = DepartureStation.newBuilder().setStationName("A").setDistance(300).build();
        DepartureStation s2 = DepartureStation.newBuilder().setStationName("B").setDistance(10).build();
        DepartureStation s3 = DepartureStation.newBuilder().setStationName("C").setDistance(200).build();
        DepartureStation s4 = DepartureStation.newBuilder().setStationName("D").setDistance(50).build();
        DepartureStation s5 = DepartureStation.newBuilder().setStationName("E").setDistance(150).build();

        List<DepartureStation> nearbyStations = List.of(s1, s2, s3, s4, s5);

        when(departuresClient.getNearbyStations(eq(coordinates), eq(50)))
                .thenReturn(nearbyStations);

        // Wir geben "Departures" zurueck (kann auch identisch zur Input-Liste sein)
        when(departuresClient.getDepartures(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        controller.onGetDeparturesRequest(coordinates, correlationId);

        // Assert (Model)
        verify(model).saveCoordinates(eq(coordinates));

        // Assert (DeparturesClient: getDepartures bekommt exakt 3 Stationen, sortiert nach Distanz)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DepartureStation>> stationsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(departuresClient).getDepartures(stationsCaptor.capture());

        List<DepartureStation> passedStations = stationsCaptor.getValue();
        assertEquals(3, passedStations.size(), "Controller sollte auf 3 Stationen limitieren");

        // Erwartete Reihenfolge nach Distanz: B(10), D(50), E(150)
        assertEquals(List.of("B", "D", "E"),
                passedStations.stream().map(DepartureStation::getStationName).toList(),
                "Stationen muessen nach Distanz sortiert und die 3 naechsten sein");

        // Assert (Model: gespeicherte Departures)
        verify(model).saveDeparturesMap(eq(passedStations));

        // Assert (async Send): wir warten via Mockito timeout
        verify(displayClient, timeout(1000))
                .sendDeparturesToDisplayManager(eq(passedStations), eq(correlationId));
    }

    @Test
    @DisplayName("onGetDeparturesRequest: wenn getNearbyStations scheitert, sendet Controller leere Liste an Display")
    void onGetDeparturesRequest_WhenNearbyStationsThrows_SendsEmptyList() {
        // Arrange
        TransportplanModel model = mock(TransportplanModel.class);
        DeparturesClient departuresClient = mock(DeparturesClient.class);
        DisplayClient displayClient = mock(DisplayClient.class);

        TransportplanController controller = new TransportplanController(model, departuresClient, displayClient);

        Coordinates coordinates = Coordinates.newBuilder()
                .setLatitude(53.5531)
                .setLongitude(9.9927)
                .build();
        String correlationId = "corr-err";

        when(departuresClient.getNearbyStations(eq(coordinates), eq(50)))
                .thenThrow(new RuntimeException("boom"));

        // Act
        controller.onGetDeparturesRequest(coordinates, correlationId);

        // Assert
        verify(model).saveCoordinates(eq(coordinates));
        verify(departuresClient, never()).getDepartures(anyList());
        verify(model, never()).saveDeparturesMap(anyList());

        verify(displayClient).sendDeparturesToDisplayManager(eq(List.of()), eq(correlationId));
    }

    @Test
    @DisplayName("onGetDeparturesRequest: wenn getDepartures scheitert, sendet Controller leere Liste an Display")
    void onGetDeparturesRequest_WhenGetDeparturesThrows_SendsEmptyList() {
        // Arrange
        TransportplanModel model = mock(TransportplanModel.class);
        DeparturesClient departuresClient = mock(DeparturesClient.class);
        DisplayClient displayClient = mock(DisplayClient.class);

        TransportplanController controller = new TransportplanController(model, departuresClient, displayClient);

        Coordinates coordinates = Coordinates.newBuilder()
                .setLatitude(53.5531)
                .setLongitude(9.9927)
                .build();
        String correlationId = "corr-dep";

        List<DepartureStation> nearbyStations = List.of(
                DepartureStation.newBuilder().setStationName("A").setDistance(10).build(),
                DepartureStation.newBuilder().setStationName("B").setDistance(20).build()
        );

        when(departuresClient.getNearbyStations(eq(coordinates), eq(50)))
                .thenReturn(nearbyStations);
        when(departuresClient.getDepartures(anyList()))
                .thenThrow(new RuntimeException("boom"));

        // Act
        controller.onGetDeparturesRequest(coordinates, correlationId);

        // Assert
        verify(model).saveCoordinates(eq(coordinates));
        verify(model, never()).saveDeparturesMap(anyList());
        verify(displayClient).sendDeparturesToDisplayManager(eq(List.of()), eq(correlationId));
    }
}
