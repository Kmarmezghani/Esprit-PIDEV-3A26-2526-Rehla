<?php

namespace App\Service;

use Dompdf\Dompdf;
use Dompdf\Options;
use Twig\Environment;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;

class PdfGenerator
{
    private $twig;
    private $params;

    public function __construct(Environment $twig, ParameterBagInterface $params)
    {
        $this->twig = $twig;
        $this->params = $params; 
    }

    public function generateReservationPdf($reservation): string
    {
        $options = new Options();
        $options->set('defaultFont', 'DejaVu Sans');
        $options->setIsRemoteEnabled(true); 
        $options->setChroot($this->params->get('kernel.project_dir') . '/public');

        $dompdf = new Dompdf($options);


        $logoPath = $this->params->get('kernel.project_dir') . '/public/uploads/logo.png';

$html = $this->twig->render('pdf/reservation_receipt.html.twig', [
    'reservation' => $reservation,
    'logoPath' => $logoPath
]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return $dompdf->output();
    }
}