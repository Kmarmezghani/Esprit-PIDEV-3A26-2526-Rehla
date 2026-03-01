package services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DescriptionApiService {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String DEFAULT_ENDPOINT = "http://localhost:8080/api/description/country";
    private static final String ENDPOINT = dotenv.get("COUNTRY_DESCRIPTION_API_URL", DEFAULT_ENDPOINT);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateCountryDescription(String countryName) throws IOException, InterruptedException {
        if (countryName == null || countryName.isBlank()) {
            throw new IllegalArgumentException("Country name is required.");
        }

        JSONObject payload = new JSONObject();
        payload.put("country", countryName.trim());
        payload.put("maxSentences", 2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Description API HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject root = new JSONObject(response.body());
        String description = root.optString("description", "").trim();
        if (description.isEmpty()) {
            throw new IOException("Description API returned empty description.");
        }
        return description;
    }
}
