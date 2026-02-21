package services;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String FROM_EMAIL = dotenv.get("EMAIL_USERNAME");
    private static final String APP_PASSWORD = dotenv.get("EMAIL_APP_PASSWORD");
    private static final String FROM_NAME = "Rehla";

    public void sendBookingConfirmation(String toEmail,
                                        String userName,
                                        String activityName,
                                        double price)
            throws MessagingException, UnsupportedEncodingException {

        if (FROM_EMAIL == null || APP_PASSWORD == null) {
            throw new IllegalStateException("EMAIL_USERNAME / EMAIL_APP_PASSWORD missing in .env");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        String safeName = (userName == null || userName.isBlank()) ? "there" : escapeHtml(userName);
        String safeActivity = (activityName == null || activityName.isBlank()) ? "your activity" : escapeHtml(activityName);
        String safePrice = String.format("%.2f", price);

        String subject = "Booking confirmed - " + safeActivity;

        String html = """
            <!doctype html>
            <html>
              <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1.0">
                <title>Booking confirmed</title>
              </head>
              <body style="margin:0;padding:0;background:#f3f6ff;font-family:Arial,sans-serif;color:#111827;">
                <div style="max-width:640px;margin:0 auto;padding:24px;">
                  
                  <div style="background:#223f91;border-radius:16px;padding:18px 20px;color:#fff;">
                    <div style="font-size:18px;font-weight:800;letter-spacing:0.4px;">Rehla</div>
                    <div style="opacity:0.9;margin-top:6px;font-size:14px;">Your Travel Companion</div>
                  </div>

                  <div style="background:#ffffff;border-radius:16px;padding:22px;margin-top:16px;
                              box-shadow:0 10px 24px rgba(17,24,39,0.08);border:1px solid #eef2ff;">
                    
                    <h1 style="margin:0 0 10px;font-size:22px;color:#111827;">Booking confirmed ✅</h1>
                    <p style="margin:0 0 14px;font-size:14px;line-height:1.6;color:#374151;">
                      Hello <strong>%s</strong>,<br/>
                      Your booking has been successfully confirmed.
                    </p>

                    <div style="background:#f7fbff;border:1px solid #e5edff;border-radius:14px;padding:14px;">
                      <div style="font-size:14px;margin:0 0 6px;color:#111827;">
                        <strong>Activity:</strong> %s
                      </div>
                      <div style="font-size:14px;margin:0;color:#111827;">
                        <strong>Price:</strong> %s TND
                      </div>
                    </div>

                    <div style="margin-top:16px;">
                      <a href="#" style="display:inline-block;background:#3A5BC7;color:#fff;text-decoration:none;
                                         padding:10px 16px;border-radius:12px;font-weight:700;font-size:14px;">
                        View my booking
                      </a>
                    </div>

                    <hr style="border:none;border-top:1px solid #eef2ff;margin:18px 0;" />

                    <p style="margin:0;font-size:13px;line-height:1.6;color:#6b7280;">
                      Thank you,<br/>
                      <strong style="color:#223f91;">Rehla</strong>
                    </p>
                  </div>

                  <div style="text-align:center;color:#9ca3af;font-size:12px;margin-top:14px;">
                    © 2026 Rehla. All rights reserved.
                  </div>

                </div>
              </body>
            </html>
            """.formatted(safeName, safeActivity, safePrice);

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        message.setContent(html, "text/html; charset=UTF-8");

        Transport.send(message);
    }


    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
