<?php

namespace App\Controller;

use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Entity\Ville; 
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
    public function index(EntityManagerInterface $em): Response
    {
        return $this->render('admin/reservations_tickets.html.twig', [
            'reservations' => $em->getRepository(Reservation::class)->findAll(),
            'tickets' => $em->getRepository(Ticket::class)->findAll(),
        ]);
    }

    #[Route('/admin/reservation/new', name: 'admin_reservation_new')]
    public function newAdmin(Request $request, EntityManagerInterface $em): Response
    {
        $reservation = new Reservation();
        $form = $this->createForm(ReservationType::class, $reservation);
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
        $form = $this->createForm(ReservationType::class, $reservation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
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

    foreach ($reservation->getTickets() as $ticket) {
        $events[] = [
            'id' => $ticket->getId(), 
            'title' => $ticket->getType(),
            'start' => $reservation->getDateDebut()->format('Y-m-d'),
            'end' => $reservation->getDateFin()->format('Y-m-d'),
        ];
    }

    return $this->json($events);
}

#[Route('/mes-reservations', name: 'mes_reservations')]
public function mesReservations(EntityManagerInterface $em): Response
{
    $reservations = $em->getRepository(Reservation::class)->findAll();

    return $this->render('reservation/mes_reservations.html.twig', [
        'reservations' => $reservations
    ]);
}

#[Route('/reservation/new', name: 'ajouter_reservation')]
public function newUser(Request $request, EntityManagerInterface $em): Response
{
    $reservation = new Reservation();
    $form = $this->createForm(ReservationUserType::class, $reservation);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        $ticketsChoisis = $form->get('tickets')->getData();

        // ✅ Manual guard — not a validator concern
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

        $em->persist($reservation);
        $em->flush();

        return $this->redirectToRoute('mes_reservations');
    }

    return $this->render('reservation/step1.html.twig', [
        'form' => $form->createView()
    ]);
}

#[Route('/mes-reservations/edit/{id}', name: 'user_reservation_edit')]
public function editUser(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    $form = $this->createForm(ReservationUserType::class, $reservation);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $em->flush();
        return $this->redirectToRoute('mes_reservations');
    }

    return $this->render('reservation/step1.html.twig', [
        'form' => $form->createView()
    ]);
}

#[Route('/mes-reservations/delete/{id}', name: 'user_reservation_delete', methods:['POST'])]
public function deleteUserReservation(Reservation $reservation, Request $request, EntityManagerInterface $em): Response
{
    if ($this->isCsrfTokenValid('delete'.$reservation->getId(), $request->request->get('_token'))) {
        $em->remove($reservation);
        $em->flush();
    }

    return $this->redirectToRoute('mes_reservations');
}

#[Route('/reservation/ticket/delete-ajax/{id}', name:'ticket_delete_ajax')]
public function deleteTicketAjax(Ticket $ticket, EntityManagerInterface $em): Response
{
    $reservation = $ticket->getReservation_id();

    $reservation->removeTicket($ticket);
    $ticket->setReservation_id(null);
    $ticket->setStatut('Disponible');

    // recalcul
    $total = 0;
    foreach ($reservation->getTickets() as $t) {
        $total += $t->getPrix();
    }

    $reservation->setCoutTotal($total);
    $reservation->setNb_tickets(count($reservation->getTickets()));

    $em->flush();

    return $this->json(['success'=>true]);
}
}