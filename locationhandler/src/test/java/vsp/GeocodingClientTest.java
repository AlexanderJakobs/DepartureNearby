package vsp;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vsp.client.GeocodingClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingClientTest {

    @Mock
    private GeocodingServiceGrpc.GeocodingServiceBlockingStub geocodingStub;

    @InjectMocks
    private GeocodingClient geocodingClient;

    @Test
    void getCoordinatesForAddress_WhenResponseHasCoordinates_ReturnsCoordinatesAndSetsMeta() {
        Address address = Address.newBuilder()
                .setStreet("Jungfernstieg")
                .setHouseNumber("1")
                .setCity("Hamburg")
                .build();

        Coordinates coords = Coordinates.newBuilder()
                .setLatitude(53.5511)
                .setLongitude(9.9937)
                .build();

        GeocodeResponse response = GeocodeResponse.newBuilder()
                .setCoordinates(coords)
                .build();

        when(geocodingStub.geocode(any(GeocodeRequest.class))).thenReturn(response);

        Coordinates result = geocodingClient.getCoordinatesForAddress(address);
        assertEquals(coords, result);

        ArgumentCaptor<GeocodeRequest> requestCaptor = ArgumentCaptor.forClass(GeocodeRequest.class);
        verify(geocodingStub, times(1)).geocode(requestCaptor.capture());

        GeocodeRequest sent = requestCaptor.getValue();
        assertTrue(sent.hasMeta());
        assertEquals("locationhandler", sent.getMeta().getCaller());
        assertNotNull(sent.getMeta().getCorrelationId());
        assertFalse(sent.getMeta().getCorrelationId().isBlank());
        assertEquals(address, sent.getAddress());
    }

    @Test
    void getCoordinatesForAddress_WhenResponseHasError_ThrowsGeocodingException() {
        Address address = Address.newBuilder().setStreet("X").setHouseNumber("1").build();

        GeocodeResponse response = GeocodeResponse.newBuilder()
                .setError(ErrorStatus.newBuilder()
                        .setCode(ErrorStatus.Code.NOT_FOUND)
                        .setMessage("not found")
                        .build())
                .build();

        when(geocodingStub.geocode(any(GeocodeRequest.class))).thenReturn(response);

        assertThrows(GeocodingClient.GeocodingException.class,
                () -> geocodingClient.getCoordinatesForAddress(address));
    }

    @Test
    void getCoordinatesForAddress_WhenStubThrowsStatusRuntimeException_ThrowsGeocodingException() {
        Address address = Address.newBuilder().setStreet("X").setHouseNumber("1").build();

        when(geocodingStub.geocode(any(GeocodeRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        GeocodingClient.GeocodingException ex = assertThrows(GeocodingClient.GeocodingException.class,
                () -> geocodingClient.getCoordinatesForAddress(address));

        assertTrue(ex.getMessage().toLowerCase().contains("unavailable")
                || ex.getMessage().toLowerCase().contains("service"));
    }
}
