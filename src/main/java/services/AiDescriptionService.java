package services;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class AiDescriptionService {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String apiKey = dotenv.get("GEMINI_API_KEY");

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";


    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final Gson gson = new GsonBuilder().create();

    private String model = "gemini-flash-latest";

    public String generate(String name, String type, String destination, String duration) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Missing GEMINI_API_KEY environment variable.");
        }

        String prompt = buildPrompt(name, type, destination, duration);

        // 1) tentative avec modèle par défaut
        try {
            return callGenerateContent(model, prompt);
        } catch (RuntimeException ex) {
            // 2) fallback: récupérer un modèle compatible generateContent
            String fallback = findFirstGenerateContentModel()
                    .orElseThrow(() -> ex); // si on ne trouve rien, on relance l'erreur initiale

            // évite de refaire listModels à chaque fois
            this.model = fallback;

            return callGenerateContent(fallback, prompt);
        }
    }

    private String callGenerateContent(String modelName, String prompt) throws Exception {
        String url = BASE + "/models/" + modelName + ":generateContent?key=" + apiKey;

        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject c0 = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject p0 = new JsonObject();
        p0.addProperty("text", prompt);
        parts.add(p0);
        c0.add("parts", parts);
        contents.add(c0);
        body.add("contents", contents);

        // optionnel: un peu de "stabilité" dans le texte
        JsonObject genCfg = new JsonObject();
        genCfg.addProperty("temperature", 0.7);
        genCfg.addProperty("maxOutputTokens", 140);
        body.add("generationConfig", genCfg);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Gemini HTTP " + resp.statusCode() + " -> " + resp.body());
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();

        // candidates[0].content.parts[0].text
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0) return "";

        JsonObject cand0 = candidates.get(0).getAsJsonObject();
        JsonObject content = cand0.getAsJsonObject("content");
        if (content == null) return "";

        JsonArray outParts = content.getAsJsonArray("parts");
        if (outParts == null || outParts.size() == 0) return "";

        JsonObject out0 = outParts.get(0).getAsJsonObject();
        return out0.has("text") ? out0.get("text").getAsString() : "";
    }

    // ✅ ListModels + filtre ceux qui supportent generateContent
    private Optional<String> findFirstGenerateContentModel() throws Exception {
        String url = BASE + "/models?key=" + apiKey;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            return Optional.empty();
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray models = root.getAsJsonArray("models");
        if (models == null) return Optional.empty();

        for (JsonElement el : models) {
            JsonObject m = el.getAsJsonObject();

            // name: "models/gemini-2.5-flash"  -> on veut juste "gemini-2.5-flash"
            String fullName = m.has("name") ? m.get("name").getAsString() : "";
            if (!fullName.startsWith("models/")) continue;
            String shortName = fullName.substring("models/".length());

            // supportedGenerationMethods contient "generateContent"
            JsonArray methods = m.getAsJsonArray("supportedGenerationMethods");
            if (methods == null) continue;

            boolean ok = false;
            for (JsonElement me : methods) {
                if ("generateContent".equalsIgnoreCase(me.getAsString())) {
                    ok = true;
                    break;
                }
            }
            if (!ok) continue;

            // Priorité: flash
            if (shortName.contains("flash")) return Optional.of(shortName);
        }

        // sinon, prendre le premier compatible
        for (JsonElement el : models) {
            JsonObject m = el.getAsJsonObject();
            String fullName = m.has("name") ? m.get("name").getAsString() : "";
            if (!fullName.startsWith("models/")) continue;
            String shortName = fullName.substring("models/".length());

            JsonArray methods = m.getAsJsonArray("supportedGenerationMethods");
            if (methods == null) continue;

            for (JsonElement me : methods) {
                if ("generateContent".equalsIgnoreCase(me.getAsString())) {
                    return Optional.of(shortName);
                }
            }
        }

        return Optional.empty();
    }

    private String buildPrompt(String name, String type, String destination, String duration) {
        return """
You are a professional travel copywriter.

Write a short premium activity description for a tourism platform.

Activity details:
- Name: %s
- Type: %s
- Destination: %s
- Duration: %s

Rules:
- 2 to 3 sentences maximum
- Elegant and immersive tone
- Specific to the destination
- No emojis
- No mention of AI
- No price
- Avoid generic phrases like "Discover the beauty"

Make it sound exclusive and experiential.
""".formatted(name, type, destination, duration);
    }
}