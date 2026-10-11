package com.itdib.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.itdib.model.ServerLocation;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;

class ServerLocationServiceTests {
    @Test
    void acceptsLiteralIpv4AndIpv6AndRejectsHostnames() {
        assertEquals("192.0.2.1", ServerLocationService.parseLiteral("192.0.2.1").getHostAddress());
        assertEquals(16, ServerLocationService.parseLiteral("2001:db8::1").getAddress().length);
        for (String invalid : new String[]{"example.com", "localhost", "127.1", "999.1.1.1", "01.2.3.4", "1.2.3.4/24", "fe80::1%eth0", "", "a:b:c"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> ServerLocationService.parseLiteral(invalid));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        }
    }

    @Test
    void missingDatabaseReturnsActionable503() {
        ServerLocationService service = new ServerLocationService("");
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.getLocation("192.0.2.1"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        assertTrue(error.getReason().contains("GEOIP_DATABASE_PATH"));
    }

    private ServerLocationService serviceWith(DatabaseReader reader) throws IOException {
        ServerLocationService service = spy(new ServerLocationService("fixture.mmdb"));
        doReturn(reader).when(service).openDatabase(any(File.class));
        return service;
    }

    @Test
    void returnsCoordinatesReusesReaderAndClosesIt() throws Exception {
        DatabaseReader reader = mock(DatabaseReader.class);
        Location location = mock(Location.class);
        CityResponse response = new CityResponse(null, null, null, location, null, null, null, null, null, null);
        when(reader.city(any(InetAddress.class))).thenReturn(response);
        when(location.getLatitude()).thenReturn(12.5);
        when(location.getLongitude()).thenReturn(-34.5);
        ServerLocationService service = serviceWith(reader);
        ServerLocation result = service.getLocation("192.0.2.1");
        assertEquals("12.5", result.getLatitude());
        assertEquals("-34.5", result.getLongitude());
        service.getLocation("2001:db8::1");
        verify(service, times(1)).openDatabase(any(File.class));
        service.close();
        verify(reader).close();
    }

    @Test
    void absentAddressReturns404AndIoFailureDoesNotDisclosePaths() throws Exception {
        DatabaseReader reader = mock(DatabaseReader.class);
        when(reader.city(any(InetAddress.class))).thenThrow(new AddressNotFoundException("not present"));
        ServerLocationService service = serviceWith(reader);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.getLocation("192.0.2.1")).getStatus());
        reset(reader);
        when(reader.city(any(InetAddress.class))).thenThrow(new IOException("private filesystem detail"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.getLocation("192.0.2.1"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        assertFalse(error.getReason().contains("private filesystem detail"));
        reset(reader);
        when(reader.city(any(InetAddress.class))).thenThrow(new UnsupportedOperationException("wrong database type"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class, () -> service.getLocation("192.0.2.1")).getStatus());
    }

    @Test
    void missingCoordinatesReturns404() throws Exception {
        DatabaseReader reader = mock(DatabaseReader.class);
        CityResponse response = new CityResponse(null, null, null, new Location(), null, null, null, null, null, null);
        when(reader.city(any(InetAddress.class))).thenReturn(response);
        ServerLocationService service = serviceWith(reader);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.getLocation("192.0.2.1")).getStatus());
    }
}
