<?php

namespace App\Service;

use Endroid\QrCode\Builder\BuilderInterface;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Dompdf\Dompdf;
use Dompdf\Options;

class BookingEmailService
{
    public function __construct(
        private MailerInterface $mailer,
        private BuilderInterface $defaultQrCodeBuilder
    ) {
    }

    public function sendBookingConfirmation(
        string $toEmail,
        string $userName,
        string $activityName,
        float $price,
        int $reservationId,
        int $userId,
        int $activityId
    ): void {
        $safeName = !empty(trim($userName)) ? htmlspecialchars($userName, ENT_QUOTES, 'UTF-8') : 'there';
        $safeActivity = !empty(trim($activityName)) ? htmlspecialchars($activityName, ENT_QUOTES, 'UTF-8') : 'your activity';
        $safePrice = number_format($price, 2, '.', '');

        $qrContent = sprintf(
    'http://192.168.100.49/rehla/public/booking-pass/booking_pass_%d.pdf',
    $reservationId
);

        $result = $this->defaultQrCodeBuilder->build(
            data: $qrContent,
            size: 250,
            margin: 10
        );
        $pdfQrContent = sprintf(
    "PASS REHLA\nClient: %s\nActivité: %s\nPrix: %s TND\nStatut: Réservée",
    $userName,
    $activityName,
    number_format($price, 2, '.', '')
);

$pdfQrResult = $this->defaultQrCodeBuilder->build(
    data: $pdfQrContent,
    size: 220,
    margin: 10
);

$pdfQrPath = sys_get_temp_dir() . '/qr_pdf_reservation_' . $reservationId . '.png';
$pdfQrResult->saveToFile($pdfQrPath);
$pdfQrBase64 = base64_encode(file_get_contents($pdfQrPath));
$pdfQrSrc = 'data:image/png;base64,' . $pdfQrBase64;
        
$options = new Options();
$options->set('defaultFont', 'Arial');

$dompdf = new Dompdf($options);

$html = '
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: DejaVu Sans, Arial, sans-serif;
            background: #f4f7fb;
            margin: 0;
            padding: 30px;
            color: #1f2937;
        }

        .card {
            background: #ffffff;
            border-radius: 18px;
            overflow: hidden;
            border: 1px solid #dbe4f0;
        }

        .header {
            background: #223f91;
            color: white;
            padding: 24px 28px;
        }

        .brand {
            font-size: 30px;
            font-weight: bold;
            margin-bottom: 6px;
        }

        .subtitle {
            font-size: 14px;
            opacity: 0.9;
        }

        .content {
            padding: 28px;
        }

        .title {
            font-size: 24px;
            font-weight: bold;
            margin-bottom: 22px;
            color: #223f91;
        }

        .info-box {
            background: #f8faff;
            border: 1px solid #e4ecf7;
            border-radius: 12px;
            padding: 14px 16px;
            margin-bottom: 14px;
        }

        .label {
            font-size: 12px;
            color: #6b7280;
            margin-bottom: 4px;
        }

        .value {
            font-size: 18px;
            font-weight: bold;
            color: #111827;
        }

        .status {
            display: inline-block;
            margin-top: 12px;
            background: #dcfce7;
            color: #166534;
            border: 1px solid #86efac;
            padding: 8px 14px;
            border-radius: 999px;
            font-size: 14px;
            font-weight: bold;
        }

        .footer {
            margin-top: 24px;
            font-size: 12px;
            color: #6b7280;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">
            <div class="brand">Rehla</div>
            <div class="subtitle">Pass de réservation</div>
        </div>

        <div class="content">
            <div class="title">Réservation confirmée</div>

            <div class="info-box">
                <div class="label">Client</div>
                <div class="value">' . htmlspecialchars($userName) . '</div>
            </div>

            <div class="info-box">
                <div class="label">Activité</div>
                <div class="value">' . htmlspecialchars($activityName) . '</div>
            </div>

            <div class="info-box">
                <div class="label">Prix</div>
                <div class="value">' . number_format($price, 2) . ' TND</div>
            </div>

           <span class="status">Réservée</span>

<div style="text-align:center; margin-top: 22px;">
    <img src="' . $pdfQrSrc . '" width="120">
</div>

<div class="footer">
    Veuillez présenter ce pass lors de votre activité.
</div>
        </div>
    </div>
</body>
</html>
';
$dompdf->loadHtml($html);
$dompdf->setPaper('A4', 'portrait');
$dompdf->render();


$pdfPath = 'C:/xampp/htdocs/rehla/public/booking-pass/booking_pass_' . $reservationId . '.pdf';
file_put_contents($pdfPath, $dompdf->output());

        $tempQrPath = sys_get_temp_dir() . '/qr_reservation_' . $reservationId . '.png';
        $result->saveToFile($tempQrPath);

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
          <div style="font-size:14px;margin:0 0 6px;color:#111827;">
            <strong>Price:</strong> {$safePrice} TND
          </div>
          <div style="font-size:14px;margin:0;color:#111827;">
            <strong>Reservation ID:</strong> {$reservationId}
          </div>
        </div>

        <p style="margin-top:16px;font-size:14px;color:#374151;">
          Your QR ticket is attached to this email as a PNG file.
        </p>

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
            ->html($html)
            ->attachFromPath($tempQrPath, 'ticket-qr.png', 'image/png');

        try {
            $this->mailer->send($email);
        } finally {
    if (file_exists($tempQrPath)) {
        unlink($tempQrPath);
    }

    if (isset($pdfQrPath) && file_exists($pdfQrPath)) {
        unlink($pdfQrPath);
    }
}
    }

    public function sendBookingCancellation(
        string $toEmail,
        string $userName,
        string $activityName,
        float $price
    ): void {
        $safeName = !empty(trim($userName)) ? htmlspecialchars($userName, ENT_QUOTES, 'UTF-8') : 'there';
        $safeActivity = !empty(trim($activityName)) ? htmlspecialchars($activityName, ENT_QUOTES, 'UTF-8') : 'your activity';
        $safePrice = number_format($price, 2, '.', '');

        $subject = 'Booking cancelled - ' . $safeActivity;

        $html = <<<HTML
<!doctype html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <title>Booking cancelled</title>
  </head>
  <body style="margin:0;padding:0;background:#fef2f2;font-family:Arial,sans-serif;color:#111827;">
    <div style="max-width:640px;margin:0 auto;padding:24px;">
      
      <div style="background:#991b1b;border-radius:16px;padding:18px 20px;color:#fff;">
        <div style="font-size:18px;font-weight:800;letter-spacing:0.4px;">Rehla</div>
        <div style="opacity:0.9;margin-top:6px;font-size:14px;">Booking update</div>
      </div>

      <div style="background:#ffffff;border-radius:16px;padding:22px;margin-top:16px;
                  box-shadow:0 10px 24px rgba(17,24,39,0.08);border:1px solid #fee2e2;">
        
        <h1 style="margin:0 0 10px;font-size:22px;color:#991b1b;">Booking cancelled</h1>
        <p style="margin:0 0 14px;font-size:14px;line-height:1.6;color:#374151;">
          Hello <strong>{$safeName}</strong>,<br/>
          Your booking has been cancelled.
        </p>

        <div style="background:#fff7f7;border:1px solid #fecaca;border-radius:14px;padding:14px;">
          <div style="font-size:14px;margin:0 0 6px;color:#111827;">
            <strong>Activity:</strong> {$safeActivity}
          </div>
          <div style="font-size:14px;margin:0;color:#111827;">
            <strong>Price:</strong> {$safePrice} TND
          </div>
        </div>

        <p style="margin:16px 0 0;font-size:13px;line-height:1.6;color:#6b7280;">
          If this cancellation was not expected, please contact support.
        </p>
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