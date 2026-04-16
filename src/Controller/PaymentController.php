<?php

namespace App\Controller;

use App\Entity\Reservation;
use Doctrine\ORM\EntityManagerInterface;
use Stripe\Stripe;
use Stripe\Checkout\Session;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use App\Service\PdfGenerator;

class PaymentController extends AbstractController
{
    public function __construct(private string $stripeSecretKey)
    {
    }

    #[Route('/reservation/{id}/pay', name: 'reservation_pay')]
    public function pay(Reservation $reservation, Request $request): RedirectResponse
    {
        // Redirect to login if not authenticated
        $userId = $request->getSession()->get('user_id');
        if (!$userId) {
            return $this->redirectToRoute('home');
        }

        Stripe::setApiKey($this->stripeSecretKey);

// Call ExchangeRate API directly (no self-HTTP call)
$apiKey    = $_ENV['EXCHANGE_RATE_API_KEY'];
$ratesJson = file_get_contents("https://v6.exchangerate-api.com/v6/{$apiKey}/latest/TND");
$rates     = json_decode($ratesJson, true);
$usdRate   = $rates['conversion_rates']['USD'] ?? 0.32;

$amountTND   = $reservation->getCoutTotal();
$amountUSD   = $amountTND * $usdRate;
$amountCents = (int) round($amountUSD * 100);

        $session = Session::create([
            'mode'        => 'payment',
            'success_url' => $this->generateUrl('payment_success', ['id' => $reservation->getId()], UrlGeneratorInterface::ABSOLUTE_URL),
            'cancel_url'  => $this->generateUrl('payment_cancel',  ['id' => $reservation->getId()], UrlGeneratorInterface::ABSOLUTE_URL),
            'line_items'  => [[
                'quantity'   => 1,
                'price_data' => [
                    'currency'     => 'usd',
                    'unit_amount'  => $amountCents,
                    'product_data' => [
                        'name' => 'Réservation #' . $reservation->getId()
                             . ' — ' . ($reservation->getDestination()?->getNom() ?? ''),
                    ],
                ],
            ]],
            'metadata' => [
                'reservation_id' => $reservation->getId(),
                'amount_tnd'     => $amountTND,
            ],
        ]);

        return new RedirectResponse($session->url);
    }

    #[Route('/reservation/{id}/payment-success', name: 'payment_success')]
public function success(
    Reservation $reservation,
    EntityManagerInterface $em,
    PdfGenerator $pdfGenerator
): Response
{
    $reservation->setStatut('confirmée');
    $em->flush();

    // Generate PDF (but don't return it)
    $pdfContent = $pdfGenerator->generateReservationPdf($reservation);

    // Save it temporarily or ignore (simple version)
    $filePath = 'recu-reservation-' . $reservation->getId() . '.pdf';
    file_put_contents($this->getParameter('kernel.project_dir') . '/public/' . $filePath, $pdfContent);

    return $this->render('reservation/payment_success.html.twig', [
        'reservation' => $reservation,
        'pdfFile' => $filePath
    ]);
}

    #[Route('/reservation/{id}/payment-cancel', name: 'payment_cancel')]
    public function cancel(Reservation $reservation): Response
    {
        return $this->render('reservation/payment_cancel.html.twig', [
            'reservation' => $reservation,
        ]);
    }
}