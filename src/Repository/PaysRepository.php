<?php

namespace App\Repository;

use App\Entity\Pays;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Pays>
 */
class PaysRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Pays::class);
    }

    public function searchAndSort(?string $search, string $sort = 'nom', string $direction = 'asc', ?int $visitMin = null, ?int $visitMax = null): array
    {
        $qb = $this->createQueryBuilder('p');

        if (!empty($search)) {
            $qb->andWhere('p.nom LIKE :search OR p.description LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        if ($visitMin !== null) {
            $qb->andWhere('p.visit_count >= :visitMin')
               ->setParameter('visitMin', $visitMin);
        }

        if ($visitMax !== null) {
            $qb->andWhere('p.visit_count <= :visitMax')
               ->setParameter('visitMax', $visitMax);
        }

        $sortMap = [
            'nom' => 'p.nom',
            'visitCount' => 'p.visit_count',
        ];

        $qb->orderBy($sortMap[$sort] ?? 'p.nom', strtoupper($direction));

        return $qb->getQuery()->getResult();
    }

    /**
     * Returns the top N countries ranked by a composite score:
     *   - Reservations count  : 40%
     *   - Visit count         : 30%
     *   - Attractions count   : 20%
     *   - Cities count        : 10%
     * Each metric is normalised to 0-100 before weighting.
     *
     * @return array<array{pays: Pays, score: float, reservations: int, attractions: int}>
     */
    public function findTopDestinations(int $limit = 3): array
    {
        // 1. Load all pays with their villes, attractions and reservations
        $allPays = $this->createQueryBuilder('p')
            ->leftJoin('p.villes', 'v')
            ->addSelect('v')
            ->getQuery()
            ->getResult();

        if (empty($allPays)) {
            return [];
        }

        // 2. Count reservations per pays via a single DQL query
        $reservationCounts = $this->getEntityManager()
            ->createQuery(
                'SELECT IDENTITY(r.destination) AS ville_id, COUNT(r.id) AS total
                 FROM App\Entity\Reservation r
                 GROUP BY r.destination'
            )
            ->getResult();

        // Map ville_id => reservation count
        $reservationsByVille = [];
        foreach ($reservationCounts as $row) {
            $reservationsByVille[(int)$row['ville_id']] = (int)$row['total'];
        }

        // 3. Compute raw metrics for every pays
        $rows = [];
        foreach ($allPays as $pays) {
            $cityCount       = $pays->getVilles()->count();
            $attractionCount = 0;
            $reservationCount = 0;

            foreach ($pays->getVilles() as $ville) {
                $attractionCount  += $ville->getAttractions()->count();
                $reservationCount += $reservationsByVille[$ville->getId()] ?? 0;
            }

            $rows[] = [
                'pays'         => $pays,
                'reservations' => $reservationCount,
                'visits'       => $pays->getVisitCount() ?? 0,
                'attractions'  => $attractionCount,
                'cities'       => $cityCount,
            ];
        }

        // 4. Normalise each metric to 0-100
        $maxReservations = max(array_column($rows, 'reservations')) ?: 1;
        $maxVisits       = max(array_column($rows, 'visits'))       ?: 1;
        $maxAttractions  = max(array_column($rows, 'attractions'))  ?: 1;
        $maxCities       = max(array_column($rows, 'cities'))       ?: 1;

        foreach ($rows as &$row) {
            $normR = ($row['reservations'] / $maxReservations) * 100;
            $normV = ($row['visits']       / $maxVisits)       * 100;
            $normA = ($row['attractions']  / $maxAttractions)  * 100;
            $normC = ($row['cities']       / $maxCities)       * 100;

            // Weighted composite score
            $row['score'] = round(
                $normR * 0.40 +
                $normV * 0.30 +
                $normA * 0.20 +
                $normC * 0.10,
                1
            );
        }
        unset($row);

        // 5. Sort descending by score and return top N
        usort($rows, fn($a, $b) => $b['score'] <=> $a['score']);

        return array_slice($rows, 0, $limit);
    }
}

