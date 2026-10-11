package com.itdib.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.itdib.model.ServerLocation;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;

/** Local MMDB lookups. No hostname resolution or remote geolocation requests. */
@Service
public class ServerLocationService {
    private final String databasePath;
    private DatabaseReader reader;

    public ServerLocationService(@Value("${geoip.database-path:}") String databasePath) {
        this.databasePath = databasePath;
    }

    public ServerLocation getLocation(String ipAddress) {
        InetAddress address = parseLiteral(ipAddress);
        try {
            CityResponse response = database().city(address);
            Location location = response.getLocation();
            if (location.getLatitude() == null || location.getLongitude() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No coordinates in this database");
            }
            ServerLocation result = new ServerLocation();
            result.setLatitude(String.valueOf(location.getLatitude()));
            result.setLongitude(String.valueOf(location.getLongitude()));
            return result;
        } catch (AddressNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found in this database");
        } catch (IOException | GeoIp2Exception | UnsupportedOperationException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Geolocation database unavailable");
        }
    }

    static InetAddress parseLiteral(String value) {
        if (value == null || value.length() > 45 || value.isEmpty()) throw invalidAddress();
        if (value.contains(":")) {
            // A colon and only IP characters prevent DNS hostname resolution.
            if (!value.matches("[0-9a-fA-F:.]+")) throw invalidAddress();
        } else {
            String[] octets = value.split("\\.", -1);
            if (octets.length != 4) throw invalidAddress();
            for (String octet : octets) {
                if (!octet.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octet) > 255) {
                    throw invalidAddress();
                }
            }
        }
        try { return InetAddress.getByName(value); }
        catch (UnknownHostException e) { throw invalidAddress(); }
    }

    private static ResponseStatusException invalidAddress() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a literal IPv4 or IPv6 address");
    }

    private synchronized DatabaseReader database() throws IOException {
        if (reader == null) {
            if (databasePath.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Set GEOIP_DATABASE_PATH to a City MMDB file");
            }
            reader = openDatabase(new File(databasePath));
        }
        return reader;
    }

    protected DatabaseReader openDatabase(File file) throws IOException {
        return new DatabaseReader.Builder(file).build();
    }

    @PreDestroy
    public synchronized void close() throws IOException {
        if (reader != null) { reader.close(); reader = null; }
    }
}
