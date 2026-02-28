package services;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class GeminiTipsService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");

    // ✅ MODEL CHANGED
    private static final String MODEL = "gemini-2.5-flash";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final Gson gson = new GsonBuilder().create();

    public List<String> extractTips(List<String> reviewsText) throws Exception {

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY manquante dans .env");
        }

        String joined = reviewsText.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(30)
                .collect(Collectors.joining("\n- ", "- ", ""));

        if (joined.isBlank()) {
            System.out.println("[TIPS] No reviews -> empty tips.");
            return List.of();
        }

        String prompt = """
        Extract practical travel tips from these reviews.

        Rules:
        - Output ONLY bullet points
        - 3 to 7 tips
        - MUST be actionable advice (imperative), e.g. "Arrive early", "Bring water"
        - If there are not enough actionable tips, you may rewrite review feedback into helpful advice.
          Example: "Fast paced" -> "Be prepared for a fast-paced visit."
        - No numbering, no intro, no quotes

        Reviews:
        %s
        """.formatted(joined);

        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();

        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + API_KEY;

        System.out.println("\n================ [TIPS] GEMINI REQUEST ================");
        System.out.println("[TIPS] Model: " + MODEL);
        System.out.println("[TIPS] Reviews count: " + reviewsText.size());
        System.out.println("=======================================================\n");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(40))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println("\n================ [TIPS] GEMINI RESPONSE ================");
        System.out.println("[TIPS] HTTP Status: " + resp.statusCode());
        System.out.println("[TIPS] RAW Body:");
        System.out.println(resp.body());
        System.out.println("========================================================\n");

        if (resp.statusCode() >= 400) {
            // 429 quota -> retourne vide (UI peut afficher "No tips")
            if (resp.statusCode() == 429) return List.of();
            throw new RuntimeException("Gemini error " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        String text = safeExtractText(root);

        System.out.println("\n============= [TIPS] GEMINI EXTRACTED TEXT =============");
        System.out.println(text);
        System.out.println("========================================================\n");

        List<String> tips = parseBullets(text);

        System.out.println("\n================= [TIPS] PARSED TIPS ===================");
        System.out.println("[TIPS] Tips count: " + tips.size());
        for (String t : tips) System.out.println("TIP: " + t);
        System.out.println("========================================================\n");

        return tips;
    }

    private String safeExtractText(JsonObject root) {
        try {
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return "";

            JsonObject cand0 = candidates.get(0).getAsJsonObject();
            JsonObject content = cand0.getAsJsonObject("content");
            if (content == null) return "";

            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) return "";

            JsonObject part0 = parts.get(0).getAsJsonObject();
            JsonElement txt = part0.get("text");
            return (txt == null) ? "" : txt.getAsString();
        } catch (Exception e) {
            System.out.println("[TIPS] safeExtractText failed: " + e.getMessage());
            return "";
        }
    }

    private List<String> parseBullets(String content) {
        if (content == null) return List.of();

        List<String> tips = new ArrayList<>();
        for (String line : content.split("\\R")) {
            String s = line.trim()
                    .replaceFirst("^[•\\-*]+\\s*", "")
                    .replaceFirst("^\\d+[\\).\\-]\\s*", "") // "1) " / "1. "
                    .trim();

            if (!s.isEmpty()) tips.add(s);
        }

        return tips.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .limit(7)
                .toList();
    }
}