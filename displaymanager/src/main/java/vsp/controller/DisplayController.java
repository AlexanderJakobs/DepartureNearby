package vsp.controller;


import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.Address;
import vsp.DepartureStation;
import vsp.Departure;
import vsp.DisplayModel;
import vsp.DisplayView;
import vsp.client.LocationClient;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class DisplayController {

    private static final Logger log = LoggerFactory.getLogger(DisplayController.class);
    private final DisplayView view;
    private final DisplayModel model;
    private final LocationClient locationClient;
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "^([\\p{L}\\s.-]+)\\s+(\\d+[a-zA-Z]?)$"
    );

    public DisplayController(DisplayView view, DisplayModel model, LocationClient locationClient) {
        this.view = view;
        this.model = model;
        this.locationClient = locationClient;
    }

    /**
     * ENTRY POINT: Empfängt Adresse von Externalrest (via gRPC)
     *
     * EXIT POINT: Leitet die Adresse weiter an den gRPC-Stub des DisplayManagers
     */
    public void userPassLocation(String input){
        log.info("DisplayManager received Address: {}", input);
        // Aus String Datentyp Adresse erstellen
        Address address = parseAddress(input);
        model.saveAddress(address);
        // Stub nutzen und Adresse an Locationhandler weitergeben
        locationClient.sendUserPassLocation(address);
        view.showLoading(address);
    }

    /**
     * ENTRY POINT: Empfängt Departures von Transportplan (via gRPC)
     * Leitet die Departures an die View weiter damit sie auf dem Monitor angezeigt werden
     */
    public void displayDepartures(List<DepartureStation> departureStations){
        displayDepartures(departureStations, null);
    }

    /**
     * ENTRY POINT: Empfängt Departures mit Timestamp von Transportplan (via gRPC)
     * Leitet die Departures an die View weiter damit sie auf dem Monitor angezeigt werden
     */
    public void displayDepartures(List<DepartureStation> departureStations, Timestamp dataFetchedAt){
        if (departureStations == null){
            throw new IllegalArgumentException("departures cannot be null");
        }
        log.info("DisplayManager received departures: {}",departureStations.size());

        model.saveDepartures(departureStations);

        model.displayDepartures(departureStations, dataFetchedAt);
    }

    private Address parseAddress(String input){
        if (input == null) {
            throw new IllegalArgumentException("Address is null");
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(input.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Address input not valid");
        }

        String street = matcher.group(1).trim();
        String houseNumber = matcher.group(2);

        return Address.newBuilder().setStreet(street).setHouseNumber(houseNumber).build();
    }


}