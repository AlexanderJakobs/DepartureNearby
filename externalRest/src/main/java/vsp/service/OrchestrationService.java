package vsp.service;

import com.google.protobuf.Timestamp;
import vsp.client.DisplaymanagerClient;
import vsp.client.GeofoxClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

@Service
public class OrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    private final DisplaymanagerClient displaymanagerClient;

    private final GeofoxClient geofoxClient;

    public OrchestrationService(DisplaymanagerClient displaymanagerClient, GeofoxClient geofoxClient) {
        this.displaymanagerClient = displaymanagerClient;
        this.geofoxClient = geofoxClient;
        log.info("OrchestrationService initialized");
    }

    public boolean addRequest(String input) {
        displaymanagerClient.sendUserPassLocation(input);
        log.info("Added request with address: {}", input);
        return true;
    }

    public Timestamp getLastInteraction() {
        log.info("Getting last interaction");
        if (geofoxClient.getLastInteraction() == null) {
            log.debug("No last interaction found");
            throw new NoSuchElementException("No last interaction with data supplier.");
        }
        Timestamp lastInteraction = geofoxClient.getLastInteraction();
        log.debug("Last interaction at: {}", lastInteraction);
        return lastInteraction;
    }
}
