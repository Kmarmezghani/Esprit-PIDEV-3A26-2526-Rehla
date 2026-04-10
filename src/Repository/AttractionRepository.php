<?php

namespace App\Repository;

use App\Entity\Attraction;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Attraction>
 */
class AttractionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Attraction::class);
    }

    public function searchAndSort(?string $search, string $sort = 'nom', string $direction = 'asc', ?string $type = null, ?float $prixMin = null, ?float $prixMax = null, ?bool $estFerme = null): array
    {
        $qb = $this->createQueryBuilder('a')
            ->leftJoin('a.ville_id', 'v')
            ->addSelect('v');

        if (!empty($search)) {
            $qb->andWhere('a.nom LIKE :search OR a.description LIKE :search OR a.type LIKE :search OR v.nom LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        if (!empty($type)) {
            $qb->andWhere('a.type = :type')
               ->setParameter('type', $type);
        }

        if ($prixMin !== null) {
            $qb->andWhere('a.prix >= :prixMin')
               ->setParameter('prixMin', $prixMin);
        }

        if ($prixMax !== null) {
            $qb->andWhere('a.prix <= :prixMax')
               ->setParameter('prixMax', $prixMax);
        }

        if ($estFerme !== null) {
            $qb->andWhere('a.est_ferme = :estFerme')
               ->setParameter('estFerme', $estFerme);
        }

        $sortMap = [
            'nom' => 'a.nom',
            'prix' => 'a.prix',
            'heureOuverture' => 'a.heure_ouverture',
        ];

        $qb->orderBy($sortMap[$sort] ?? 'a.nom', strtoupper($direction));

        return $qb->getQuery()->getResult();
    }
}
