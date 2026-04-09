<?php

namespace App\Controller;

use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Form\ReservationType;
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

    #[Route('/reservation/new', name: 'admin_reservation_new')]
    public function new(Request $request, EntityManagerInterface $em): Response
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
        ]);
    }
}