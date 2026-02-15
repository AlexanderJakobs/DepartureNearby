package vsp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransportplanModelTest {

    @Test
    @DisplayName("saveCoordinates/getCoordinates: speichert und liefert Koordinaten")
    void saveAndGetCoordinates() {
        TransportplanModel model = new TransportplanModel();
        Coordinates coords = Coordinates.newBuilder()
                .setLatitude(53.5531)
                .setLongitude(9.9927)
                .build();

        model.saveCoordinates(coords);

        assertNotNull(model.getCoordinates());
        assertEquals(53.5531, model.getCoordinates().getLatitude(), 1e-6);
        assertEquals(9.9927, model.getCoordinates().getLongitude(), 1e-6);
    }

    @Test
    @DisplayName("saveDeparturesMap/getDepartureStations: speichert und liefert DepartureStations")
    void saveAndGetDepartureStations() {
        TransportplanModel model = new TransportplanModel();

        DepartureStation station = DepartureStation.newBuilder()
                .setStationName("Jungfernstieg")
                .setDistance(10)
                .addDepartures(Departure.newBuilder().setLineName("U1").build())
                .build();

        model.saveDeparturesMap(List.of(station));

        assertNotNull(model.getDepartureStations());
        assertEquals(1, model.getDepartureStations().size());
        assertEquals("Jungfernstieg", model.getDepartureStations().get(0).getStationName());
        assertEquals(1, model.getDepartureStations().get(0).getDeparturesCount());
    }

    @Test
    @DisplayName("clear: setzt Coordinates auf null und Departures auf leere Liste")
    void clearResetsState() {
        TransportplanModel model = new TransportplanModel();
        model.saveCoordinates(Coordinates.newBuilder().setLatitude(1).setLongitude(2).build());
        model.saveDeparturesMap(List.of(DepartureStation.newBuilder().setStationName("X").build()));

        model.clear();

        assertNull(model.getCoordinates());
        assertNotNull(model.getDepartureStations());
        assertTrue(model.getDepartureStations().isEmpty());
    }
}
