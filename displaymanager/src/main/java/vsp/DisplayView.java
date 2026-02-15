package vsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Ein simpler Monitor, der die Ergebnisse des Nearest-Departure-Systems anzeigt
 */
@Component
public class DisplayView {
    private static final Logger log = LoggerFactory.getLogger(DisplayView.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    /**
     * Zeigt Departures auf dem Monitor (Konsole) an
     */
    public void showDeparturesToMonitor(List<DepartureStation> departureStations) {
        showDeparturesToMonitor(departureStations, null);
    }

    /**
     * Zeigt Departures auf dem Monitor (Konsole) an mit Timestamp
     */
    public void showDeparturesToMonitor(List<DepartureStation> departureStations, com.google.protobuf.Timestamp dataFetchedAt) {
        System.out.println("\n  " + "=".repeat(87));
        System.out.println("                              DEPARTURE MONITOR");
        System.out.println("                  (Data available for Hamburg/HVV region only)");
        System.out.println("  " + "=".repeat(87));

        if (departureStations == null || departureStations.isEmpty()) {
            showError("No departures found");
            return;
        } else {
            LocalDateTime now = LocalDateTime.now(ZONE_ID);

            // Header: nur LINE und TIME
            System.out.printf("  %-27s %-10s %-10s %30s%n",
                    "LINE", "IN", "TIME", "STOP");
            System.out.println("  " + "-".repeat(87));

            int totalDepartures = 0;
            for (DepartureStation station : departureStations) {
                if (station.getDeparturesList().isEmpty()) {
                    // Station ohne Abfahrten anzeigen
                    System.out.printf("  %-27s %-10s %-10s %30s%n",
                            "-",
                            "-",
                            "-",
                            truncate(station.getStationName(), 30));
                } else {
                    for (Departure dep : station.getDeparturesList()) {
                        totalDepartures++;

                        // Konvertiere Protobuf Timestamp zu LocalDateTime
                        LocalDateTime departureTime = convertTimestamp(dep.getDepartureTime());

                        // Berechne Minuten bis Abfahrt
                        long minutesUntil = ChronoUnit.MINUTES.between(now, departureTime);
                        String minutesDisplay = formatMinutesUntil(minutesUntil);

                        // Formatiere Uhrzeit
                        String timeDisplay = departureTime.format(TIME_FORMATTER);

                        System.out.printf("  %-27s %-10s %-10s %30s%n",
                                truncate(dep.getLineName(), 20),
                                minutesDisplay,
                                timeDisplay,
                                truncate(station.getStationName(), 30));
                    }
                }
            }
            System.out.println("  " + "-".repeat(87));
            System.out.printf("  Total: %d departures from %d stations%n", totalDepartures, departureStations.size());

            // Use passed timestamp if available, otherwise use current time
            LocalDateTime fetchedAt = dataFetchedAt != null
                    ? convertTimestamp(dataFetchedAt)
                    : now;
            System.out.printf("  Last data update: %s%n", fetchedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        }
        System.out.println("  " + "=".repeat(87) + "\n");
    }

        /**
     * Konvertiert Protobuf Timestamp zu LocalDateTime
     */
    private LocalDateTime convertTimestamp(com.google.protobuf.Timestamp timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now(ZONE_ID);
        }
        
        Instant instant = Instant.ofEpochSecond(
            timestamp.getSeconds(), 
            timestamp.getNanos()
        );
        
        return LocalDateTime.ofInstant(instant, ZONE_ID);
    }

    /**
     * Formatiert Minuten bis Abfahrt für bessere Lesbarkeit
     */
    private String formatMinutesUntil(long minutes) {
        if (minutes < 0) {
            return "departed";
        } else if (minutes == 0) {
            return "now";
        } else if (minutes == 1) {
            return "1 min";
        } else if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return String.format("%dh %02dm", hours, remainingMinutes);
        }
    }

    /**
     * Benachrichtigung: Adresse wurde gespeichert
     */
    public void notifyAddressSaved() {
        log.info("[MONITOR] Address saved - processing...");
    }

    /**
     * Benachrichtigung: Departures wurden gespeichert
     */
    public void notifyDeparturesSaved() {
        log.info("[MONITOR] Departures received - displaying...");
    }

    /**
     * Zeigt eine Fehlermeldung an
     */
    public void showError(String message) {
        System.err.println("\n" + "!".repeat(80));
        System.err.println("  ERROR: " + message);
        System.err.println("!".repeat(80) + "\n");
    }

    /**
     * Zeigt Loading-Status an
     */
    public void showLoading(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null.");
        }

        System.out.println("\n[MONITOR] Searching departures near: " + address.getStreet() + " " + address.getHouseNumber());
        System.out.println("[MONITOR] Please wait...\n");
    }

    // Helper: Text abschneiden wenn zu lang
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength - 3) + "..."
                : text;
    }
}

