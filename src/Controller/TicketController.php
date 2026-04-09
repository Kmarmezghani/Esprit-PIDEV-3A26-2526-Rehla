<?php

namespace App\Controller;

use App\Entity\Ticket;
use App\Entity\Reservation;
use App\Form\TicketType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;   // ⭐ THIS WAS MISSING
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

final class TicketController extends AbstractController
{
   /* #[Route('/ticket', name: 'app_ticket')]
    public function index(): Response
    {
        return $this->render('ticket/back/formTicket.html.twig', [
            'controller_name' => 'TicketController',
        ]);
    }*/
        #[Route('/ticket', name: 'admin_tickets')]
public function index(EntityManagerInterface $em): Response
{
    return $this->render('/ticket/index.html.twig', [
        'tickets' => $em->getRepository(Ticket::class)->findAll(),
        'reservations' => $em->getRepository(Reservation::class)->findAll(), // ← ajoute ça
    ]);
}
    #[Route('/ticket/back', name: 'admin_ticket_new')]
public function newTicket(Request $request, EntityManagerInterface $em): Response
{
    $ticket = new Ticket();
    $form = $this->createForm(TicketType::class, $ticket);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $em->persist($ticket);
        $em->flush();

        return $this->redirectToRoute('admin_tickets');
    }

    return $this->render('/ticket/back/formTicket.html.twig', [
        'form' => $form->createView()
    ]);
}
#[Route('/ticket/edit/{id}', name: 'admin_ticket_edit')]
public function editTicket(Ticket $ticket, Request $request, EntityManagerInterface $em): Response
{
    $form = $this->createForm(TicketType::class, $ticket);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $em->flush(); // no persist needed, entity is already managed
        return $this->redirectToRoute('admin_tickets'); // go back to the list
    }

    return $this->render('/ticket/back/editTicket.html.twig', [
        'form' => $form->createView(),
        'edit' => true, // optional flag to change the heading or button text
    ]);
}

#[Route('/ticket/delete/{id}', name: 'admin_ticket_delete')]
public function delete(Ticket $ticket, EntityManagerInterface $em, Request $request)
{
    if ($this->isCsrfTokenValid('delete'.$ticket->getId(), $request->request->get('_token'))) {
        $em->remove($ticket);
        $em->flush();

        $this->addFlash('success', 'Ticket deleted successfully 🗑️');
    }

    return $this->redirectToRoute('admin_tickets');
}
}
