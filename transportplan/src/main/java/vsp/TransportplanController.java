package vsp;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vsp.client.DeparturesClient;
import vsp.client.DisplayClient;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller für den Transportplan (MC-Pattern).
 * Empfängt Coordinates vom Locationhandler (via gRPC),
 * ruft DeparturesClient (via externalRest -> Geofox) für Abfahrten auf,
 * und sendet DepartureStations an DisplayManager (via gRPC).
 */
@Component
public class TransportplanController {

    private static final Logger log = LoggerFactory.getLogger(TransportplanController.class);

    private final TransportplanModel model;
    private final DeparturesClient departuresClient;
    private final DisplayClient displayClient;

    public TransportplanController(
            TransportplanModel model,
            DeparturesClient departuresClient,DisplayClient displayClient) {
        this.model = model;
        this.departuresClient = departuresClient;
        this.displayClient = displayClient;
        log.info("TransportplanController initialized");
    }

    /**
     * ENTRY POINT: Empfängt Coordinates vom Locationhandler (via gRPC).
     * Wird vom TransportplanIngressService aufgerufen.
     *
     * @param coordinates Die Koordinaten für die Abfahrtssuche
     * @param correlationId Die Correlation-ID für Tracing
     */
    public void onGetDeparturesRequest(Coordinates coordinates, String correlationId) {
        log.info("Transportplan received coordinates: lat={}, lon={}, correlationId={}",
                coordinates.getLatitude(), coordinates.getLongitude(), correlationId);

        // 1. Coordinates im Model speichern
        model.saveCoordinates(coordinates);

        try {
            // 2. Stationen via DeparturesClient abrufen (calls externalRest -> Geofox)
            List<DepartureStation> responseStations = departuresClient.getNearbyStations(coordinates,50);

            log.info("ExternalRest returned {} stations with departures, correlationId={}",
                    responseStations.size(), correlationId);

            // 2.1 Nach Distanz sortieren und nur die ersten drei extrahieren
            responseStations = responseStations.stream()
                    .sorted(Comparator.comparingDouble(DepartureStation::getDistance))
                    .limit(3)
                    .collect(Collectors.toCollection(ArrayList::new));


            // 2,5. Abfahrten für Stationen via DeparturesClient abrufen
            List<DepartureStation> departureStations = departuresClient.getDepartures(responseStations);


            // 3. DepartureStations im Model speichern
            model.saveDeparturesMap(departureStations);

            // 4. DepartureStations an DisplayClient weiterleiten (an dieser Stelle ist der async-Call im Controller, da vorher noch ein blockierender Call ausgeführt wird um die Departures zu bekommen von HVV)

            CompletableFuture.runAsync(()-> {
                try {
                    displayClient.sendDeparturesToDisplayManager(departureStations, correlationId);
                } catch (Exception e) {
                    log.error("Error in async processing (ACK already sent)", e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to fetch departures, correlationId={}", correlationId, e);
            // Leere Liste senden im Fehlerfall mit aktuellem Timestamp
            displayClient.sendDeparturesToDisplayManager(List.of(), correlationId);
        }
    }



}
