<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Personne;
use App\Service\WaitlistService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

final class WaitlistController extends AbstractController
{
    #[Route('/waitlist/join/{id}', name: 'waitlist_join')]
    public function join(
        Activite $activite,
        WaitlistService $waitlistService,
        EntityManagerInterface $em,
        Request $request
    ): RedirectResponse {
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            $this->addFlash('error', 'Vous devez vous connecter d’abord.');
            return $this->redirectToRoute('app_login');
        }

        $personne = $em->getRepository(Personne::class)->find($userId);

        if (!$personne) {
            $this->addFlash('error', 'Utilisateur introuvable.');
            return $this->redirectToRoute('activite');
        }

        $added = $waitlistService->joinWaitlist($personne, $activite);

        if (!$added) {
            $this->addFlash('warning', 'Vous êtes déjà dans la liste d’attente pour cette activité.');
        } else {
            $this->addFlash('success', 'Vous avez rejoint la liste d’attente avec succès.');
        }

        return $this->redirectToRoute('activite');
    }

    #[Route('/waitlist/confirm/{id}', name: 'waitlist_confirm')]
public function confirm(
    Activite $activite,
    Request $request,
    EntityManagerInterface $em,
    WaitlistService $waitlistService
): RedirectResponse {
    $userId = $request->getSession()->get('user_id');

    if (!$userId) {
        $this->addFlash('error', 'Vous devez vous connecter.');
        return $this->redirectToRoute('app_login');
    }

    $personne = $em->getRepository(Personne::class)->find($userId);

    if (!$personne) {
        $this->addFlash('error', 'Utilisateur introuvable.');
        return $this->redirectToRoute('activite');
    }

    $qty = max(1, (int) $request->query->get('qty', 1));

    $ok = $waitlistService->confirmHold($personne, $activite, $qty);

    if ($ok) {
        $waitlistService->markNotificationAsConfirmed($personne, $activite);
        $this->addFlash('success', 'Votre réservation a été confirmée.');
    } else {
        $this->addFlash('error', 'Le délai est expiré ou cette place n’est plus disponible.');
    }

    return $this->redirectToRoute('activite');
}
    #[Route('/waitlist/refuse/{id}', name: 'waitlist_refuse')]
    public function refuse(
        Activite $activite,
        Request $request,
        EntityManagerInterface $em,
        WaitlistService $waitlistService
    ): RedirectResponse {
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            $this->addFlash('error', 'Vous devez vous connecter.');
            return $this->redirectToRoute('app_login');
        }

        $personne = $em->getRepository(Personne::class)->find($userId);

        if (!$personne) {
            $this->addFlash('error', 'Utilisateur introuvable.');
            return $this->redirectToRoute('activite');
        }

        $ok = $waitlistService->refuseHold($personne, $activite);

        if ($ok) {
            $waitlistService->markNotificationAsCancelled($personne, $activite);
            $this->addFlash('info', 'Vous avez refusé la place.');
        } else {
            $this->addFlash('error', 'Aucune place en attente pour vous.');
        }

        return $this->redirectToRoute('activite');
    }
}