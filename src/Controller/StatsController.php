<?php

namespace App\Controller;

use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;

class StatsController extends AbstractController
{
    public function index(EntityManagerInterface $em): Response
    {
        $conn = $em->getConnection();

        // --- KPIs globaux ---
        $kpis = $conn->fetchAssociative("
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN statut = 'Confirmée' THEN 1 ELSE 0 END) as confirmed,
                SUM(CASE WHEN statut = 'Annulée'   THEN 1 ELSE 0 END) as cancelled,
                SUM(CASE WHEN statut = 'réservée'  THEN 1 ELSE 0 END) as pending,
                SUM(CASE WHEN statut = 'Confirmée' THEN coutTotal ELSE 0 END) as revenue,
                AVG(coutTotal) as avg_cost,
                AVG(nb_tickets) as avg_tickets
            FROM reservation
        ");

        // --- Historique mensuel complet ---
        $byMonth = $conn->fetchAllAssociative("
            SELECT
                MONTH(dateReservation) as month,
                YEAR(dateReservation)  as year,
                COUNT(*)               as reservations,
                SUM(coutTotal)         as revenue
            FROM reservation
            GROUP BY YEAR(dateReservation), MONTH(dateReservation)
            ORDER BY year ASC, month ASC
        ");

        // --- 3 derniers mois pour le calcul de prédiction ---
        $last3 = $conn->fetchAllAssociative("
            SELECT
                MONTH(dateReservation) as month,
                COUNT(*)               as reservations,
                SUM(coutTotal)         as revenue
            FROM reservation
            WHERE dateReservation >= DATE_SUB(NOW(), INTERVAL 3 MONTH)
            GROUP BY MONTH(dateReservation)
            ORDER BY month ASC
        ");

        // --- Calcul tendance et prédictions ---
        $avgPredRes = 0;
        $avgPredRev = 0;
        $trend      = 0;

        if (count($last3) > 0) {
            $avgPredRes = round(
                array_sum(array_column($last3, 'reservations')) / count($last3)
            );
            $avgPredRev = round(
                array_sum(array_column($last3, 'revenue')) / count($last3)
            );
        }

        if (count($last3) >= 2) {
            $last   = (int) $last3[count($last3) - 1]['reservations'];
            $before = (int) $last3[count($last3) - 2]['reservations'];
            if ($before > 0) {
                $trend = round((($last - $before) / $before) * 100, 1);
            }
        }

        $predNextMonth = round($avgPredRes * (1 + ($trend / 100)));
        $predNextRev   = round($avgPredRev  * (1 + ($trend / 100)));

        // --- Top 5 destinations par nombre de réservations ---
        $topDest = $conn->fetchAllAssociative("
            SELECT
                v.nom,
                COUNT(r.id)      as total,
                SUM(r.coutTotal) as revenue,
                AVG(r.coutTotal) as avg_cost,
                SUM(CASE WHEN r.statut = 'Annulée' THEN 1 ELSE 0 END) as cancelled
            FROM reservation r
            JOIN ville v ON r.destination_id = v.id
            GROUP BY v.nom
            ORDER BY total DESC
            LIMIT 5
        ");

        // --- Taux d'annulation par destination ---
        $cancelByDest = $conn->fetchAllAssociative("
            SELECT
                v.nom,
                COUNT(r.id) as total,
                SUM(CASE WHEN r.statut = 'Annulée' THEN 1 ELSE 0 END) as cancelled,
                ROUND(
                    SUM(CASE WHEN r.statut = 'Annulée' THEN 1 ELSE 0 END)
                    / COUNT(r.id) * 100
                , 1) as cancel_rate
            FROM reservation r
            JOIN ville v ON r.destination_id = v.id
            GROUP BY v.nom
            ORDER BY cancel_rate DESC
            LIMIT 5
        ");

        // --- Réservations par jour de semaine ---
        $byWeekday = $conn->fetchAllAssociative("
            SELECT
                DAYOFWEEK(dateReservation) as day,
                COUNT(*) as total
            FROM reservation
            GROUP BY DAYOFWEEK(dateReservation)
            ORDER BY total DESC
        ");

        // --- Top 8 clients ---
        $topClients = $conn->fetchAllAssociative("
            SELECT
                p.nom,
                COUNT(r.id)           as total,
                SUM(r.coutTotal)      as spent,
                MAX(r.dateReservation) as last_date
            FROM reservation r
            JOIN personne p ON r.personne_id = p.id
            GROUP BY p.nom
            ORDER BY total DESC
            LIMIT 8
        ");

        // --- Clients fidèles (3 réservations ou plus) ---
        $loyalClients = (int) $conn->fetchOne("
            SELECT COUNT(*) FROM (
                SELECT personne_id
                FROM reservation
                GROUP BY personne_id
                HAVING COUNT(*) >= 3
            ) t
        ");

        // --- Délai moyen entre réservation et départ ---
        $avgLeadTime = round(
            (float) $conn->fetchOne("
                SELECT AVG(DATEDIFF(dateDebut, dateReservation))
                FROM reservation
            "),
            1
        );

        // --- Meilleur mois historique ---
        $bestMonth = $conn->fetchAssociative("
            SELECT
                MONTH(dateReservation) as month,
                YEAR(dateReservation)  as year,
                COUNT(*) as total
            FROM reservation
            GROUP BY YEAR(dateReservation), MONTH(dateReservation)
            ORDER BY total DESC
            LIMIT 1
        ");

        // --- Taux de conversion ---
        $conversionRate = 0;
        if ((int) $kpis['total'] > 0) {
            $conversionRate = round(
                ((int) $kpis['confirmed'] / (int) $kpis['total']) * 100,
                1
            );
        }

        return $this->render('admin/stats/index.html.twig', [
            'kpis'           => $kpis,
            'byMonth'        => $byMonth,
            'last3'          => $last3,
            'predNextMonth'  => $predNextMonth,
            'predNextRev'    => $predNextRev,
            'trend'          => $trend,
            'topDest'        => $topDest,
            'cancelByDest'   => $cancelByDest,
            'byWeekday'      => $byWeekday,
            'topClients'     => $topClients,
            'loyalClients'   => $loyalClients,
            'avgLeadTime'    => $avgLeadTime,
            'bestMonth'      => $bestMonth,
            'conversionRate' => $conversionRate,
        ]);
    }
}