<?php

namespace App\UserPdfBundle\Service;

use App\Entity\Personne;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Component\HttpFoundation\Response;

class UserPdfService
{
    private const BRAND_COLOR = '#223f91';

    private const STATUT_COLORS = [
        'ACTIF'    => '#28a745',
        'INACTIF'  => '#6c757d',
        'SUSPENDU' => '#dc3545',
    ];

    private const ROLE_COLORS = [
        'ADMIN'  => '#223f91',
        'GUIDE'  => '#fd7e14',
        'CLIENT' => '#17a2b8',
    ];

    /** @param Personne[] $users */
    public function generateUsersListPdf(array $users): Response
    {
        $total     = count($users);
        $actifs    = count(array_filter($users, fn($u) => $u->getStatutCompte() === 'ACTIF'));
        $suspendus = count(array_filter($users, fn($u) => $u->getStatutCompte() === 'SUSPENDU'));
        $guides    = count(array_filter($users, fn($u) => $u->getRole() === 'GUIDE'));
        $clients   = count(array_filter($users, fn($u) => $u->getRole() === 'CLIENT'));

        $rows = '';
        foreach ($users as $u) {
            $statutColor = self::STATUT_COLORS[$u->getStatutCompte()] ?? '#6c757d';
            $roleColor   = self::ROLE_COLORS[$u->getRole()] ?? '#6c757d';
            $date        = $u->getDateInscription() ? $u->getDateInscription()->format('d/m/Y') : '—';

            $rows .= '<tr>
                <td>' . htmlspecialchars((string) $u->getId()) . '</td>
                <td><strong>' . htmlspecialchars($u->getNom() . ' ' . $u->getPrenom()) . '</strong></td>
                <td>' . htmlspecialchars($u->getEmail()) . '</td>
                <td style="color:' . $roleColor . '; font-weight:600;">' . $u->getRole() . '</td>
                <td><span style="color:' . $statutColor . '; font-weight:600;">' . $u->getStatutCompte() . '</span></td>
                <td>' . $date . '</td>
            </tr>';
        }

        $html = $this->buildHtml($rows, $total, $actifs, $suspendus, $guides, $clients);
        $pdf  = $this->renderPdf($html);

        $filename = 'utilisateurs_rehla_' . date('Ymd_His') . '.pdf';

        return new Response($pdf, 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"',
        ]);
    }

    private function buildHtml(string $rows, int $total, int $actifs,
                               int $suspendus, int $guides, int $clients): string
    {
        $color = self::BRAND_COLOR;

        return '<!DOCTYPE html><html><head><meta charset="UTF-8">
        <style>
            body { font-family: DejaVu Sans, sans-serif; font-size: 11px; color: #333; margin: 20px; }
            .header { background: ' . $color . '; color: #fff; padding: 20px 24px; border-radius: 8px; margin-bottom: 20px; }
            .header h1 { margin: 0; font-size: 20px; }
            .header p  { margin: 4px 0 0; font-size: 11px; opacity: .85; }
            .stats { display: table; width: 100%; margin-bottom: 20px; border-spacing: 8px; }
            .stat-box { display: table-cell; background: #f8f9fa; border: 1px solid #dee2e6;
                        border-radius: 6px; padding: 10px 14px; text-align: center; width: 20%; }
            .stat-num { font-size: 22px; font-weight: 700; color: ' . $color . '; display: block; }
            .stat-lbl { font-size: 10px; color: #6c757d; }
            table { width: 100%; border-collapse: collapse; }
            th { background: ' . $color . '; color: #fff; padding: 8px 10px; text-align: left; font-size: 11px; }
            td { padding: 7px 10px; border-bottom: 1px solid #f0f0f0; }
            tr:nth-child(even) td { background: #f8f9fa; }
            .footer { margin-top: 16px; font-size: 10px; color: #aaa; text-align: center; }
        </style></head><body>
        <div class="header">
            <h1>&#128101; Liste des Utilisateurs — Rehla</h1>
            <p>Exporté le ' . date('d/m/Y à H:i') . ' &nbsp;|&nbsp; Total : ' . $total . ' utilisateurs</p>
        </div>
        <div class="stats">
            <div class="stat-box"><span class="stat-num">' . $total . '</span><span class="stat-lbl">Total</span></div>
            <div class="stat-box"><span class="stat-num" style="color:#28a745;">' . $actifs . '</span><span class="stat-lbl">Actifs</span></div>
            <div class="stat-box"><span class="stat-num" style="color:#dc3545;">' . $suspendus . '</span><span class="stat-lbl">Suspendus</span></div>
            <div class="stat-box"><span class="stat-num" style="color:#fd7e14;">' . $guides . '</span><span class="stat-lbl">Guides</span></div>
            <div class="stat-box"><span class="stat-num" style="color:#17a2b8;">' . $clients . '</span><span class="stat-lbl">Clients</span></div>
        </div>
        <table>
            <thead><tr>
                <th>#</th><th>Nom complet</th><th>Email</th><th>Rôle</th><th>Statut</th><th>Inscription</th>
            </tr></thead>
            <tbody>' . $rows . '</tbody>
        </table>
        <div class="footer">Rehla Travel Agency &mdash; Document généré automatiquement</div>
        </body></html>';
    }

    private function renderPdf(string $html): string
    {
        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        return $dompdf->output();
    }
}
