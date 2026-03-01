package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CountriesNowService {
    private static final String BASE_URL = "https://countriesnow.space/api/v0.1/countries";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile boolean cacheLoaded = false;
    private List<String> countriesCache = Collections.emptyList();
    private Map<String, List<String>> citiesByCountryCache = Collections.emptyMap();

    private synchronized void ensureCacheLoaded() {
        if (cacheLoaded) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                cacheLoaded = true;
                countriesCache = Collections.emptyList();
                citiesByCountryCache = Collections.emptyMap();
                return;
            }

            JSONObject root = new JSONObject(response.body());
            JSONArray data = root.optJSONArray("data");
            if (data == null) {
                cacheLoaded = true;
                countriesCache = Collections.emptyList();
                citiesByCountryCache = Collections.emptyMap();
                return;
            }

            List<String> countries = new ArrayList<>();
            Map<String, List<String>> citiesByCountry = new HashMap<>();

            for (int i = 0; i < data.length(); i++) {
                JSONObject entry = data.optJSONObject(i);
                if (entry == null) continue;

                String country = entry.optString("country", "").trim();
                if (country.isEmpty()) continue;

                countries.add(country);

                JSONArray citiesArray = entry.optJSONArray("cities");
                List<String> cities = new ArrayList<>();
                if (citiesArray != null) {
                    for (int j = 0; j < citiesArray.length(); j++) {
                        String city = citiesArray.optString(j, "").trim();
                        if (!city.isEmpty()) cities.add(city);
                    }
                }
                cities.sort(Comparator.comparing(String::toLowerCase));
                citiesByCountry.put(country.toLowerCase(Locale.ROOT), cities);
            }

            countries.sort(Comparator.comparing(String::toLowerCase));
            countriesCache = countries;
            citiesByCountryCache = citiesByCountry;
            cacheLoaded = true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cacheLoaded = true;
            countriesCache = Collections.emptyList();
            citiesByCountryCache = Collections.emptyMap();
        } catch (IOException e) {
            cacheLoaded = true;
            countriesCache = Collections.emptyList();
            citiesByCountryCache = Collections.emptyMap();
        }
    }

    public List<String> getCountries() {
        ensureCacheLoaded();
        return new ArrayList<>(countriesCache);
    }

    public List<String> getCitiesByCountry(String country) {
        ensureCacheLoaded();
        if (country == null || country.isBlank()) {
            return Collections.emptyList();
        }

        List<String> cities = citiesByCountryCache.get(country.trim().toLowerCase(Locale.ROOT));
        if (cities != null) return new ArrayList<>(cities);

        // Fallback: normalize spacing/case to handle minor naming differences.
        String normalizedInput = country.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : citiesByCountryCache.entrySet()) {
            String normalizedKey = entry.getKey().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (normalizedKey.equals(normalizedInput)) {
                return new ArrayList<>(entry.getValue());
            }
        }
        return Collections.emptyList();
    }
}
