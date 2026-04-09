<?php

namespace App\Repository;

use App\Entity\Ville;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Ville>
 */
class VilleRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Ville::class);
    }

    public function searchAndSort(?string $search, string $sort = 'nom', string $direction = 'asc'): array
    {
        $qb = $this->createQueryBuilder('v')
            ->leftJoin('v.pays_id', 'p')
            ->addSelect('p');

        if (!empty($search)) {
            $qb->andWhere('v.nom LIKE :search OR v.typeTourisme LIKE :search OR v.saison LIKE :search OR p.nom LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        $sortMap = [
            'nom' => 'v.nom',
            'visitCount' => 'v.visit_count',
            'typeTourisme' => 'v.typeTourisme',
        ];

        $qb->orderBy($sortMap[$sort] ?? 'v.nom', strtoupper($direction));

        return $qb->getQuery()->getResult();
    }
}
