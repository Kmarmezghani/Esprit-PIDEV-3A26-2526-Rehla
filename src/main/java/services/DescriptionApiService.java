package services;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DescriptionApiService {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY_SECONDARY");
    private static final String MODEL = "gemini-2.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateCountryDescription(String countryName) throws IOException, InterruptedException {
        if (countryName == null || countryName.isBlank()) {
            throw new IllegalArgumentException("Country name is required.");
        }
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_API_KEY in .env");
        }

        String prompt = """
                Write a short tourism description for the country "%s".
                Rules:
                - exactly 2 sentences
                - maximum 70 words total
                - factual, clear, and friendly
                - no markdown, no emojis, no bullet points
                - output only the description text
                """.formatted(countryName.trim());

        JSONObject part = new JSONObject().put("text", prompt);
        JSONArray parts = new JSONArray().put(part);
        JSONObject content = new JSONObject().put("parts", parts);
        JSONArray contents = new JSONArray().put(content);
        JSONObject payload = new JSONObject().put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + API_KEY))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Description API HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject root = new JSONObject(response.body());
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IOException("Gemini response has no candidates.");
        }

        JSONObject first = candidates.getJSONObject(0);
        JSONObject geminiContent = first.optJSONObject("content");
        if (geminiContent == null) {
            throw new IOException("Gemini response missing content.");
        }
        JSONArray geminiParts = geminiContent.optJSONArray("parts");
        if (geminiParts == null || geminiParts.isEmpty()) {
            throw new IOException("Gemini response missing text parts.");
        }

        String description = geminiParts.getJSONObject(0).optString("text", "").trim();
        if (description.isEmpty()) {
            throw new IOException("Gemini returned empty description.");
        }
        return description.replaceAll("\\s+", " ");
    }
}