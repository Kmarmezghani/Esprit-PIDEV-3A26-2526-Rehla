package services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service for IP geolocation using ipapi.co API.
 * Free tier: 1000 requests/day.
 */
public class GeoIPService {

    private static final String IPAPI_URL = "https://ipapi.co/%s/json/";
    private static final String IPAPI_SELF_URL = "https://ipapi.co/json/";

    private static GeoIPService instance;

    private GeoIPService() {}

    public static synchronized GeoIPService getInstance() {
        if (instance == null) {
            instance = new GeoIPService();
        }
        return instance;
    }

    /**
     * Gets location info for a specific IP address.
     */
    public LocationInfo getLocation(String ipAddress) {
        try {
            String urlStr = (ipAddress == null || ipAddress.isBlank())
                    ? IPAPI_SELF_URL
                    : String.format(IPAPI_URL, ipAddress);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Rehla-App/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());

                    if (json.has("error") && json.getBoolean("error")) {
                        System.err.println("[GeoIP] API error: " + json.optString("reason", "Unknown error"));
                        return getDefaultLocation();
                    }

                    LocationInfo info = new LocationInfo();
                    info.ip = json.optString("ip", "");
                    info.city = json.optString("city", "Unknown");
                    info.region = json.optString("region", "");
                    info.country = json.optString("country_name", "Unknown");
                    info.countryCode = json.optString("country_code", "");
                    info.latitude = json.optDouble("latitude", 0.0);
                    info.longitude = json.optDouble("longitude", 0.0);
                    info.timezone = json.optString("timezone", "");
                    info.isp = json.optString("org", "");

                    System.out.println("[GeoIP] Location resolved: " + info.city + ", " + info.country);
                    return info;
                }
            } else {
                System.err.println("[GeoIP] HTTP error: " + responseCode);
                return getDefaultLocation();
            }
        } catch (Exception e) {
            System.err.println("[GeoIP] Error getting location: " + e.getMessage());
            return getDefaultLocation();
        }
    }

    /**
     * Gets the current machine's location based on its public IP.
     */
    public LocationInfo getCurrentLocation() {
        return getLocation(null);
    }

    /**
     * Returns a default location for when the API fails.
     */
    private LocationInfo getDefaultLocation() {
        LocationInfo info = new LocationInfo();
        info.ip = "127.0.0.1";
        info.city = "Local";
        info.country = "Unknown";
        info.countryCode = "XX";
        return info;
    }

    /**
     * Data class holding location information.
     */
    public static class LocationInfo {
        public String ip = "";
        public String city = "";
        public String region = "";
        public String country = "";
        public String countryCode = "";
        public double latitude = 0.0;
        public double longitude = 0.0;
        public String timezone = "";
        public String isp = "";

        @Override
        public String toString() {
            return city + ", " + country + " (" + countryCode + ")";
        }

        public String toShortString() {
            return city + ", " + countryCode;
        }
    }
}
