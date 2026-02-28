package services;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Face Recognition Service using Face++ API.
 * 
 * Face++ (Megvii) provides:
 * - Free tier: 1000 API calls/month
 * - Face detection and comparison
 * - No need to store images - just face tokens
 * 
 * Sign up at: https://www.faceplusplus.com/
 * Get API Key and Secret from console.
 */
public class FaceRecognitionService {

    private static final String FACEPP_DETECT_URL = "https://api-us.faceplusplus.com/facepp/v3/detect";
    private static final String FACEPP_COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";
    
    private static final String FACE_DATA_FILE = "face_enrollments.json";
    private static final double CONFIDENCE_THRESHOLD = 80.0;

    private static FaceRecognitionService instance;
    
    private String apiKey;
    private String apiSecret;
    private Map<String, String> enrolledFaces;

    private FaceRecognitionService() {
        loadConfig();
        loadEnrolledFaces();
    }

    public static synchronized FaceRecognitionService getInstance() {
        if (instance == null) {
            instance = new FaceRecognitionService();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                apiKey = props.getProperty("facepp.api.key", "");
                apiSecret = props.getProperty("facepp.api.secret", "");
            }
        } catch (IOException e) {
            System.err.println("[FaceRecognition] Error loading config: " + e.getMessage());
        }
    }

    private void loadEnrolledFaces() {
        enrolledFaces = new HashMap<>();
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".rehla", FACE_DATA_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                JSONObject json = new JSONObject(content);
                for (String key : json.keySet()) {
                    enrolledFaces.put(key, json.getString(key));
                }
                System.out.println("[FaceRecognition] Loaded " + enrolledFaces.size() + " enrolled faces");
            }
        } catch (Exception e) {
            System.err.println("[FaceRecognition] Error loading enrolled faces: " + e.getMessage());
        }
    }

    private void saveEnrolledFaces() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".rehla");
            Files.createDirectories(dir);
            Path path = dir.resolve(FACE_DATA_FILE);
            
            JSONObject json = new JSONObject(enrolledFaces);
            Files.writeString(path, json.toString(2));
            System.out.println("[FaceRecognition] Saved enrolled faces");
        } catch (Exception e) {
            System.err.println("[FaceRecognition] Error saving enrolled faces: " + e.getMessage());
        }
    }

    /**
     * Checks if Face++ API is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && 
               !apiKey.equals("YOUR_FACEPP_API_KEY") &&
               apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Checks if a user has enrolled their face.
     */
    public boolean isUserEnrolled(String email) {
        return enrolledFaces.containsKey(email.toLowerCase());
    }

    /**
     * Enrolls a user's face from an image.
     * 
     * @param email User's email
     * @param faceImage BufferedImage of the user's face
     * @return true if enrollment successful
     */
    public FaceResult enrollFace(String email, BufferedImage faceImage) {
        if (!isConfigured()) {
            return new FaceResult(false, "Face++ API not configured. Please set API keys in config.properties");
        }

        try {
            String base64Image = imageToBase64(faceImage);
            
            String faceToken = detectFace(base64Image);
            
            if (faceToken == null) {
                return new FaceResult(false, "No face detected in the image. Please ensure your face is clearly visible.");
            }

            enrolledFaces.put(email.toLowerCase(), faceToken);
            saveEnrolledFaces();

            System.out.println("[FaceRecognition] Enrolled face for: " + email);
            return new FaceResult(true, "Face enrolled successfully!");

        } catch (Exception e) {
            System.err.println("[FaceRecognition] Enrollment error: " + e.getMessage());
            return new FaceResult(false, "Error enrolling face: " + e.getMessage());
        }
    }

    /**
     * Verifies a face against the enrolled face for a user.
     * 
     * @param email User's email
     * @param faceImage BufferedImage of the face to verify
     * @return FaceResult with success status and confidence score
     */
    public FaceResult verifyFace(String email, BufferedImage faceImage) {
        if (!isConfigured()) {
            return new FaceResult(false, "Face++ API not configured");
        }

        String enrolledToken = enrolledFaces.get(email.toLowerCase());
        if (enrolledToken == null) {
            return new FaceResult(false, "No face enrolled for this user. Please enroll your face first.");
        }

        try {
            String base64Image = imageToBase64(faceImage);
            
            String currentFaceToken = detectFace(base64Image);
            
            if (currentFaceToken == null) {
                return new FaceResult(false, "No face detected. Please position your face in the camera.");
            }

            double confidence = compareFaces(enrolledToken, currentFaceToken);

            if (confidence >= CONFIDENCE_THRESHOLD) {
                System.out.println("[FaceRecognition] Face verified for " + email + " (confidence: " + confidence + "%)");
                return new FaceResult(true, "Face verified!", confidence);
            } else {
                System.out.println("[FaceRecognition] Face mismatch for " + email + " (confidence: " + confidence + "%)");
                return new FaceResult(false, "Face does not match. Confidence: " + String.format("%.1f", confidence) + "%", confidence);
            }

        } catch (Exception e) {
            System.err.println("[FaceRecognition] Verification error: " + e.getMessage());
            return new FaceResult(false, "Error verifying face: " + e.getMessage());
        }
    }

    /**
     * Removes enrolled face for a user.
     */
    public void removeEnrollment(String email) {
        enrolledFaces.remove(email.toLowerCase());
        saveEnrolledFaces();
    }

    /**
     * Detects a face in the image and returns the face token.
     */
    private String detectFace(String base64Image) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);
        params.put("api_secret", apiSecret);
        params.put("image_base64", base64Image);
        params.put("return_attributes", "none");

        String response = postFormData(FACEPP_DETECT_URL, params);
        JSONObject json = new JSONObject(response);

        if (json.has("error_message")) {
            throw new IOException("API Error: " + json.getString("error_message"));
        }

        JSONArray faces = json.getJSONArray("faces");
        if (faces.length() == 0) {
            return null;
        }

        return faces.getJSONObject(0).getString("face_token");
    }

    /**
     * Compares two face tokens and returns confidence percentage.
     */
    private double compareFaces(String faceToken1, String faceToken2) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);
        params.put("api_secret", apiSecret);
        params.put("face_token1", faceToken1);
        params.put("face_token2", faceToken2);

        String response = postFormData(FACEPP_COMPARE_URL, params);
        JSONObject json = new JSONObject(response);

        if (json.has("error_message")) {
            throw new IOException("API Error: " + json.getString("error_message"));
        }

        return json.getDouble("confidence");
    }

    /**
     * Converts BufferedImage to Base64 string.
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Posts form data to URL.
     */
    private String postFormData(String urlString, Map<String, String> params) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (postData.length() > 0) postData.append('&');
            postData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            postData.append('=');
            postData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Result class for face operations.
     */
    public static class FaceResult {
        public final boolean success;
        public final String message;
        public final double confidence;

        public FaceResult(boolean success, String message) {
            this(success, message, 0);
        }

        public FaceResult(boolean success, String message, double confidence) {
            this.success = success;
            this.message = message;
            this.confidence = confidence;
        }
    }

}
