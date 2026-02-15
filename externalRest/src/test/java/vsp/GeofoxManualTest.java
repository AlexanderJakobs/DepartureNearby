//package vsp;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import vsp.app.ExternalRestApplicationConfig;
//import vsp.client.GeofoxClient;
//
//import java.time.Instant;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
///**
// * Manual Test - Ruft die echte Geofox API auf und gibt Abfahrten aus.
// *
// * Ausführen mit:
// *   ./gradlew :externalRest:test --tests "vsp.GeofoxManualTest.printDeparturesForLocation"
// *
// * WICHTIG: Geofox API Credentials müssen als Environment-Variablen gesetzt sein:
// *   - GEOFOX_API_USER
// *   - GEOFOX_API_PASSWORD
// */
//class GeofoxManualTest {
//
//    private GeofoxClient geofoxClient;
//
//    @BeforeEach
//    void setUp() {
//        ExternalRestApplicationConfig config = createConfig();
//        geofoxClient = new GeofoxClient(config);
//    }
//
//    private ExternalRestApplicationConfig createConfig() {
//        ExternalRestApplicationConfig config = new ExternalRestApplicationConfig();
//        ExternalRestApplicationConfig.ExternalApis apis = new ExternalRestApplicationConfig.ExternalApis();
//
//        // Geofox API config from application.properties
//        apis.setGeofoxBaseUrl("https://gti.geofox.de/gti/public");
//        apis.setGeofoxApiUser("AlexanderJakobs");
//        apis.setGeofoxApiPassword("FE1iy1B!99HX");
//        apis.setGeofoxTimeout(10000);
//
//        config.setExternalApis(apis);
//        return config;
//    }
//
//    private static final DateTimeFormatter TIME_FORMAT =
//            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
//
//    @Test
//    @DisplayName("Print departures for Hamburg Jungfernstieg (53.5531, 9.9927)")
//    void printDeparturesForLocation() {
//        // Hamburg Jungfernstieg area
//        double latitude = 53.5531;
//        double longitude = 9.9927;
//        int maxStations = 3;
//
//        printTransportPlan(latitude, longitude, maxStations);
//    }
//
//    @Test
//    @DisplayName("Print departures for Hamburg Hauptbahnhof")
//    void printDeparturesForHauptbahnhof() {
//        double latitude = 53.5530;
//        double longitude = 10.0069;
//        int maxStations = 3;
//
//        printTransportPlan(latitude, longitude, maxStations);
//    }
//
//    /**
//     * Prints the full transport plan for given coordinates.
//     */
//    private void printTransportPlan(double latitude, double longitude, int maxStations) {
//        System.out.println("\n" + "=".repeat(70));
//        System.out.println("TRANSPORTPLAN - Abfahrten");
//        System.out.println("=".repeat(70));
//        System.out.printf("Koordinaten: lat=%.4f, lon=%.4f%n", latitude, longitude);
//        System.out.printf("Max Stationen: %d%n", maxStations);
//        System.out.println("=".repeat(70));
//
//        Coordinates coordinates = Coordinates.newBuilder()
//                .setLatitude(latitude)
//                .setLongitude(longitude)
//                .build();
//
//        try {
//            List<DepartureStation> stations = geofoxClient.getDepartures(coordinates, maxStations);
//
//            if (stations.isEmpty()) {
//                System.out.println("\nKeine Stationen in der Nähe gefunden.");
//            } else {
//                System.out.printf("%n%d Stationen mit Abfahrten gefunden:%n%n", stations.size());
//
//                int stationNum = 1;
//                for (DepartureStation station : stations) {
//                    printStation(stationNum++, station);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("\nFEHLER: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        System.out.println("=".repeat(70));
//        System.out.println("ENDE TRANSPORTPLAN");
//        System.out.println("=".repeat(70) + "\n");
//    }
//
//    /**
//     * Prints a single station with all its departures.
//     */
//    private void printStation(int number, DepartureStation station) {
//        System.out.println("-".repeat(60));
//        System.out.printf("Station %d: %s%n", number, station.getStationName());
//        System.out.println("-".repeat(60));
//
//        if (station.getDeparturesCount() == 0) {
//            System.out.println("  Keine Abfahrten verfügbar");
//        } else {
//            System.out.printf("  %-30s %s%n", "Linie", "Abfahrt");
//            System.out.println("  " + "-".repeat(50));
//
//            for (Departure departure : station.getDeparturesList()) {
//                String lineName = departure.getLineName();
//                String time = formatDepartureTime(departure);
//                System.out.printf("  %-30s %s%n", lineName, time);
//            }
//        }
//        System.out.println();
//    }
//
//    /**
//     * Formats the departure time for display.
//     */
//    private String formatDepartureTime(Departure departure) {
//        if (!departure.hasDepartureTime()) {
//            return "N/A";
//        }
//
//        long seconds = departure.getDepartureTime().getSeconds();
//        Instant departureInstant = Instant.ofEpochSecond(seconds);
//        Instant now = Instant.now();
//
//        long minutesUntil = (seconds - now.getEpochSecond()) / 60;
//
//        String absoluteTime = TIME_FORMAT.format(departureInstant);
//
//        if (minutesUntil <= 0) {
//            return absoluteTime + " (jetzt)";
//        } else if (minutesUntil == 1) {
//            return absoluteTime + " (in 1 Min)";
//        } else {
//            return absoluteTime + " (in " + minutesUntil + " Min)";
//        }
//    }
//}