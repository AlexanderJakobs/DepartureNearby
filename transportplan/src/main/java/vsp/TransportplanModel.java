package vsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Model für den Transportplan (MC-Pattern).
 * Speichert die aktuellen Coordinates und DepartureStations.
 */
@Component
public class TransportplanModel {

    private static final Logger log = LoggerFactory.getLogger(TransportplanModel.class);

    private Coordinates coordinates;
    private List<DepartureStation> departureStations;

    /**
     * Speichert die Coordinates vom Locationhandler.
     */
    public void saveCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
        log.debug("Coordinates saved: lat={}, lon={}",
                coordinates.getLatitude(), coordinates.getLongitude());
    }

    /**
     * Gibt die gespeicherten Coordinates zurück.
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Speichert die DepartureStations von der Geofox API.
     */
    public void saveDeparturesMap(List<DepartureStation> departureStations) {
        this.departureStations = departureStations;
        log.debug("DepartureStations saved: {} stations", departureStations.size());
    }

    /**
     * Gibt die gespeicherten DepartureStations zurück.
     */
    public List<DepartureStation> getDepartureStations() {
        return departureStations;
    }

    /**
     * Löscht alle gespeicherten Daten.
     */
    public void clear() {
        this.coordinates = null;
        this.departureStations = new ArrayList<>();
        log.debug("TransportModel cleared");
    }
}
