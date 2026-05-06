<?php

namespace App\Repository;

use App\Entity\Activite;
use App\Entity\Personne;
use App\Entity\Waitlist;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class WaitlistRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Waitlist::class);
    }

    public function findActiveEntryForUserAndActivity(Personne $personne, Activite $activite): ?Waitlist
    {
        return $this->createQueryBuilder('w')
            ->andWhere('w.personne = :personne')
            ->andWhere('w.activite = :activite')
            ->andWhere('w.status IN (:statuses)')
            ->setParameter('personne', $personne)
            ->setParameter('activite', $activite)
            ->setParameter('statuses', ['WAITING', 'HOLD'])
            ->orderBy('w.createdAt', 'ASC')
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function findNextWaitingForActivity(Activite $activite): ?Waitlist
{
    return $this->createQueryBuilder('w')
        ->andWhere('w.activite = :activite')
        ->andWhere('w.status = :status')
        ->setParameter('activite', $activite)
        ->setParameter('status', 'WAITING')
        ->orderBy('w.createdAt', 'ASC')
        ->setMaxResults(1)
        ->getQuery()
        ->getOneOrNullResult();
}

    public function findExpiredHolds(): array
    {
        return $this->createQueryBuilder('w')
            ->andWhere('w.status = :status')
            ->andWhere('w.holdExpiresAt IS NOT NULL')
            ->andWhere('w.holdExpiresAt < :now')
            ->setParameter('status', 'HOLD')
            ->setParameter('now', new \DateTime())
            ->getQuery()
            ->getResult();
    }

    public function countWaitingForActivity(Activite $activite): int
    {
        return (int) $this->createQueryBuilder('w')
            ->select('COUNT(w.id)')
            ->andWhere('w.activite = :activite')
            ->andWhere('w.status = :status')
            ->setParameter('activite', $activite)
            ->setParameter('status', 'WAITING')
            ->getQuery()
            ->getSingleScalarResult();
    }
    public function countActiveHoldsForActivity(Activite $activite): int
{
    return (int) $this->createQueryBuilder('w')
        ->select('COUNT(w.id)')
        ->andWhere('w.activite = :activite')
        ->andWhere('w.status = :status')
        ->andWhere('w.holdExpiresAt IS NOT NULL')
        ->andWhere('w.holdExpiresAt > :now')
        ->setParameter('activite', $activite)
        ->setParameter('status', 'HOLD')
        ->setParameter('now', new \DateTime())
        ->getQuery()
        ->getSingleScalarResult();
}
public function findActiveHoldsByPersonne(Personne $personne): array
{
    return $this->createQueryBuilder('w')
        ->andWhere('w.personne = :personne')
        ->andWhere('w.status = :status')
        ->andWhere('w.holdExpiresAt IS NOT NULL')
        ->andWhere('w.holdExpiresAt > :now')
        ->setParameter('personne', $personne)
        ->setParameter('status', 'HOLD')
        ->setParameter('now', new \DateTime())
        ->getQuery()
        ->getResult();
}
public function findActiveHoldForActivity(Activite $activite): ?Waitlist
{
    return $this->createQueryBuilder('w')
        ->andWhere('w.activite = :activite')
        ->andWhere('w.status = :status')
        ->andWhere('w.holdExpiresAt IS NOT NULL')
        ->andWhere('w.holdExpiresAt > :now')
        ->setParameter('activite', $activite)
        ->setParameter('status', 'HOLD')
        ->setParameter('now', new \DateTime())
        ->orderBy('w.createdAt', 'ASC')
        ->setMaxResults(1)
        ->getQuery()
        ->getOneOrNullResult();
}
public function countActiveHoldsForActivities(): array
{
    $rows = $this->createQueryBuilder('w')
        ->select('IDENTITY(w.activite) AS activiteId, COUNT(w.id) AS total')
        ->andWhere('w.status = :status')
        ->andWhere('w.holdExpiresAt IS NOT NULL')
        ->andWhere('w.holdExpiresAt > :now')
        ->setParameter('status', 'HOLD')
        ->setParameter('now', new \DateTime())
        ->groupBy('w.activite')
        ->getQuery()
        ->getArrayResult();

    $result = [];

    foreach ($rows as $row) {
        $result[(int) $row['activiteId']] = (int) $row['total'];
    }

    return $result;
}
}