package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "rehla.noreply@gmail.com";
    private static final String APP_PASSWORD = "rvre dpgg ulwo dpfq";
    private static final String FROM_NAME = "Rehla";

    public void sendBookingConfirmation(String toEmail,
                                        String userName,
                                        String activityName,
                                        double price)
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

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Booking confirmed - " + activityName);

        String body = ""
                + "Hello " + (userName == null ? "" : userName) + ",\n\n"
                + "Your booking has been confirmed.\n"
                + "Activity: " + activityName + "\n"
                + "Price: " + String.format("%.2f", price) + " TND\n\n"
                + "Thank you,\n"
                + "Rehla";

        message.setText(body);

        Transport.send(message);
    }
}
