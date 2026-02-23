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
    public void sendCancellationConfirmation(String toEmail,
                                             String userName,
                                             String activityName,
                                             String startDate,
                                             String endDate,
                                             int ticketCount,
                                             double totalPrice)
            throws MessagingException, UnsupportedEncodingException {

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

        String subject = "Booking Cancelled - " + activityName;

        String html = """
        <!doctype html>
        <html>
        <body style="margin:0;padding:0;background:#f3f6ff;font-family:Arial,sans-serif;color:#111827;">
        <div style="max-width:640px;margin:0 auto;padding:24px;">

            <div style="background:#223f91;border-radius:16px;padding:18px;color:#fff;">
                <div style="font-size:18px;font-weight:800;">Rehla</div>
                <div style="opacity:0.9;margin-top:6px;font-size:14px;">Your Travel Companion</div>
            </div>

            <div style="background:#ffffff;border-radius:16px;padding:22px;margin-top:16px;
                        box-shadow:0 10px 24px rgba(17,24,39,0.08);border:1px solid #eef2ff;">

                <h2 style="margin-top:0;">Your booking has been cancelled ❌</h2>

                <p>Hello <strong>%s</strong>,</p>

                <p>Your reservation for the following activity has been successfully cancelled:</p>

                <div style="background:#f7fbff;border-radius:12px;padding:14px;margin-top:10px;">
                    <p><strong>Activity:</strong> %s</p>
                    <p><strong>Dates:</strong> %s → %s</p>
                    <p><strong>Tickets:</strong> %d</p>
                    <p><strong>Total paid:</strong> %.2f TND</p>
                </div>

                <p style="margin-top:16px;">
                    The spots are now available again.  
                    We hope to see you soon on another adventure 🌍
                </p>

                <p style="margin-top:20px;font-size:13px;color:#6b7280;">
                    Thank you,<br/>
                    <strong style="color:#223f91;">Rehla Team</strong>
                </p>
            </div>

            <div style="text-align:center;color:#9ca3af;font-size:12px;margin-top:14px;">
                © 2026 Rehla. All rights reserved.
            </div>

        </div>
        </body>
        </html>
        """.formatted(
                userName,
                activityName,
                startDate,
                endDate,
                ticketCount,
                totalPrice
        );

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(html, "text/html; charset=UTF-8");

        Transport.send(message);
    }
    public void sendWaitlistHoldEmail(String toEmail,
                                      String userName,
                                      String activityName,
                                      String destinationDisplay,
                                      String startDate,
                                      String endDate,
                                      String token,
                                      int minutes,
                                      String confirmUrl)
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
        String safeDest = (destinationDisplay == null || destinationDisplay.isBlank()) ? "Unknown" : escapeHtml(destinationDisplay);
        String safeStart = (startDate == null || startDate.isBlank()) ? "—" : escapeHtml(startDate);
        String safeEnd = (endDate == null || endDate.isBlank()) ? "—" : escapeHtml(endDate);
        String safeToken = (token == null) ? "" : escapeHtml(token);

        String subject = "Une place s'est libérée ✅ - " + safeActivity;

        String btnHref = (confirmUrl != null && !confirmUrl.isBlank()) ? confirmUrl : "#";
        String showBtn = (confirmUrl != null && !confirmUrl.isBlank()) ? "" : "display:none;";

        String html = """
        <!doctype html>
        <html>
          <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1.0">
            <title>Waiting list</title>
          </head>
          <body style="margin:0;padding:0;background:#f3f6ff;font-family:Arial,sans-serif;color:#111827;">
            <div style="max-width:640px;margin:0 auto;padding:24px;">

              <div style="background:#223f91;border-radius:16px;padding:18px 20px;color:#fff;">
                <div style="font-size:18px;font-weight:800;letter-spacing:0.4px;">Rehla</div>
                <div style="opacity:0.9;margin-top:6px;font-size:14px;">Your Travel Companion</div>
              </div>

              <div style="background:#ffffff;border-radius:16px;padding:22px;margin-top:16px;
                          box-shadow:0 10px 24px rgba(17,24,39,0.08);border:1px solid #eef2ff;">

                <h1 style="margin:0 0 10px;font-size:22px;color:#111827;">Une place s'est libérée 🎉</h1>

                <p style="margin:0 0 14px;font-size:14px;line-height:1.6;color:#374151;">
                  Bonjour <strong>%s</strong>,<br/>
                  Une place vient de se libérer pour une activité qui t’intéresse.
                  Tu as <strong>%d minutes</strong> pour confirmer.
                </p>

                <div style="background:#f7fbff;border:1px solid #e5edff;border-radius:14px;padding:14px;">
                  <div style="font-size:14px;margin:0 0 6px;color:#111827;"><strong>Activité:</strong> %s</div>
                  <div style="font-size:14px;margin:0 0 6px;color:#111827;"><strong>Destination:</strong> %s</div>
                  <div style="font-size:14px;margin:0;color:#111827;"><strong>Dates:</strong> %s → %s</div>
                </div>

                <div style="margin-top:16px;">
                  <a href="%s" style="%s display:inline-block;background:#3A5BC7;color:#fff;text-decoration:none;
                                     padding:10px 16px;border-radius:12px;font-weight:700;font-size:14px;">
                    Confirmer ma réservation
                  </a>
                </div>

                <div style="margin-top:14px;background:#fff7ed;border:1px solid #fed7aa;border-radius:12px;padding:12px;">
                  <div style="font-size:12px;color:#92400e;font-weight:800;margin-bottom:6px;">
                    Si tu n’as pas de bouton, utilise ce token dans l’app :
                  </div>
                  <div style="font-family:Consolas,monospace;font-size:13px;color:#111827;">
                    %s
                  </div>
                </div>

                <hr style="border:none;border-top:1px solid #eef2ff;margin:18px 0;" />

                <p style="margin:0;font-size:13px;line-height:1.6;color:#6b7280;">
                  Merci,<br/>
                  <strong style="color:#223f91;">Rehla</strong>
                </p>
              </div>

              <div style="text-align:center;color:#9ca3af;font-size:12px;margin-top:14px;">
                © 2026 Rehla. All rights reserved.
              </div>

            </div>
          </body>
        </html>
        """.formatted(
                safeName,
                minutes,
                safeActivity,
                safeDest,
                safeStart,
                safeEnd,
                btnHref,
                showBtn,
                safeToken
        );

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
