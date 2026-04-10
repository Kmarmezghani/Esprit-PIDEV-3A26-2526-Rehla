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
}
