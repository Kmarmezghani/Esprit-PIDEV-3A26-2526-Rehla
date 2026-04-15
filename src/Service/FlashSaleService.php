<?php

namespace App\Service;

use App\Entity\Activite;
use Doctrine\ORM\EntityManagerInterface;

class FlashSaleService
{
    public function __construct(
        private EntityManagerInterface $em
    ) {
    }

    public function refreshFlashSales(): void
    {
        $now = new \DateTime();
        $threeDaysLater = (clone $now)->modify('+3 days');
        $twelveHoursLater = (clone $now)->modify('+12 hours');

        $activites = $this->em->getRepository(Activite::class)->findAll();

        foreach ($activites as $activite) {
            
            if (
                $activite->getIsFlashSale() &&
                $activite->getFlashExpiresAt() !== null &&
                $activite->getFlashExpiresAt() <= $now
            ) {
                $activite->setIsFlashSale(false);
                $activite->setFlashPrice(null);
                $activite->setFlashExpiresAt(null);
            }

            // 2) activate new flash sale if eligible
            if (
                !$activite->getIsFlashSale() &&
                $activite->getStatus() === 'DISPONIBLE' &&
                $activite->getDateDebut() !== null &&
                $activite->getDateDebut() >= $now &&
                $activite->getDateDebut() <= $threeDaysLater &&
                $activite->getMaxPlaces() !== null &&
                $activite->getMaxPlaces() > 0 &&
                $activite->getPrix() > 0
            ) {
                $booked = $this->countBookedTicketsForActivity($activite->getId());
                $note = (float) ($activite->getNoteMoyenne() ?? 0);

                if (
                    $booked < ($activite->getMaxPlaces() * 0.5) &&
                    $note < 3.5
                ) {
                    $flashPrice = round($activite->getPrix() * 0.70, 2);
                } else {
                    $flashPrice = round($activite->getPrix() * 0.80, 2);
                }

                $activite->setIsFlashSale(true);
                $activite->setFlashPrice($flashPrice);
                $activite->setFlashExpiresAt($twelveHoursLater);
            }
        }

        $this->em->flush();
    }

    public function revalidateFlashForActivity(Activite $activite): void
    {
        $now = new \DateTime();
        $threeDaysLater = (clone $now)->modify('+3 days');

        if (
            $activite->getIsFlashSale() &&
            (
                $activite->getFlashPrice() === null ||
                $activite->getFlashPrice() <= 0 ||
                $activite->getFlashExpiresAt() === null ||
                $activite->getFlashExpiresAt() <= $now
            )
        ) {
            $activite->setIsFlashSale(false);
            $activite->setFlashPrice(null);
            $activite->setFlashExpiresAt(null);
        }

        if (
            $activite->getIsFlashSale() &&
            (
                $activite->getStatus() !== 'DISPONIBLE' ||
                $activite->getDateDebut() === null ||
                $activite->getDateDebut() < $now ||
                $activite->getDateDebut() > $threeDaysLater ||
                $activite->getMaxPlaces() === null ||
                $activite->getNoteMoyenne() >= 3.5
            )
        ) {
            $activite->setIsFlashSale(false);
            $activite->setFlashPrice(null);
            $activite->setFlashExpiresAt(null);
        }

        $this->em->flush();
    }

    private function countBookedTicketsForActivity(int $activiteId): int
{
    return (int) $this->em->createQuery(
        'SELECT COUNT(t.id)
         FROM App\Entity\Ticket t
         JOIN t.reservation_id r
         WHERE t.activite = :activiteId
         AND r.statut = :statut'
    )
    ->setParameter('activiteId', $activiteId)
    ->setParameter('statut', 'réservée')
    ->getSingleScalarResult();
}
}