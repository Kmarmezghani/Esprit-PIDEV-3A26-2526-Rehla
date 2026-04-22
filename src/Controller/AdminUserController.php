<?php

namespace App\Controller;

use App\Entity\Guide;
use App\Entity\Personne;
use App\Entity\Preference;
use App\Service\UserRiskAnalysisService;
use App\UserMailerBundle\Service\UserMailerService;
use Doctrine\ORM\EntityManagerInterface;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class AdminUserController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  Helper : vérifie que l'administrateur est connecté
    // ─────────────────────────────────────────────────────────────
    private function requireAdmin(Request $request): ?Response
    {
        $session = $request->getSession();
        if (!$session->get('user_id') || $session->get('user_role') !== 'ADMIN') {
            return $this->redirectToRoute('app_login');
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  LISTE DES UTILISATEURS
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs', name: 'admin_users')]
    public function list(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        $users = $em->getRepository(Personne::class)->findAll();

        return $this->render('admin/users_admin.html.twig', [
            'users' => $users,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MODIFIER UN UTILISATEUR (admin)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/modifier', name: 'admin_user_edit')]
    public function edit(int $id, Request $request, EntityManagerInterface $em, UserMailerService $mailer): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        /** @var Guide|null $guide */
        $guide = $em->getRepository(Guide::class)->findOneBy(['personne' => $personne]);

        if ($request->isMethod('POST')) {
            $role          = in_array($request->request->get('role'), ['CLIENT', 'GUIDE', 'ADMIN']) ? $request->request->get('role') : 'CLIENT';
            $statut        = $request->request->get('statutCompte', 'ACTIF');
            $suspensionFin = $request->request->get('suspension_fin', '');
            $specialite    = trim($request->request->get('specialite', ''));
            $langues       = trim($request->request->get('langues', ''));
            $experience    = trim($request->request->get('experience', ''));

            $oldStatut = $personne->getStatutCompte();
            $oldRole   = $personne->getRole();

            $personne->setRole($role);
            $personne->setStatutCompte($statut);

            // ── Métier B : suspension temporaire ──
            if ($statut === 'SUSPENDU' && $suspensionFin !== '') {
                $personne->setSuspensionFin(new \DateTime($suspensionFin));
            } else {
                $personne->setSuspensionFin(null);
            }

            // ── UserMailerBundle : email de suspension ──
            if ($statut === 'SUSPENDU' && $oldStatut !== 'SUSPENDU') {
                $mailer->sendAccountSuspendedEmail($personne, $personne->getSuspensionFin());
            }

            if ($role === 'GUIDE') {
                if (!$guide) {
                    $guide = new Guide();
                    $guide->setPersonne($personne);
                    $em->persist($guide);
                }
                $guide->setSpecialite($specialite ?: 'Non précisé');
                $guide->setLangues($langues ?: 'Non précisé');
                $guide->setExperience($experience ?: 'Non précisé');
            } elseif ($oldRole === 'GUIDE' && $guide) {
                $em->remove($guide);
            }

            $em->flush();

            $this->addFlash('admin_success', 'Rôle et statut mis à jour avec succès !');
            return $this->redirectToRoute('admin_users');
        }

        return $this->render('admin/user_edit.html.twig', [
            'personne' => $personne,
            'guide'    => $guide,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  SUPPRIMER UN UTILISATEUR
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/supprimer', name: 'admin_user_delete', methods: ['POST'])]
    public function delete(int $id, Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        // Empêcher l'admin de se supprimer lui-même
        if ($personne->getId() === $request->getSession()->get('user_id')) {
            $this->addFlash('danger', 'Vous ne pouvez pas supprimer votre propre compte administrateur.');
            return $this->redirectToRoute('admin_users');
        }

        $em->remove($personne);
        $em->flush();

        $this->addFlash('admin_success', 'Utilisateur supprimé avec succès.');
        return $this->redirectToRoute('admin_users');
    }

    // ─────────────────────────────────────────────────────────────
    //  IA — ANALYSE DES RISQUES UTILISATEURS (Gemini)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/analyse-risques', name: 'admin_risk_analysis')]
    public function riskAnalysis(Request $request, EntityManagerInterface $em, UserRiskAnalysisService $riskService): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        $users   = $em->getRepository(Personne::class)->findAll();
        $results = $riskService->analyzeUsers($users);
        $debug   = $riskService->getLastDebug();

        return $this->render('admin/risk_analysis.html.twig', [
            'users'   => $users,
            'results' => $results,
            'debug'   => $debug,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTIER 2 — EXPORT PDF DE LA LISTE DES UTILISATEURS
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/export-pdf', name: 'admin_users_pdf')]
    public function exportPdf(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        $users = $em->getRepository(Personne::class)->findBy([], ['dateInscription' => 'DESC']);

        // Statistiques rapides
        $total     = count($users);
        $actifs    = count(array_filter($users, fn($u) => $u->getStatutCompte() === 'ACTIF'));
        $suspendus = count(array_filter($users, fn($u) => $u->getStatutCompte() === 'SUSPENDU'));
        $guides    = count(array_filter($users, fn($u) => $u->getRole() === 'GUIDE'));
        $clients   = count(array_filter($users, fn($u) => $u->getRole() === 'CLIENT'));

        $statutColors = ['ACTIF' => '#28a745', 'INACTIF' => '#6c757d', 'SUSPENDU' => '#dc3545'];
        $roleColors   = ['ADMIN' => '#223f91', 'GUIDE' => '#fd7e14', 'CLIENT' => '#17a2b8'];

        $rows = '';
        foreach ($users as $u) {
            $statutColor = $statutColors[$u->getStatutCompte()] ?? '#6c757d';
            $roleColor   = $roleColors[$u->getRole()] ?? '#6c757d';
            $date        = $u->getDateInscription() ? $u->getDateInscription()->format('d/m/Y') : '—';
            $rows .= '<tr>
                <td>' . htmlspecialchars($u->getId()) . '</td>
                <td><strong>' . htmlspecialchars($u->getNom() . ' ' . $u->getPrenom()) . '</strong></td>
                <td>' . htmlspecialchars($u->getEmail()) . '</td>
                <td style="color:' . $roleColor . '; font-weight:600;">' . $u->getRole() . '</td>
                <td><span style="color:' . $statutColor . '; font-weight:600;">' . $u->getStatutCompte() . '</span></td>
                <td>' . $date . '</td>
            </tr>';
        }

        $html = '<!DOCTYPE html><html><head><meta charset="UTF-8">
        <style>
            body { font-family: DejaVu Sans, sans-serif; font-size: 11px; color: #333; margin: 20px; }
            .header { background: #223f91; color: #fff; padding: 20px 24px; border-radius: 8px; margin-bottom: 20px; }
            .header h1 { margin: 0; font-size: 20px; }
            .header p  { margin: 4px 0 0; font-size: 11px; opacity: .85; }
            .stats { display: table; width: 100%; margin-bottom: 20px; border-spacing: 8px; }
            .stat-box { display: table-cell; background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px;
                        padding: 10px 14px; text-align: center; width: 20%; }
            .stat-num  { font-size: 22px; font-weight: 700; color: #223f91; display: block; }
            .stat-lbl  { font-size: 10px; color: #6c757d; }
            table { width: 100%; border-collapse: collapse; }
            th { background: #223f91; color: #fff; padding: 8px 10px; text-align: left; font-size: 11px; }
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

        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'utilisateurs_rehla_' . date('Ymd_His') . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"',
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTIER 3 — STATISTIQUES UTILISATEURS (Chart.js)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/statistiques', name: 'admin_stats')]
    public function stats(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        $users = $em->getRepository(Personne::class)->findAll();

        // Distribution par rôle
        $byRole = ['CLIENT' => 0, 'GUIDE' => 0, 'ADMIN' => 0];
        foreach ($users as $u) {
            $r = $u->getRole();
            if (isset($byRole[$r])) $byRole[$r]++;
        }

        // Distribution par statut
        $byStatut = ['ACTIF' => 0, 'INACTIF' => 0, 'SUSPENDU' => 0];
        foreach ($users as $u) {
            $s = $u->getStatutCompte();
            if (isset($byStatut[$s])) $byStatut[$s]++;
        }

        // Inscriptions par mois (12 derniers mois)
        $inscriptionsByMonth = [];
        $now = new \DateTime();
        for ($i = 11; $i >= 0; $i--) {
            $month = (new \DateTime())->modify("-$i months");
            $key   = $month->format('Y-m');
            $label = $month->format('M Y');
            $inscriptionsByMonth[$key] = ['label' => $label, 'count' => 0];
        }
        foreach ($users as $u) {
            if ($u->getDateInscription()) {
                $key = $u->getDateInscription()->format('Y-m');
                if (isset($inscriptionsByMonth[$key])) {
                    $inscriptionsByMonth[$key]['count']++;
                }
            }
        }

        return $this->render('admin/stats.html.twig', [
            'total'               => count($users),
            'byRole'              => $byRole,
            'byStatut'            => $byStatut,
            'inscriptionsByMonth' => array_values($inscriptionsByMonth),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  VOIR LES PRÉFÉRENCES D'UN UTILISATEUR (lecture seule)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/preferences', name: 'admin_user_preferences')]
    public function viewPreferences(int $id, Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        /** @var Preference|null $preference */
        $preference = $em->getRepository(Preference::class)->findOneBy(['personne_id' => $personne]);

        return $this->render('admin/user_preferences_show.html.twig', [
            'personne'   => $personne,
            'preference' => $preference,
        ]);
    }
}
