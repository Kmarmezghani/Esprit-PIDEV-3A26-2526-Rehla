<?php

namespace App\EventSubscriber;

use App\Entity\Reservation;
use CalendarBundle\CalendarEvents;
use CalendarBundle\Entity\Event as CalendarBundleEvent;
use CalendarBundle\Event\CalendarEvent;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpFoundation\RequestStack;

class ReservationCalendarSubscriber implements EventSubscriberInterface
{
    public function __construct(
        private EntityManagerInterface $em,
        private RequestStack $requestStack
    ) {}

    public static function getSubscribedEvents(): array
    {
        return [
            CalendarEvents::SET_DATA => 'onCalendarSetData',
        ];
    }

    public function onCalendarSetData(CalendarEvent $calendar): void
    {
        $start = $calendar->getStart();
        $end   = $calendar->getEnd();

        $session = $this->requestStack->getSession();
        $userId  = $session->get('user_id');

        if (!$userId) return;

        $reservationId = $session->get('selected_reservation_id');
        if (!$reservationId) return;

        $reservation = $this->em->getRepository(Reservation::class)->find($reservationId);
        if (!$reservation) return;

        $ville = $reservation->getDestination()?->getNom() ?? '';

        foreach ($reservation->getTickets() as $ticket) {
            $current   = clone $reservation->getDateDebut();
            $ticketEnd = clone $reservation->getDateFin();

            while ($current <= $ticketEnd) {
                if ($current >= $start && $current <= $end) {
                    $event = new CalendarBundleEvent(
                        $ticket->getType(),
                        clone $current,
                        clone $current
                    );

                    $event->addOption('extendedProps', [
                        'ticketId' => $ticket->getId(),
                        'ville'    => $ville,
                    ]);

                    $event->addOption('id', $ticket->getId() . '-' . $current->format('Y-m-d'));

                    $calendar->addEvent($event);
                }

                $current->modify('+1 day');
            }
        }
    }
}