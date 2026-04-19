<?php

namespace App\Service;

use App\Entity\Activite;
use App\Entity\Notification;
use App\Entity\Personne;
use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Entity\Waitlist;
use App\Repository\NotificationRepository;
use App\Repository\WaitlistRepository;
use Doctrine\ORM\EntityManagerInterface;

class WaitlistService
{
    public function __construct(
        private EntityManagerInterface $em,
        private WaitlistRepository $waitlistRepository,
        private NotificationRepository $notificationRepository,
        private BookingEmailService $bookingEmailService
    ) {
    }

    public function joinWaitlist(Personne $personne, Activite $activite): bool
{
    $existing = $this->waitlistRepository->findOneBy([
        'personne' => $personne,
        'activite' => $activite
    ]);

    if ($existing) {
        if (in_array($existing->getStatus(), ['WAITING', 'HOLD'], true)) {
            return false;
        }

        $existing->setStatus('WAITING');
        $existing->setCreatedAt(new \DateTime());
        $existing->setHoldExpiresAt(null);
        $existing->setHoldToken(null);

        $this->em->flush();

        return true;
    }

    $waitlist = new Waitlist();
    $waitlist->setPersonne($personne);
    $waitlist->setActivite($activite);
    $waitlist->setStatus('WAITING');
    $waitlist->setCreatedAt(new \DateTime());
    $waitlist->setHoldExpiresAt(null);
    $waitlist->setHoldToken(null);

    $this->em->persist($waitlist);
    $this->em->flush();

    return true;
}
    public function promoteNext(Activite $activite): ?Waitlist
{
    $existingHold = $this->waitlistRepository->findActiveHoldForActivity($activite);

    if ($existingHold) {
        return null;
    }

    $next = $this->waitlistRepository->findNextWaitingForActivity($activite);

    if (!$next) {
        return null;
    }

    $next->setStatus('HOLD');
    $next->setHoldToken(bin2hex(random_bytes(32)));
    $next->setHoldExpiresAt((new \DateTime())->modify('+20 minutes'));

    $notification = new Notification();
    $notification->setMessage('Une place s’est libérée pour "' . $activite->getNom() . '". Réservez maintenant.');
    $notification->setType('WAITLIST_HOLD');
    $notification->setActivite_id($activite);
    $notification->setReceiver_id($next->getPersonne());
    $notification->setIs_read(false);
    $notification->setCreated_at(new \DateTime());
    $notification->setIs_sent_sms(false);

    $this->em->persist($notification);
    $this->em->flush();

    return $next;
}
    public function refuseHold(Personne $personne, Activite $activite): bool
{
    $entry = $this->waitlistRepository->findOneBy([
        'personne' => $personne,
        'activite' => $activite,
        'status' => 'HOLD'
    ]);

    if (!$entry) {
        return false;
    }

    $entry->setStatus('CANCELLED');
    $entry->setHoldToken(null);
    $entry->setHoldExpiresAt(null);

    $this->em->flush();

    if ($activite->getMaxPlaces() > 0) {
        $this->promoteNext($activite);
    }

    return true;
}
    public function confirmHold(Personne $personne, Activite $activite, int $qty = 1): bool
{
    $entry = $this->waitlistRepository->findOneBy([
        'personne' => $personne,
        'activite' => $activite,
        'status' => 'HOLD'
    ]);

    if (!$entry) {
        return false;
    }

    if (!$entry->getHoldExpiresAt() || $entry->getHoldExpiresAt() < new \DateTime()) {
        $entry->setStatus('EXPIRED');
        $entry->setHoldToken(null);
        $entry->setHoldExpiresAt(null);
        $this->em->flush();

        return false;
    }

    if ($qty < 1) {
        return false;
    }

    if ($qty > $activite->getMaxPlaces()) {
        return false;
    }

    if (!$activite->getDestination()) {
        return false;
    }

    $reservation = new Reservation();
    $reservation->setDateReservation(new \DateTime());
    $reservation->setDateDebut($activite->getDateDebut());
    $reservation->setDateFin($activite->getDateFin());
    $reservation->setStatut('réservée');

    $unitPrice = (
        $activite->getIsFlashSale() &&
        $activite->getFlashPrice() !== null &&
        $activite->getFlashExpiresAt() !== null &&
        $activite->getFlashExpiresAt() > new \DateTime()
    )
        ? $activite->getFlashPrice()
        : $activite->getPrix();

    $reservation->setCoutTotal($unitPrice * $qty);
    $reservation->setNb_tickets($qty);
    $reservation->setPersonne_id($personne);
    $reservation->setDestination($activite->getDestination());

    $this->em->persist($reservation);

    for ($i = 0; $i < $qty; $i++) {
        $ticket = new Ticket();
        $ticket->setType('Activité');
        $ticket->setStatut('Reservé');
        $ticket->setPrix($unitPrice);
        $ticket->setReservation_id($reservation);
        $ticket->setActivite($activite);
        $ticket->setDestination($activite->getDestination());

        $this->em->persist($ticket);
    }

    $activite->setMaxPlaces($activite->getMaxPlaces() - $qty);

    $entry->setStatus('CONFIRMED');
    $entry->setHoldToken(null);
    $entry->setHoldExpiresAt(null);

    $this->em->flush();

    try {
        if ($personne->getEmail()) {
            $this->bookingEmailService->sendBookingConfirmation(
                $personne->getEmail(),
                trim(($personne->getNom() ?? '') . ' ' . ($personne->getPrenom() ?? '')),
                $activite->getNom(),
                (float) $reservation->getCoutTotal(),
                $reservation->getId(),
                $personne->getId(),
                $activite->getId(),
                $activite->getDateDebut(),
                $activite->getDateFin()
            );
        }
    } catch (\Throwable $e) {
    }

    if ($activite->getMaxPlaces() > 0) {
        $this->promoteNext($activite);
    }

    return true;
}

   public function markNotificationAsConfirmed(Personne $personne, Activite $activite): void
{
    $notification = $this->notificationRepository->findLatestUnreadWaitlistHoldByReceiverAndActivite($personne, $activite);

    if ($notification) {
        $notification->setIs_read(true);
        $notification->setType('BOOKING_CONFIRMED');
        $this->em->flush();
    }
}

    public function markNotificationAsCancelled(Personne $personne, Activite $activite): void
{
    $notification = $this->notificationRepository->findLatestUnreadWaitlistHoldByReceiverAndActivite($personne, $activite);

    if ($notification) {
        $notification->setIs_read(true);
        $notification->setType('BOOKING_CANCELLED');
        $this->em->flush();
    }
}
public function expireExpiredHolds(): int
{
    $expiredHolds = $this->waitlistRepository->findExpiredHolds();
    $count = 0;

    foreach ($expiredHolds as $entry) {
        $activite = $entry->getActivite();
        $personne = $entry->getPersonne();

        $entry->setStatus('EXPIRED');
        $entry->setHoldToken(null);
        $entry->setHoldExpiresAt(null);

        $notification = $this->notificationRepository
            ->findLatestUnreadWaitlistHoldByReceiverAndActivite($personne, $activite);

        if ($notification) {
            $notification->setIs_read(true);
            $notification->setType('WAITLIST_EXPIRED');
        }

        $this->em->flush();

        if ($activite && $activite->getMaxPlaces() > 0) {
            $this->promoteNext($activite);
        }

        $count++;
    }

    return $count;
}
}