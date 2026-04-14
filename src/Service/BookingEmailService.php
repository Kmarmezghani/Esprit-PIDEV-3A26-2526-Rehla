<?php

namespace App\Service;

use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;

class BookingEmailService
{
    public function __construct(
        private MailerInterface $mailer
    ) {
    }

    public function sendBookingConfirmation(
        string $toEmail,
        string $userName,
        string $activityName,
        float $price
    ): void {
        $safeName = !empty(trim($userName)) ? htmlspecialchars($userName) : 'there';
        $safeActivity = !empty(trim($activityName)) ? htmlspecialchars($activityName) : 'your activity';
        $safePrice = number_format($price, 2, '.', '');

        $subject = 'Booking confirmed - ' . $safeActivity;

        $html = <<<HTML
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
          Hello <strong>{$safeName}</strong>,<br/>
          Your booking has been successfully confirmed.
        </p>

        <div style="background:#f7fbff;border:1px solid #e5edff;border-radius:14px;padding:14px;">
          <div style="font-size:14px;margin:0 0 6px;color:#111827;">
            <strong>Activity:</strong> {$safeActivity}
          </div>
          <div style="font-size:14px;margin:0;color:#111827;">
            <strong>Price:</strong> {$safePrice} TND
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
HTML;

        $email = (new Email())
            ->from('rehla.noreply@gmail.com')
            ->to($toEmail)
            ->subject($subject)
            ->html($html);

        $this->mailer->send($email);
    }
}