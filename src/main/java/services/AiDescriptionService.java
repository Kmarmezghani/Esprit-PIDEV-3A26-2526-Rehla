package services;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class AiDescriptionService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("OPENROUTER_API_KEY");

    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    // ✅ timeouts plus grands
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final Gson gson = new Gson();

    // ✅ Garde ta liste (mais ajoute openrouter/auto en 1er => évite 404 des modèles)
    // (OpenRouter choisit un modèle dispo automatiquement)
    private final List<String> models = List.of(
            "openrouter/auto",
            "meta-llama/llama-3-8b-instruct",
            "mistralai/mistral-7b-instruct",
            "google/gemma-7b-it"
    );

    public String generate(String name,
                           String type,
                           String destination,
                           String duration,
                           List<String> attractions) throws Exception {

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new RuntimeException("Missing OPENROUTER_API_KEY.");
        }

        // ✅ IMPORTANT: ici attractions peut contenir des lignes riches (Name|Type|Hours|Note)
        List<String> cleanAttractions = attractions == null
                ? List.of()
                : attractions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(8)
                .collect(Collectors.toList());

        // ✅ 2 ou 3 attractions à forcer
        int required = (cleanAttractions.size() >= 3) ? 3 : (cleanAttractions.size() >= 2 ? 2 : 0);

        String prompt1 = buildPrompt(name, type, destination, duration, cleanAttractions, required, false);

        RuntimeException lastErr = null;

        for (String model : models) {
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    String out = callOpenRouter(model, prompt1);
                    out = clean(out);
                    out = enforceTwoSentences(out);

                    // ✅ si l'IA n'utilise pas assez d'attractions => retry strict (nouveau prompt)
                    if (required > 0 && !usesAtLeastNAttractions(out, cleanAttractions, required)) {
                        String prompt2 = buildPrompt(name, type, destination, duration, cleanAttractions, required, true);
                        String out2 = callOpenRouter(model, prompt2);
                        out2 = clean(out2);
                        out2 = enforceTwoSentences(out2);

                        // si encore mauvais => on force une réécriture stricte (100% AI)
                        if (!usesAtLeastNAttractions(out2, cleanAttractions, required)) {
                            String prompt3 = buildRewritePrompt(out2, cleanAttractions, required);
                            String out3 = callOpenRouter(model, prompt3);
                            out3 = clean(out3);
                            return enforceTwoSentences(out3);
                        }
                        return out2;
                    }

                    // ✅ si phrase incomplète / tronquée
                    if (looksTruncated(out)) {
                        String prompt2 = buildPrompt(name, type, destination, duration, cleanAttractions, required, true);
                        String out2 = callOpenRouter(model, prompt2);
                        out2 = clean(out2);
                        return enforceTwoSentences(out2);
                    }

                    return out;

                } catch (RuntimeException ex) {
                    lastErr = ex;

                    // si timeout / 429 / 503 => retry
                    if (!isRetryable(ex) || attempt == 3) break;

                    Thread.sleep(1000L * attempt);
                }
            }
        }

        throw (lastErr != null) ? lastErr : new RuntimeException("AI request failed.");
    }

    private String callOpenRouter(String model, String prompt) throws Exception {

        JsonObject body = new JsonObject();
        body.addProperty("model", model);

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content",
                "You write Airbnb-style activity listing copy: neutral, structured, practical, and concrete. "
                        + "Follow the constraints strictly and output ONLY two sentences.");
        messages.add(sys);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        body.add("messages", messages);

        JsonObject provider = new JsonObject();
        provider.addProperty("allow_fallbacks", true);
        body.add("provider", provider);

        body.addProperty("temperature", 0.35);
        body.addProperty("max_tokens", 520);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "TravelApp")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException te) {
            throw new RuntimeException("Timeout on model: " + model, te);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter HTTP " + response.statusCode() + " -> " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    private boolean isRetryable(RuntimeException ex) {
        String m = ex.getMessage();
        if (m == null) return false;
        String low = m.toLowerCase();
        return low.contains("timeout")
                || low.contains("timed out")
                || low.contains("http 429")
                || low.contains("rate")
                || low.contains("http 503")
                || low.contains("overloaded")
                || low.contains("unavailable");
    }

    /**
     * attractions: chaque ligne doit être idéalement:
     * - Name: Tour Eiffel | Type: Monument | Hours: 09:00-23:45 | Note: Vue panoramique...
     */
    private String buildPrompt(String name,
                               String type,
                               String destination,
                               String duration,
                               List<String> attractions,
                               int requiredAttractions,
                               boolean strict) {

        int minWords = strict ? 80 : 70;
        int maxWords = strict ? 115 : 105;

        String allowedBlock = attractions.isEmpty()
                ? "- None available"
                : attractions.stream()
                .map(a -> "- " + a)
                .collect(Collectors.joining("\n"));

        String must;
        if (requiredAttractions >= 3) {
            must = "Mention EXACTLY THREE attraction NAMES from the list and include ONE concrete detail (Hours/Type/Note) for at least TWO of them.";
        } else if (requiredAttractions == 2) {
            must = "Mention EXACTLY TWO attraction NAMES from the list and include ONE concrete detail (Hours/Type/Note) for BOTH.";
        } else {
            must = "If the list is empty, write a complete two-sentence route description without inventing place names.";
        }

        return """
Write EXACTLY two complete sentences in English, %d–%d words total.
Both sentences must end with a period.
Output ONLY the two sentences (no title, no bullets).

STYLE: Airbnb Experience listing.
Tone: friendly-professional, practical, not academic.

MANDATORY:
- Mention EXACTLY TWO or THREE attraction NAMES from the list.
- Use provided details (Type/Note/Hours) only if they help the flow naturally.
- Do NOT mechanically list opening hours.
- Do NOT repeat database wording.

Hard bans (do NOT use these words/phrases):
- world-famous, iconic, unforgettable, opportunity, panoramic views, explore, discover
- home to, offering, includes a visit to, covers, curated route
- "art collections and exhibits" (rewrite simply)

Structure:
- Sentence 1: what the day looks like + meeting point + first stop + timing logic (use Hours if provided).
- Sentence 2: transfer + second stop + pacing + how it fits the duration.

ATTRACTIONS (allowed names + details):
%s

Activity details:
Name: %s
Type: %s
Destination: %s
Duration: %s
""".formatted(
                minWords, maxWords,
                must,
                allowedBlock,
                safe(name), safe(type), safe(destination), safe(duration)
        );
    }

    private String buildRewritePrompt(String badOutput, List<String> attractions, int required) {
        // On extrait les "Name:" si le texte est riche, sinon on garde tel quel
        List<String> names = attractions.stream()
                .map(this::extractNameFromLine)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        String namesLine = names.isEmpty() ? "(none)" : String.join(", ", names);

        return """
Rewrite the text below into EXACTLY two complete sentences, 70–125 words total, Airbnb listing style.
Both sentences must end with a period.
You must mention %d attraction names from this allowed set (copy exact spelling): %s
Also include at least one provided detail (Hours/Type/Note) for each mentioned attraction, without inventing anything.
Output ONLY the two sentences.

TEXT:
%s
""".formatted(required, namesLine, badOutput);
    }

    private String extractNameFromLine(String line) {
        if (line == null) return "";
        String s = line.trim();
        // support: "Name: Tour Eiffel | Type: ..."
        int idx = s.toLowerCase().indexOf("name:");
        if (idx >= 0) {
            String after = s.substring(idx + 5).trim();
            int pipe = after.indexOf("|");
            return (pipe >= 0 ? after.substring(0, pipe) : after).trim();
        }
        // fallback: si c'est juste le nom
        int pipe = s.indexOf("|");
        return (pipe >= 0 ? s.substring(0, pipe) : s).trim();
    }

    private boolean usesAtLeastNAttractions(String text, List<String> attractionLines, int n) {
        if (n <= 0) return true;
        if (text == null) return false;

        String low = text.toLowerCase();
        int count = 0;

        // On matche sur les NOMS extraits (pas toute la ligne)
        for (String line : attractionLines) {
            String name = extractNameFromLine(line);
            if (name.isBlank()) continue;

            if (low.contains(name.toLowerCase())) {
                count++;
                if (count >= n) return true;
            }
        }
        return false;
    }

    private boolean looksTruncated(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        if (!t.endsWith(".")) return true;

        String low = t.toLowerCase();
        String[] badEnds = {" on.", " to.", " for.", " with.", " of.", " in.", " at.", " including.", " focusing on."};
        for (String be : badEnds) {
            if (low.endsWith(be)) return true;
        }
        return false;
    }

    private String enforceTwoSentences(String text) {
        if (text == null) return "";

        String s = text.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return s;

        String[] parts = s.split("(?<=\\.)\\s+");

        if (parts.length >= 2) {
            String out = parts[0].trim() + " " + parts[1].trim();
            if (!out.endsWith(".")) out += ".";
            return out;
        }

        if (!s.endsWith(".")) s += ".";
        return s;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}