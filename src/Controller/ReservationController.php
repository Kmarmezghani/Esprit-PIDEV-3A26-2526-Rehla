<?php

namespace App\Controller;

use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Entity\Ville; 
use App\Entity\Personne;
use App\Form\ReservationType;
use App\Form\ReservationUserType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

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
    
public function edit(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    $oldStatut = $reservation->getStatut();

    $form = $this->createForm(ReservationType::class, $reservation);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        if ($oldStatut !== 'annulée' && $reservation->getStatut() === 'annulée') {
            $this->restoreActivityPlacesFromReservation($reservation, $em);
        }

        $em->flush();
        return $this->redirectToRoute('admin_reservations_tickets');
    }

    return $this->render('reservation/back/edit.html.twig', [
        'form' => $form->createView()
    ]);
}
    #[Route('/reservation/delete/{id}', name: 'admin_reservation_delete')]
public function delete(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    if ($this->isCsrfTokenValid('delete' . $reservation->getId(), $request->request->get('_token'))) {
        $this->restoreActivityPlacesFromReservation($reservation, $em);
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
public function mesReservations(Request $request, EntityManagerInterface $em): Response
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

    return $this->render('reservation/mes_reservations.html.twig', [
        'reservations' => $reservations
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

public function editUser(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    $oldStatut = $reservation->getStatut();

    $form = $this->createForm(ReservationUserType::class, $reservation, ['is_edit' => true]);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        if ($oldStatut !== 'annulée' && $reservation->getStatut() === 'annulée') {
            $this->restoreActivityPlacesFromReservation($reservation, $em);
        }

        $em->flush();
        return $this->redirectToRoute('mes_reservations');
    }

    return $this->render('reservation/step1.html.twig', [
        'form' => $form->createView(),
        'isEdit' => true
    ]);
}
#[Route('/mes-reservations/delete/{id}', name: 'user_reservation_delete', methods:['POST'])]
public function deleteUserReservation(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    if ($this->isCsrfTokenValid('delete'.$reservation->getId(), $request->request->get('_token'))) {
        $this->restoreActivityPlacesFromReservation($reservation, $em);
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
}