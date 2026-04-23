<?php

namespace App\Service;

use Dompdf\Dompdf;
use Dompdf\Options;
use Endroid\QrCode\Builder\BuilderInterface;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;

class BookingEmailService
{
    public function __construct(
        private MailerInterface $mailer,
        private BuilderInterface $defaultQrCodeBuilder
    ) {
    }

    private function generateBookingPassPdf(
        string $userName,
        string $activityName,
        float $price,
        int $reservationId,
        \DateTimeInterface $activityStart,
        \DateTimeInterface $activityEnd,
        string $statusLabel
    ): void {
        $pdfQrContent = sprintf(
            "PASS REHLA\nClient: %s\nActivité: %s\nPrix: %s TND\nStatut: %s",
            $userName,
            $activityName,
            number_format($price, 2, '.', ''),
            $statusLabel
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

        $passCode = 'RH' . str_pad((string) $reservationId, 5, '0', STR_PAD_LEFT);

        $activityStartDate = $activityStart->format('d/m/Y');
        $activityEndDate = $activityEnd->format('d/m/Y');
        $activityStartTime = $activityStart->format('H:i');
        $activityEndTime = $activityEnd->format('H:i');

        $logoPath = 'C:/xampp/htdocs/rehla/public/assets/images/logoblue.png';
        $logoBase64 = base64_encode(file_get_contents($logoPath));
        $logoSrc = 'data:image/png;base64,' . $logoBase64;

        $isCancelled = $statusLabel === 'Annulée';
        $statusBg = $isCancelled ? '#fee2e2' : '#dcfce7';
        $statusColor = $isCancelled ? '#b91c1c' : '#166534';
        $statusBorder = $isCancelled ? '#fca5a5' : '#86efac';

        $html = '
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: DejaVu Sans, Arial, sans-serif;
            background: #edf3fb;
            margin: 0;
            padding: 24px;
            color: #111827;
        }

        .page-title {
            text-align: center;
            font-size: 24px;
            font-weight: bold;
            color: #223f91;
            margin-bottom: 6px;
        }

        .page-subtitle {
            text-align: center;
            font-size: 12px;
            color: #6b7280;
            margin-bottom: 20px;
        }

        .ticket-wrap {
            position: relative;
            width: 100%;
        }

        .ticket {
            background: #ffffff;
            border-radius: 22px;
            border: 1px solid #d7e3f4;
            overflow: hidden;
        }

        .ticket-header {
            padding: 18px 22px 10px 22px;
        }

        .brand-table {
            width: 100%;
            border-collapse: collapse;
        }

        .brand-table td {
            vertical-align: top;
        }

        .logo {
            width: 46px;
            height: 46px;
        }

        .brand-name {
            font-size: 18px;
            font-weight: bold;
            color: #111827;
            line-height: 1.1;
            margin-top: 4px;
        }

        .top-code {
            text-align: right;
            font-size: 10px;
            color: #6b7280;
        }

        .top-code strong {
            display: block;
            font-size: 18px;
            color: #223f91;
            margin-top: 2px;
        }

        .qr-block {
            text-align: center;
            padding: 4px 20px 6px 20px;
        }

        .qr-block img {
            width: 210px;
            height: 210px;
        }

        .main-activity {
            padding: 0 28px 6px 28px;
        }

        .small-label {
            font-size: 10px;
            color: #6b7280;
            text-transform: uppercase;
        }

        .big-value {
            font-size: 28px;
            font-weight: bold;
            color: #111827;
            line-height: 1.1;
            margin-top: 2px;
        }

        .activity-date {
            font-size: 14px;
            color: #223f91;
            margin-top: 8px;
            font-weight: bold;
        }

        .activity-time {
            font-size: 13px;
            color: #374151;
            margin-top: 2px;
        }

        .separator {
            border-top: 2px dashed #cfd8e6;
            margin: 12px 0;
        }

        .bottom {
            padding: 6px 28px 20px 28px;
        }

        .bottom-table {
            width: 100%;
            border-collapse: collapse;
        }

        .bottom-table td {
            vertical-align: top;
        }

        .client-name {
            font-size: 16px;
            font-weight: bold;
            color: #111827;
            margin-top: 2px;
        }

        .price {
            font-size: 22px;
            font-weight: bold;
            color: #223f91;
            margin-top: 2px;
        }

        .status {
            display: inline-block;
            margin-top: 8px;
            background: ' . $statusBg . ';
            color: ' . $statusColor . ';
            border: 1px solid ' . $statusBorder . ';
            padding: 6px 12px;
            border-radius: 999px;
            font-size: 12px;
            font-weight: bold;
        }

        .status-box {
            text-align: right;
            padding-top: 18px;
        }

        .footer {
            text-align: center;
            font-size: 14px;
            font-weight: bold;
            color: #223f91;
            padding: 0 20px 18px 20px;
        }

        .cut-left, .cut-right {
            position: absolute;
            top: 68%;
            width: 22px;
            height: 22px;
            background: #edf3fb;
            border-radius: 50%;
        }

        .cut-left { left: -11px; }
        .cut-right { right: -11px; }
    </style>
</head>
<body>

    <div class="page-title">VOTRE PASS</div>
    <div class="page-subtitle">Veuillez le présenter sur votre téléphone lors de votre arrivée</div>

    <div class="ticket-wrap">
        <div class="cut-left"></div>
        <div class="cut-right"></div>

        <div class="ticket">
            <div class="ticket-header">
                <table class="brand-table">
                    <tr>
                        <td style="width:58px;">
                           <img src="' . $logoSrc . '" class="logo">
                        </td>
                        <td>
                            <div class="brand-name">Rehla</div>
                        </td>
                        <td style="width:120px;">
                            <div class="top-code">
                                CODE PASS
                                <strong>' . $passCode . '</strong>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>

            <div class="qr-block">
                <img src="' . $pdfQrSrc . '">
            </div>

            <div class="main-activity">
                <div class="small-label">Activité</div>
                <div class="big-value">' . htmlspecialchars($activityName) . '</div>
                <div class="activity-date">Début : ' . $activityStartDate . ' à ' . $activityStartTime . '</div>
                <div class="activity-time">Fin : ' . $activityEndDate . ' à ' . $activityEndTime . '</div>
            </div>

            <div class="separator"></div>

            <div class="bottom">
                <table class="bottom-table">
                    <tr>
                        <td style="width:70%;">
                            <div class="small-label">Nom du client</div>
                            <div class="client-name">' . htmlspecialchars($userName) . '</div>

                            <div style="margin-top:12px;" class="small-label">Prix</div>
                            <div class="price">' . number_format($price, 2, '.', '') . ' TND</div>
                        </td>

                        <td class="status-box">
                            <span class="status">' . $statusLabel . '</span>
                        </td>
                    </tr>
                </table>
            </div>

            <div class="footer">REHLA</div>
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

        if (file_exists($pdfQrPath)) {
            unlink($pdfQrPath);
        }
    }

    public function regenerateCancelledBookingPass(
        string $userName,
        string $activityName,
        float $price,
        int $reservationId,
        \DateTimeInterface $activityStart,
        \DateTimeInterface $activityEnd
    ): void {
        $this->generateBookingPassPdf(
            $userName,
            $activityName,
            $price,
            $reservationId,
            $activityStart,
            $activityEnd,
            'Annulée'
        );
    }

    public function sendBookingConfirmation(
        string $toEmail,
        string $userName,
        string $activityName,
        float $price,
        int $reservationId,
        int $userId,
        int $activityId,
        \DateTimeInterface $activityStart,
        \DateTimeInterface $activityEnd
    ): void {
        $safeName = !empty(trim($userName)) ? htmlspecialchars($userName, ENT_QUOTES, 'UTF-8') : 'there';
        $safeActivity = !empty(trim($activityName)) ? htmlspecialchars($activityName, ENT_QUOTES, 'UTF-8') : 'your activity';
        $safePrice = number_format($price, 2, '.', '');

        $qrContent = sprintf(
            'http://192.168.1.37/rehla/public/booking-pass/booking_pass_%d.pdf',
            $reservationId
        );

        $result = $this->defaultQrCodeBuilder->build(
            data: $qrContent,
            size: 250,
            margin: 10
        );

        $this->generateBookingPassPdf(
            $userName,
            $activityName,
            $price,
            $reservationId,
            $activityStart,
            $activityEnd,
            'Réservée'
        );

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