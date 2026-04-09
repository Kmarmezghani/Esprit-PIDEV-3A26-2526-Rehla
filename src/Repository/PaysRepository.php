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

    public function searchAndSort(?string $search, string $sort = 'nom', string $direction = 'asc'): array
    {
        $qb = $this->createQueryBuilder('p');

        if (!empty($search)) {
            $qb->andWhere('p.nom LIKE :search OR p.description LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        $sortMap = [
            'nom' => 'p.nom',
            'visitCount' => 'p.visit_count',
        ];

        $qb->orderBy($sortMap[$sort] ?? 'p.nom', strtoupper($direction));

        return $qb->getQuery()->getResult();
    }
}
