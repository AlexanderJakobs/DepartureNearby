package vsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Model für den Locationhandler (MC-Pattern).
 * Speichert Address und Coordinates Daten.
 */
@Component
public class LocationModel {

    private static final Logger log = LoggerFactory.getLogger(LocationModel.class);

    private Address address;
    private Coordinates coordinates;

    /**
     * Speichert die Adresse.
     */
    public void saveAddress(Address address) {
        this.address = address;
        log.debug("Address saved: {} {}", address.getStreet(), address.getHouseNumber());
    }

    /**
     * Speichert die aufgelösten Koordinaten.
     */
    public void saveCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
        log.debug("Coordinates saved: lat={}, lon={}",
                coordinates.getLatitude(), coordinates.getLongitude());
    }

    /**
     * Gibt die gespeicherte Adresse zurück.
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Gibt die gespeicherten Koordinaten zurück.
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Prüft, ob Koordinaten vorhanden sind.
     */
    public boolean hasCoordinates() {
        return coordinates != null;
    }

    /**
     * Setzt den Model-Zustand zurück.
     */
    public void reset() {
        this.address = null;
        this.coordinates = null;
        log.debug("LocationModel reset");
    }
}
