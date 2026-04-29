<?php

namespace App\Controller;

use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Entity\Ville; 
use App\Entity\Personne;
use App\Form\ReservationType;
use App\Form\ReservationUserType;
use App\Repository\NotificationRepository;
use App\Repository\PersonneRepository;
use App\Service\BookingEmailService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use App\Service\PdfGenerator;
use App\Service\WaitlistService;

class ReservationController extends AbstractController
{
    #[Route('/admin/reservations-tickets', name: 'admin_reservations_tickets')]
public function index(Request $request, EntityManagerInterface $em): Response
{
    $search = $request->query->get('search');

    if ($search) {
        $reservations = $em->getRepository(Reservation::class)
            ->createQueryBuilder('r')
            ->leftJoin('r.personne_id', 'p')
            ->leftJoin('r.destination', 'd')
            ->where('p.nom LIKE :search OR d.nom LIKE :search OR r.statut LIKE :search')
            ->setParameter('search', '%' . $search . '%')
            ->getQuery()
            ->getResult();

        $tickets = $em->getRepository(Ticket::class)
            ->createQueryBuilder('t')
            ->leftJoin('t.destination', 'd')
            ->where('t.type LIKE :search OR d.nom LIKE :search OR t.statut LIKE :search')
            ->setParameter('search', '%' . $search . '%')
            ->getQuery()
            ->getResult();
    } else {
        $reservations = $em->getRepository(Reservation::class)->findAll();
        $tickets = $em->getRepository(Ticket::class)->findAll();
    }

    return $this->render('admin/reservations_tickets.html.twig', [
        'reservations' => $reservations,
        'tickets' => $tickets,
        'search' => $search
    ]);
}

    #[Route('/admin/reservation/new', name: 'admin_reservation_new')]
    public function newAdmin(Request $request, EntityManagerInterface $em): Response
    {
        $reservation = new Reservation();
        $form = $this->createForm(ReservationType::class, $reservation );
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($reservation);
            $em->flush();
            return $this->redirectToRoute('admin_reservations_tickets');
        }

        return $this->render('reservation/new.html.twig', [
            'form' => $form->createView()
        ]);
    }

   #[Route('/reservation/edit/{id}', name: 'admin_reservation_edit')]
public function edit(
    Reservation $reservation,
    Request $request,
    EntityManagerInterface $em,
    BookingEmailService $bookingEmailService,
    WaitlistService $waitlistService
): Response
{
    $oldStatut = $reservation->getStatut();

    $form = $this->createForm(ReservationType::class, $reservation);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        if ($oldStatut !== 'annulée' && $reservation->getStatut() === 'annulée') {
            $this->sendCancellationEmailIfActivityReservation($reservation, $bookingEmailService, $em);
            $this->restoreActivityPlacesFromReservation($reservation, $em);

            $processedActivities = [];

            foreach ($reservation->getTickets() as $ticket) {
                $activite = $ticket->getActivite();

                if ($activite && !in_array($activite->getId(), $processedActivities, true)) {
                    $waitlistService->promoteNext($activite);
                    $processedActivities[] = $activite->getId();
                }
            }
        }

        $em->flush();

        return $this->redirectToRoute('admin_reservations_tickets');
    }

    return $this->render('reservation/back/edit.html.twig', [
        'form' => $form->createView()
    ]);
}
    #[Route('/reservation/delete/{id}', name: 'admin_reservation_delete')]
public function delete(
    Reservation $reservation,
    Request $request,
    EntityManagerInterface $em,
    BookingEmailService $bookingEmailService,
    WaitlistService $waitlistService
): Response
{
    if ($this->isCsrfTokenValid('delete' . $reservation->getId(), $request->request->get('_token'))) {
        $this->sendCancellationEmailIfActivityReservation($reservation, $bookingEmailService, $em);
        $this->restoreActivityPlacesFromReservation($reservation, $em);

        $processedActivities = [];

        foreach ($reservation->getTickets() as $ticket) {
            $activite = $ticket->getActivite();

            if ($activite && !in_array($activite->getId(), $processedActivities, true)) {
                $waitlistService->promoteNext($activite);
                $processedActivities[] = $activite->getId();
            }
        }

        $em->remove($reservation);
        $em->flush();
    }

    return $this->redirectToRoute('admin_reservations_tickets');
}

    #[Route('/admin/dashboard', name: 'admin_dashboard')]
public function dashboard(EntityManagerInterface $em): Response
{
    return $this->render('admin/reservations_tickets.html.twig', [
        'reservations' => $em->getRepository(Reservation::class)->findAll(),
        'tickets' => $em->getRepository(Ticket::class)->findAll(),
        'selectedReservation' => null
    ]);
}

   #[Route('/admin/reservation/{id}/tickets', name: 'admin_reservation_tickets')]
public function reservationTickets(Reservation $reservation, EntityManagerInterface $em): Response
{
    $tickets = $em->getRepository(Ticket::class)
                  ->findBy(['reservation_id' => $reservation]);

    $reservations = $em->getRepository(Reservation::class)->findAll();

    return $this->render('admin/reservations_tickets.html.twig', [
        'reservations' => $reservations,
        'tickets' => $tickets,
        'selectedReservation' => $reservation
    ]);
}
#[Route('/reservation/{id}/tickets', name: 'reservation_tickets')]
public function getTickets(Reservation $reservation): Response
{
    $events = [];
    $ville = $reservation->getDestination()->getNom();

    foreach ($reservation->getTickets() as $ticket) {
        $start = clone $reservation->getDateDebut();
        $end   = clone $reservation->getDateFin();

        // Loop through every day of the reservation
        $current = clone $start;
        while ($current <= $end) {
            $events[] = [
                'id'    => $ticket->getId() . '-' . $current->format('Y-m-d'),
                'realId' => $ticket->getId(), // ← used for deletion
                'title' => $ticket->getType(),
                'start' => $current->format('Y-m-d'),
                'end'   => $current->format('Y-m-d'),
                'extendedProps' => [
                    'ville'   => $ville,
                    'ticketId' => $ticket->getId()
                ]
            ];
            $current->modify('+1 day');
        }
    }

    return $this->json($events);
}

#[Route('/mes-reservations', name: 'mes_reservations')]
public function mesReservations(Request $request,
PersonneRepository $personneRepository,
NotificationRepository $notificationRepository, EntityManagerInterface $em): Response
{
    $userId = $request->getSession()->get('user_id');

    if (!$userId) {
        return $this->redirectToRoute('home'); 
    }

    $reservations = $em->getRepository(Reservation::class)
        ->createQueryBuilder('r')
        ->where('r.personne_id = :id')
        ->setParameter('id', $userId)
        ->getQuery()
        ->getResult();

    $hasUnreadActivityNotifications = false;
$activityNotifications = [];

$userId = $request->getSession()->get('user_id');

if ($userId) {
    $personne = $personneRepository->find($userId);

    if ($personne) {
        $activityNotifications = $notificationRepository->findActivityNotificationsByUser($personne);
        $hasUnreadActivityNotifications = $notificationRepository->hasUnreadActivityNotifications($personne);
    }
}
    return $this->render('reservation/mes_reservations.html.twig', [
        'reservations' => $reservations,
        'activityNotifications' => $activityNotifications,
'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
    ]);
}
#[Route('/reservation/new', name: 'ajouter_reservation')]
public function newUser(Request $request, EntityManagerInterface $em): Response
{
    $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);
    $reservation = new Reservation();
    $form = $this->createForm(ReservationUserType::class, $reservation,['is_edit' => false]);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        $ticketsChoisis = $form->get('tickets')->getData();

      
        if (count($ticketsChoisis) === 0) {
            $this->addFlash('error', 'Veuillez sélectionner au moins un ticket.');
            return $this->render('reservation/step1.html.twig', [
                'form' => $form->createView()
            ]);
        }

        $total = 0;
        $nbTickets = count($ticketsChoisis);

        foreach ($ticketsChoisis as $ticket) {
            $ticket->setReservation_id($reservation);
            $ticket->setStatut('Reservé');
            $reservation->addTicket($ticket);
            $em->persist($ticket);
            $total += $ticket->getPrix();
        }

        $reservation->setDateReservation(new \DateTime());
        $reservation->setStatut('réservée');
        $reservation->setCoutTotal($total);
        $reservation->setNb_tickets($nbTickets);

        $reservation->setPersonne_id($personne);
        $em->persist($reservation);
        $em->flush();

        return $this->redirectToRoute('mes_reservations');
    }

    return $this->render('reservation/step1.html.twig', [
        'form' => $form->createView(),
        'isEdit' => false
    ]);
}

#[Route('/mes-reservations/edit/{id}', name: 'user_reservation_edit')]
public function editUser(
    Reservation $reservation,
    Request $request,
    EntityManagerInterface $em,
    BookingEmailService $bookingEmailService,
    WaitlistService $waitlistService
): Response
{
    $oldStatut = $reservation->getStatut();

    $form = $this->createForm(ReservationUserType::class, $reservation, ['is_edit' => true]);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        if ($oldStatut !== 'annulée' && $reservation->getStatut() === 'annulée') {
            $this->sendCancellationEmailIfActivityReservation($reservation, $bookingEmailService, $em);
            $this->restoreActivityPlacesFromReservation($reservation, $em);

            foreach ($reservation->getTickets() as $ticket) {
                if ($ticket->getActivite()) {
                    $waitlistService->promoteNext($ticket->getActivite());
                }
            }
        }

        $total = 0;
        foreach ($reservation->getTickets() as $ticket) {
            $total += $ticket->getPrix();
        }

        $reservation->setCoutTotal($total);
        $reservation->setNb_tickets(count($reservation->getTickets()));

        $em->flush();

        return $this->redirectToRoute('mes_reservations');
    }

    return $this->render('reservation/step1.html.twig', [
        'form' => $form->createView(),
        'isEdit' => true
    ]);
}
#[Route('/mes-reservations/delete/{id}', name: 'user_reservation_delete', methods: ['POST'])]
public function deleteUserReservation(
    Reservation $reservation,
    Request $request,
    EntityManagerInterface $em,
    BookingEmailService $bookingEmailService,
    WaitlistService $waitlistService
): Response
{
    if ($this->isCsrfTokenValid('delete' . $reservation->getId(), $request->request->get('_token'))) {
        $this->sendCancellationEmailIfActivityReservation($reservation, $bookingEmailService, $em);
        $this->restoreActivityPlacesFromReservation($reservation, $em);

        $processedActivities = [];

        foreach ($reservation->getTickets() as $ticket) {
            $activite = $ticket->getActivite();

            if ($activite && !in_array($activite->getId(), $processedActivities, true)) {
                $waitlistService->promoteNext($activite);
                $processedActivities[] = $activite->getId();
            }
        }

        $em->remove($reservation);
        $em->flush();
    }

    return $this->redirectToRoute('mes_reservations');
}
#[Route('/reservation/ticket/delete-ajax/{id}', name:'ticket_delete_ajax')]
public function deleteTicketAjax(Ticket $ticket, EntityManagerInterface $em): Response
{
    $reservation = $ticket->getReservation_id();
    $activite = $ticket->getActivite();

    if ($activite) {
        $activite->setMaxPlaces($activite->getMaxPlaces() + 1);
    }

    $prixTicket = $ticket->getPrix();

    $em->remove($ticket);

    if ($reservation) {
        $reservation->setCoutTotal($reservation->getCoutTotal() - $prixTicket);
        $reservation->setNb_tickets($reservation->getNb_tickets() - 1);

        if ($reservation->getNb_tickets() <= 0) {
            $reservation->setNb_tickets(0);
            $reservation->setStatut('annulée');
        }
    }

    $em->flush();

    return $this->json(['success' => true]);
}
private function restoreActivityPlacesFromReservation(Reservation $reservation, EntityManagerInterface $em): void
{
    $tickets = $em->getRepository(Ticket::class)->findBy([
        'reservation_id' => $reservation
    ]);

    foreach ($tickets as $ticket) {
        $activite = $ticket->getActivite();

        if ($activite) {
            // Ticket linked to an activity → restore place and delete
            $activite->setMaxPlaces($activite->getMaxPlaces() + 1);
            $em->remove($ticket);
        } else {
            // Ticket not linked to an activity → free it up
            $ticket->setStatut('Disponible');
            $ticket->setReservation_id(null);
        }
    }
}
#[Route('/api/exchange-rates', name: 'exchange_rates')]
public function getRates(): Response
{
    $key = $_ENV['EXCHANGE_RATE_API_KEY'];
    $data = file_get_contents("https://v6.exchangerate-api.com/v6/{$key}/latest/TND");
    return new Response($data, 200, ['Content-Type' => 'application/json']);
}
private function sendCancellationEmailIfActivityReservation(
    Reservation $reservation,
    BookingEmailService $bookingEmailService,
    EntityManagerInterface $em
): void {
    $personne = $reservation->getPersonne_id();

    if (!$personne || !$personne->getEmail()) {
        return;
    }

    $tickets = $em->getRepository(Ticket::class)->findBy([
        'reservation_id' => $reservation
    ]);

    if (!$tickets || count($tickets) === 0) {
        return;
    }

    $firstActivityTicket = null;

    foreach ($tickets as $ticket) {
        if ($ticket->getActivite()) {
            $firstActivityTicket = $ticket;
            break;
        }
    }

    if (!$firstActivityTicket) {
        return;
    }

    $activite = $firstActivityTicket->getActivite();

    if (!$activite) {
        return;
    }

   $userFullName = trim(($personne->getNom() ?? '') . ' ' . ($personne->getPrenom() ?? ''));


$bookingEmailService->regenerateCancelledBookingPass(
    $userFullName,
    $activite->getNom(),
    (float) $reservation->getCoutTotal(),
    $reservation->getId(),
    $activite->getDateDebut(),
    $activite->getDateFin()
);


$bookingEmailService->sendBookingCancellation(
    $personne->getEmail(),
    $userFullName,
    $activite->getNom(),
    (float) $reservation->getCoutTotal()
);
}
#[Route('/booking-pass/{id}', name: 'booking_pass_show', methods: ['GET'])]
public function showBookingPass(Reservation $reservation): Response
{
    return new Response('OK');
}

#[Route('/api/tickets-by-destination', name: 'api_tickets_by_destination')]
public function ticketsByDestination(Request $request, EntityManagerInterface $em): Response
{
    $villeId     = $request->query->get('ville');
    $reservaId   = $request->query->get('reservation'); // pour le mode edit
    $currentUser = $request->getSession()->get('user_id');

    $qb = $em->getRepository(Ticket::class)->createQueryBuilder('t')
        ->where('t.reservation_id IS NULL');

    // En mode édition : inclure les tickets déjà liés à cette réservation
    if ($reservaId) {
        $qb->orWhere('t.reservation_id = :resa')
           ->setParameter('resa', $reservaId);
    }

    if ($villeId) {
        $qb->andWhere('t.destination = :dest')
           ->setParameter('dest', $villeId);
    }

    $tickets = $qb->getQuery()->getResult();

    // Tickets déjà sélectionnés dans la réservation courante
    $selectedIds = [];
    if ($reservaId) {
        $resa = $em->getRepository(Reservation::class)->find($reservaId);
        if ($resa) {
            $selectedIds = $resa->getTickets()->map(fn(Ticket $t) => $t->getId())->toArray();
        }
    }

    $data = array_map(fn(Ticket $t) => [
        'id'       => $t->getId(),
        'type'     => $t->getType(),
        'prix'     => $t->getPrix(),
        'selected' => in_array($t->getId(), $selectedIds),
    ], $tickets);

    return $this->json($data);
}

#[Route('/reservation/{id}/receipt', name:'reservation_receipt')]
public function receipt(Reservation $reservation, PdfGenerator $pdf): Response
{
    $pdfContent = $pdf->generateReservationPdf($reservation);

    return new Response(
        $pdfContent,
        200,
        [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="recu-reservation-'.$reservation->getId().'.pdf"'
        ]
    );
}

#[Route('/reservation/select/{id}', name: 'reservation_select', methods: ['POST'])]
public function selectReservation(Reservation $reservation, Request $request): Response
{
    $userId = $request->getSession()->get('user_id');

    // Security: make sure the reservation belongs to the logged-in user
    if ($reservation->getPersonne_id()->getId() !== $userId) {
        return $this->json(['error' => 'Unauthorized'], 403);
    }

    $request->getSession()->set('selected_reservation_id', $reservation->getId());

    return $this->json(['success' => true]);
}

}