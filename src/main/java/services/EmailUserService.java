package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Service for sending transactional emails via Brevo API.
 * Used for OTP codes and security alerts.
 */
public class EmailUserService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static EmailUserService instance;

    private String apiKey;
    private String senderEmail;
    private String senderName;

    private EmailUserService() {
        loadConfig();
    }

    public static synchronized EmailUserService getInstance() {
        if (instance == null) {
            instance = new EmailUserService();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                apiKey = props.getProperty("brevo.api.key", "");
                senderEmail = props.getProperty("brevo.sender.email", "noreply@rehla.tn");
                senderName = props.getProperty("brevo.sender.name", "Rehla Security");
            } else {
                System.err.println("[Email] config.properties not found, using defaults");
                apiKey = "";
                senderEmail = "noreply@rehla.tn";
                senderName = "Rehla Security";
            }
        } catch (IOException e) {
            System.err.println("[Email] Error loading config: " + e.getMessage());
            apiKey = "";
            senderEmail = "noreply@rehla.tn";
            senderName = "Rehla Security";
        }
    }

    /**
     * Sends an OTP email to the user.
     */
    public boolean sendOTPEmail(String toEmail, String userName, String otpCode) {
        String subject = "Your Rehla Security Code";
        String htmlContent = buildOTPEmailTemplate(userName, otpCode);
        return sendEmail(toEmail, userName, subject, htmlContent);
    }

    /**
     * Sends a security alert email (new location detected, etc.)
     */
    public boolean sendSecurityAlert(String toEmail, String userName, String alertType, String details) {
        String subject = "Security Alert - " + alertType;
        String htmlContent = buildSecurityAlertTemplate(userName, alertType, details);
        return sendEmail(toEmail, userName, subject, htmlContent);
    }

    /**
     * Core method to send email via Brevo API.
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_BREVO_API_KEY")) {
            System.out.println("[Email] API key not configured - simulating email send");
            System.out.println("[Email] To: " + toEmail);
            System.out.println("[Email] Subject: " + subject);
            System.out.println("[Email] Content preview: " + htmlContent.substring(0, Math.min(200, htmlContent.length())) + "...");
            return true;
        }

        try {
            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();

            JSONObject sender = new JSONObject();
            sender.put("name", senderName);
            sender.put("email", senderEmail);
            payload.put("sender", sender);

            JSONArray toArray = new JSONArray();
            JSONObject recipient = new JSONObject();
            recipient.put("email", toEmail);
            recipient.put("name", toName != null ? toName : toEmail);
            toArray.put(recipient);
            payload.put("to", toArray);

            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("[Email] Email sent successfully to " + toEmail);
                return true;
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    System.err.println("[Email] Failed to send email. Response: " + response);
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("[Email] Error sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildOTPEmailTemplate(String userName, String otpCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6fb; margin: 0; padding: 20px; }
                    .container { max-width: 500px; margin: 0 auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #3A5BC7, #223f91); padding: 30px; text-align: center; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { padding: 40px 30px; text-align: center; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #223f91; letter-spacing: 8px; background: #f4f6fb; padding: 20px 30px; border-radius: 10px; margin: 30px 0; display: inline-block; }
                    .message { color: #666; font-size: 16px; line-height: 1.6; }
                    .warning { color: #888; font-size: 14px; margin-top: 30px; }
                    .footer { background: #f4f6fb; padding: 20px; text-align: center; color: #888; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Rehla Security</h1>
                    </div>
                    <div class="content">
                        <p class="message">Hello <strong>%s</strong>,</p>
                        <p class="message">Your verification code is:</p>
                        <div class="otp-code">%s</div>
                        <p class="message">This code will expire in <strong>5 minutes</strong>.</p>
                        <p class="warning">If you didn't request this code, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Rehla. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName != null ? userName : "Traveler", otpCode);
    }

    private String buildSecurityAlertTemplate(String userName, String alertType, String details) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6fb; margin: 0; padding: 20px; }
                    .container { max-width: 500px; margin: 0 auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #e74c3c, #c0392b); padding: 30px; text-align: center; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { padding: 40px 30px; }
                    .alert-box { background: #fff3cd; border: 1px solid #ffc107; border-radius: 10px; padding: 20px; margin: 20px 0; }
                    .alert-title { color: #856404; font-weight: bold; font-size: 18px; margin-bottom: 10px; }
                    .alert-details { color: #666; font-size: 14px; line-height: 1.6; }
                    .message { color: #666; font-size: 16px; line-height: 1.6; }
                    .footer { background: #f4f6fb; padding: 20px; text-align: center; color: #888; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Security Alert</h1>
                    </div>
                    <div class="content">
                        <p class="message">Hello <strong>%s</strong>,</p>
                        <div class="alert-box">
                            <div class="alert-title">%s</div>
                            <div class="alert-details">%s</div>
                        </div>
                        <p class="message">If this was you, no action is needed. If you don't recognize this activity, please secure your account immediately.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 Rehla. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName != null ? userName : "Traveler", alertType, details);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
