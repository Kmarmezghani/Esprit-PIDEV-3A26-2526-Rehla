package services;

import io.github.cdimascio.dotenv.Dotenv;
import models.Activite;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class GeminiRecommendationService {

    private static final boolean DEBUG = true;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // ✅ Timeout pour éviter attente longue
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    public List<Integer> rankActivityIdsMax3(List<Activite> candidates, String userProfileText) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            if (DEBUG) System.out.println("[Gemini] API_KEY manquante");
            return List.of();
        }
        if (candidates == null || candidates.isEmpty()) return List.of();

        // ✅ Limite candidats (rapide)
        List<Activite> list = candidates.stream().limit(18).toList();

        // ✅ payload léger: description courte
        String activitiesJson = list.stream()
                .map(a -> String.format(
                        "{\"id\":%d,\"name\":%s,\"type\":%s,\"price\":%.2f,\"rating\":%.1f,\"desc\":%s}",
                        a.getId(),
                        js(safe(a.getNom())),
                        js(safe(a.getTypeActivite())),
                        a.getPrix(),
                        a.getNoteMoyenne(),
                        js(shortText(safe(a.getDescription()), 80))
                ))
                .collect(Collectors.joining(",", "[", "]"));

        String prompt = """
        Tu es un moteur de recommandation d’activités de voyage.
        Choisis et classe entre 0 et 3 activités maximum adaptées au profil utilisateur.
        Réponds UNIQUEMENT avec du JSON minifié, sans markdown, sans backticks.

        Format EXACT attendu :
        {"ranked_ids":[...]}  // 0..3 ids existants dans la liste

        Profil utilisateur:
        %s

        Activités candidates (JSON):
        %s

        Règles:
        - Respecte budgetMin/budgetMax si possible
        - Match centresInteret/typesVoyage avec name/type/desc
        - Si aucune activité ne correspond, retourne {"ranked_ids":[]}
        - Ne renvoie jamais plus de 3 IDs
        """.formatted(userProfileText, activitiesJson);

        String body = """
        {
          "contents": [
            { "parts": [ { "text": %s } ] }
          ]
        }
        """.formatted(js(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + API_KEY))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                break;
            } catch (Exception ex) {
                if (DEBUG) System.out.println("[Gemini] attempt " + attempt + " failed: " + ex.getMessage());
                if (attempt == 2) return List.of();
                Thread.sleep(500);
            }
        }

        if (DEBUG) {
            System.out.println("\n===== GEMINI RAW RESPONSE (HTTP " + response.statusCode() + ") =====");
            System.out.println(response.body());
            System.out.println("===============================================================\n");
        }

        if (response.statusCode() != 200) return List.of();

        String modelText = extractModelText(response.body());
        if (modelText == null) return List.of();

        if (DEBUG) {
            System.out.println("===== GEMINI MODEL TEXT =====");
            System.out.println(modelText);
            System.out.println("=============================\n");
        }

        modelText = modelText.replace("```json", "").replace("```", "").trim();

        List<Integer> ids = extractRankedIds(modelText);
        if (ids.size() > 3) ids = ids.subList(0, 3);

        if (DEBUG) {
            System.out.println("===== EXTRACTED ranked_ids =====");
            System.out.println(ids);
            System.out.println("================================\n");
        }

        return ids;
    }
    // ---- parse candidates[0].content.parts[0].text ----
    private String extractModelText(String json) {
        int i = json.indexOf("\"text\"");
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;

        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        for (int k = start + 1; k < json.length(); k++) {
            char c = json.charAt(k);

            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escape = false;
            } else {
                if (c == '\\') escape = true;
                else if (c == '"') break;
                else sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<Integer> extractRankedIds(String json) {
        int key = json.indexOf("\"ranked_ids\"");
        if (key < 0) return List.of();

        int start = json.indexOf('[', key);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) return List.of();

        String inside = json.substring(start + 1, end).trim();
        if (inside.isEmpty()) return List.of();

        List<Integer> ids = new ArrayList<>();
        for (String p : inside.split(",")) {
            try { ids.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {}
        }
        return ids;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String shortText(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", " ").trim();
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private static String js(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"";
    }
}