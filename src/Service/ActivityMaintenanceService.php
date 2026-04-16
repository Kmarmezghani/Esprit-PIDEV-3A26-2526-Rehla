<?php

namespace App\Service;

use App\Entity\Activite;
use Doctrine\ORM\EntityManagerInterface;

class ActivityMaintenanceService
{
    public function __construct(
        private EntityManagerInterface $em,
        private FlashSaleService $flashSaleService
    ) {
    }

    public function refreshStatusesAndFlashSales(): void
    {
        $this->markExpiredActivitiesAsUnavailable();
        $this->flashSaleService->refreshFlashSales();
    }

    public function markExpiredActivitiesAsUnavailable(): void
    {
        $now = new \DateTime();

       $this->em->createQuery(
    'UPDATE App\Entity\Activite a
     SET a.status = :status
     WHERE a.date_fin IS NOT NULL
     AND a.date_fin < :now
     AND a.status != :status'
)
        ->setParameter('status', 'INDISPONIBLE')
        ->setParameter('now', $now)
        ->execute();
    }

    public function revalidateOneActivity(Activite $activite): void
    {
        $now = new \DateTime();

       if (
    $activite->getDateFin() !== null &&
    $activite->getDateFin() < $now &&
    $activite->getStatus() !== 'INDISPONIBLE'
) {
    $activite->setStatus('INDISPONIBLE');
}

        $this->em->flush();

        $this->flashSaleService->revalidateFlashForActivity($activite);
    }
}