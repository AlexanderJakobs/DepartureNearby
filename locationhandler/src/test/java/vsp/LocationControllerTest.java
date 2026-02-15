package vsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.GeocodingClient;
import vsp.client.TransportplanClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private GeocodingClient geocodingClient;

    @Mock
    private TransportplanClient transportplanClient;

    private LocationModel model;
    private LocationController controller;

    @BeforeEach
    void setUp() {
        model = spy(new LocationModel());
        controller = new LocationController(model, geocodingClient, transportplanClient);
    }

    @Test
    void onResolveLocationRequest_WithValidAddress_SavesAndForwards() {
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .setCity("Hamburg")
                .build();

        Coordinates coords = Coordinates.newBuilder()
                .setLatitude(53.5511)
                .setLongitude(9.9937)
                .build();

        when(geocodingClient.getCoordinatesForAddress(address)).thenReturn(coords);

        controller.onResolveLocationRequest(address);

        assertEquals(address, model.getAddress());
        assertEquals(coords, model.getCoordinates());
        assertTrue(model.hasCoordinates());

        verify(geocodingClient, times(1)).getCoordinatesForAddress(address);
        verify(transportplanClient, times(1)).sendCoordinates(coords);
    }

    @Test
    void onResolveLocationRequest_WhenGeocodingFails_ThrowsRuntimeAndDoesNotSendToTransportplan() {
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .setCity("Hamburg")
                .build();

        when(geocodingClient.getCoordinatesForAddress(address))
                .thenThrow(new GeocodingClient.GeocodingException("not found"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.onResolveLocationRequest(address));
        assertTrue(ex.getMessage().contains("Geocoding failed"));

        assertEquals(address, model.getAddress());
        assertNull(model.getCoordinates());

        verify(transportplanClient, never()).sendCoordinates(any());
    }
}
