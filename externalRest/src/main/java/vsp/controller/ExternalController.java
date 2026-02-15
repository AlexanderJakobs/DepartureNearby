package vsp.controller;


import com.google.protobuf.Timestamp;
import vsp.service.OrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class ExternalController {

    private static final Logger log = LoggerFactory.getLogger(ExternalController.class);
    private final OrchestrationService orchestrationService;

    public ExternalController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
        log.info("ExternalController initialized");
    }

    @GetMapping("/location")
    public ResponseEntity<String> userPassLocation(@RequestParam String address) {
        log.debug("Received request for user pass location");
        if (address == null || address.trim().isEmpty()) {
            log.debug("Received request with no address");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No input detected.");
        }
        try {
            // Aufruf des OrchestrationServices, der an den Displaymanager weitergibt
            if (orchestrationService.addRequest(address)){
                log.info("Handing location request over to OrchestrationService");
                return ResponseEntity.status(HttpStatus.OK).body(String.format("Address received. Searching for nearest departures at %s now...",address));
            } else {
                log.error("Failed to hand over location request to OrchestrationService due to invalid address format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Address format not valid.");
            }


        } catch (Exception e) {
            log.error("Failed to handle location request: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatusUpdate(){
        log.debug("Received request for status update");
        try {
            Timestamp lastInteraction = orchestrationService.getLastInteraction();
            log.info("Handled request for status update. Last interaction at: {}", lastInteraction.toString());
            return ResponseEntity.status(HttpStatus.OK).body(lastInteraction.toString());
        } catch (NoSuchElementException e) {
            log.info("Handled request for status update. No interaction with data supplier yet.");
            return ResponseEntity.status(HttpStatus.OK).body("No supplier data received yet.");
        } catch (Exception e) {
            log.error("Failed to handle status update request: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: " + e.getMessage());
        }
    }
    // Health-Check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(" IT IS OK");
    }
}
