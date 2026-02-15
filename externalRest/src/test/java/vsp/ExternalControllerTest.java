package vsp;

import vsp.controller.ExternalController;
import vsp.service.OrchestrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalControllerTest {

    @Mock
    private OrchestrationService orchestrationService;

    @InjectMocks
    private ExternalController externalController;

    @Test
    void userPassLocation_ValidAddress_ReturnsOk() {
        // Arrange
        String address = "Hamburg 1";
        when(orchestrationService.addRequest(address)).thenReturn(true);

        // Act
        ResponseEntity<?> response = externalController.userPassLocation(address);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains(address));
        verify(orchestrationService, times(1)).addRequest(address);
    }

    @Test
    void userPassLocation_NullAddress_ReturnsBadRequest() {
        // Act
        ResponseEntity<?> response = externalController.userPassLocation(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No input detected.", response.getBody());
        verify(orchestrationService, never()).addRequest(anyString());
    }

    @Test
    void userPassLocation_EmptyAddress_ReturnsBadRequest() {
        // Act
        ResponseEntity<?> response = externalController.userPassLocation("");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No input detected.", response.getBody());
        verify(orchestrationService, never()).addRequest(anyString());
    }

    @Test
    void health_ReturnsOk() {
        // Act
        ResponseEntity<String> response = externalController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(" IT IS OK", response.getBody());
    }
}