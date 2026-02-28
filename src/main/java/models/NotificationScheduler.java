package models;

import models.Personne;
import models.notification;
import services.PersonneService;
import services.SmsService;
import services.NotificationService;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final NotificationService notificationService = new NotificationService();
    private final PersonneService personneService = new PersonneService();
    private final SmsService smsService = new SmsService(); // instance si nécessaire

    public void start() {

        Runnable task = () -> {

            LocalTime now = LocalTime.now().withSecond(0).withNano(0);

            List<Personne> users = personneService.findAllWithSmsActive();

            for (Personne user : users) {

                if (user.getHeureNotif() == null) continue;

                LocalTime userTime = user.getHeureNotif();

                if (now.getHour() == userTime.getHour()
                        && now.getMinute() == userTime.getMinute())
                {
                    System.out.println("⏰ Check notif for user " + user.getId());
                    System.out.println("Now = " + now);
                    System.out.println("UserTime = " + userTime);

                    sendDailySummary(user);
                }
            }
        };

        scheduler.scheduleAtFixedRate(
                task,
                0,
                1,
                TimeUnit.MINUTES
        );
    }

    private void sendDailySummary(Personne user) {

        List<notification> notifs = notificationService.getPendingSmsNotifications(user.getId());

        if (notifs.isEmpty()) return;

        // Construire le message récapitulatif
        String message = notificationService.buildDailySmsMessage(notifs);

        // Envoyer le SMS
        smsService.sendSms(user.getTelephone(), message);

        // Marquer les notifications comme envoyées
        notificationService.markAllAsSmsSent(notifs);
        System.out.println("Pending notifications = " + notifs.size());

    }
}