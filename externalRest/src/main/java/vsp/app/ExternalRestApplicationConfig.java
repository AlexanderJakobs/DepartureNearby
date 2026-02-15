package vsp.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vsp")
public class ExternalRestApplicationConfig {
    private ExternalApis externalApis = new ExternalApis();
    private Rest rest = new Rest();

    public static class ExternalApis {
        // Nominatim API (Geocoding)
        private String nominatimBaseUrl;
        private int nominatimTimeout;

        // Geofox API (HVV Departures)
        private String geofoxBaseUrl;
        private String geofoxApiUser;
        private String geofoxApiPassword;
        private int geofoxTimeout;

        // Nominatim Getter & Setter
        public String getNominatimBaseUrl() { return nominatimBaseUrl; }
        public void setNominatimBaseUrl(String url) { this.nominatimBaseUrl = url; }

        public int getNominatimTimeout() { return nominatimTimeout; }
        public void setNominatimTimeout(int timeout) { this.nominatimTimeout = timeout; }

        // Geofox Getter & Setter
        public String getGeofoxBaseUrl() { return geofoxBaseUrl; }
        public void setGeofoxBaseUrl(String url) { this.geofoxBaseUrl = url; }

        public String getGeofoxApiUser() { return geofoxApiUser; }
        public void setGeofoxApiUser(String user) { this.geofoxApiUser = user; }

        public String getGeofoxApiPassword() { return geofoxApiPassword; }
        public void setGeofoxApiPassword(String password) { this.geofoxApiPassword = password; }

        public int getGeofoxTimeout() { return geofoxTimeout; }
        public void setGeofoxTimeout(int timeout) { this.geofoxTimeout = timeout; }
    }

    public static class Rest {
        private int timeout = 5000;

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    // Getter
    public ExternalApis getExternalApis() { return externalApis; }
    public void setExternalApis(ExternalApis apis) { this.externalApis = apis; }

    public Rest getRest() { return rest; }
    public void setRest(Rest rest) { this.rest = rest; }
}
