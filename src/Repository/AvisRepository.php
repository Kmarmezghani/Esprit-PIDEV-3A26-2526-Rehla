<?php

namespace App\Repository;

use App\Entity\Activite;
use App\Entity\Avis;
use App\Entity\Personne;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class AvisRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Avis::class);
    }
    public function findByActivite($activite)
{
    return $this->createQueryBuilder('a')
        ->andWhere('a.activite = :act')
        ->setParameter('act', $activite)
        ->orderBy('a.dateAvis', 'DESC')
        ->getQuery()
        ->getResult();
}
public function findOneByPersonneAndActivite(Personne $personne, Activite $activite): ?Avis
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.personne = :personne')
            ->andWhere('a.activite = :activite')
            ->setParameter('personne', $personne)
            ->setParameter('activite', $activite)
            ->getQuery()
            ->getOneOrNullResult();
    }
}