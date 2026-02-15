package vsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.client.GeocodingClient;
import vsp.client.TransportplanClient;

/**
 * Controller für den Locationhandler (MC-Pattern).
 * Empfängt Address, ruft GeocodingClient (via externalRest) für Geocoding auf,
 * und leitet die Coordinates an den Transportplan weiter.
 */
@Component
public class LocationController {

    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    private final LocationModel model;
    private final GeocodingClient geocodingClient;
    private final TransportplanClient transportplanClient;

    public LocationController(
            LocationModel model,
            GeocodingClient geocodingClient,
            TransportplanClient transportplanClient) {
        this.model = model;
        this.geocodingClient = geocodingClient;
        this.transportplanClient = transportplanClient;
    }

    /**
     * ENTRY POINT: Empfängt Address vom DisplayManager (via gRPC).
     * Wird vom LocationhandlerIngress Service aufgerufen.
     *
     * @param address Die Address mit Straße, Hausnummer und Stadt
     */
    public void onResolveLocationRequest(Address address) {

        log.info("Locationhandler received address: {} {}, {}",
                address.getStreet(), address.getHouseNumber(), address.getCity());

        // 1. Address im Model speichern
        model.saveAddress(address);

        // 2. Geocoding via GeocodingClient (calls externalRest -> Nominatim)
        try {
            Coordinates coordinates = geocodingClient.getCoordinatesForAddress(address);
            log.info("Geocoding successful: {} {} -> (lat={}, lon={})",
                    address.getStreet(), address.getHouseNumber(),
                    coordinates.getLatitude(), coordinates.getLongitude());

            // 3. Coordinates im Model speichern
            model.saveCoordinates(coordinates);

            // 4. Weiterleitung an Transportplan via gRPC
            sendCoordinatesToTransportplan(coordinates);

        } catch (Exception e) {
            log.error("Geocoding failed for address: {} {}",
                    address.getStreet(), address.getHouseNumber(), e);
            throw new RuntimeException("Geocoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sendet Coordinates an den Transportplan via gRPC (asynchron).
     */
    private void sendCoordinatesToTransportplan(Coordinates coordinates) {
        transportplanClient.sendCoordinates(coordinates);
    }
}
