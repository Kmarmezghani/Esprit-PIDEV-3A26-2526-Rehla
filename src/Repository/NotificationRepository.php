<?php

namespace App\Repository;

use App\Entity\Notification;
use App\Entity\Personne;
use App\Entity\Activite;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class NotificationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Notification::class);
    }

    public function findActivityNotificationsByUser(Personne $personne, int $limit = 5): array
    {
        return $this->createQueryBuilder('n')
            ->andWhere('n.receiver_id = :receiver')
            ->andWhere('n.activite_id IS NOT NULL')
            ->setParameter('receiver', $personne)
            ->orderBy('n.created_at', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    public function hasUnreadActivityNotifications(Personne $personne): bool
    {
        $result = $this->createQueryBuilder('n')
            ->select('COUNT(n.id)')
            ->andWhere('n.receiver_id = :receiver')
            ->andWhere('n.activite_id IS NOT NULL')
            ->andWhere('n.is_read = :isRead')
            ->setParameter('receiver', $personne)
            ->setParameter('isRead', false)
            ->getQuery()
            ->getSingleScalarResult();

        return (int) $result > 0;
    }
    public function findLatestUnreadWaitlistHoldByReceiverAndActivite(Personne $personne, Activite $activite): ?Notification
{
    return $this->createQueryBuilder('n')
        ->andWhere('n.receiver_id = :receiver')
        ->andWhere('n.activite_id = :activite')
        ->andWhere('n.type = :type')
        ->andWhere('n.is_read = :isRead')
        ->setParameter('receiver', $personne)
        ->setParameter('activite', $activite)
        ->setParameter('type', 'WAITLIST_HOLD')
        ->setParameter('isRead', false)
        ->orderBy('n.created_at', 'DESC')
        ->setMaxResults(1)
        ->getQuery()
        ->getOneOrNullResult();
}
}