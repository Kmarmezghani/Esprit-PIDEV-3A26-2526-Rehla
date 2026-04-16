<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Avis;
use App\Form\AvisType;
use App\Repository\ActiviteRepository;
use App\Repository\AvisRepository;
use App\Repository\PersonneRepository;
use App\Service\AvisService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use App\Entity\Reservation;
use App\Entity\Ticket;
use App\Entity\Waitlist;
use App\Service\BookingEmailService;
use App\Service\GeminiRecommendationService;
use App\Service\ActivityMaintenanceService;
use App\Entity\Notification;
use App\Repository\NotificationRepository;
use App\Repository\WaitlistRepository;
use App\Service\WaitlistService;

final class ActiviteController extends AbstractController
{
#[Route('/activite', name: 'activite')]

public function index(
    Request $request,
    ActiviteRepository $activiteRepository,
    PersonneRepository $personneRepository,
    GeminiRecommendationService $geminiRecommendationService,
    ActivityMaintenanceService $activityMaintenanceService,
    EntityManagerInterface $em,
    NotificationRepository $notificationRepository,
    WaitlistRepository $waitlistRepository,
    WaitlistService $waitlistService
): Response
{
    $waitlistService->expireExpiredHolds();
    $activityMaintenanceService->refreshStatusesAndFlashSales();

    $destination = trim((string) $request->query->get('destination', ''));
    $dateDebut = $request->query->get('date_debut');
    $dateFin = $request->query->get('date_fin');
    $prixMax = $request->query->get('prix_max');

    $activites = $activiteRepository->searchFront(
        $destination,
        $dateDebut,
        $dateFin,
        $prixMax
    );

    $recommendedActivities = [];
    $recommendedIds = [];
    $waitlistActivities = [];
    $holdActivities = [];
    $activityCanBook = [];
    $activityNotifications = [];
    $hasUnreadActivityNotifications = false;

    $userId = $request->getSession()->get('user_id');
    $personne = null;

    if ($userId) {
        $personne = $personneRepository->find($userId);

        if ($personne) {
            $allActivities = $activiteRepository->findBy([
                'status' => 'DISPONIBLE'
            ]);

            $userProfileText = $this->buildUserProfileText($personne->getPreferences());

            $rankedIds = $geminiRecommendationService->rankActivityIdsMax3Cached(
                $personne->getId(),
                $allActivities,
                $userProfileText
            );

            if (!empty($rankedIds)) {
                $map = [];

                foreach ($allActivities as $activity) {
                    $map[$activity->getId()] = $activity;
                }

                foreach ($rankedIds as $id) {
                    if (isset($map[$id])) {
                        $recommendedActivities[] = $map[$id];
                    }
                }
            }

            $waitlists = $em->getRepository(Waitlist::class)->findBy([
                'personne' => $personne,
                'status' => ['WAITING', 'HOLD']
            ]);

            foreach ($waitlists as $w) {
                $waitlistActivities[] = $w->getActivite()->getId();
            }

            $userHolds = $waitlistRepository->findActiveHoldsByPersonne($personne);

            foreach ($userHolds as $hold) {
                $holdActivities[] = $hold->getActivite()->getId();
            }

            $activityNotifications = $notificationRepository->findActivityNotificationsByUser($personne);
            $hasUnreadActivityNotifications = $notificationRepository->hasUnreadActivityNotifications($personne);
        }
    }

    foreach ($activites as $activite) {
        $activeHoldCount = $waitlistRepository->countActiveHoldsForActivity($activite);

        $activityCanBook[$activite->getId()] =
            ((int) $activite->getMaxPlaces() > 0) && ($activeHoldCount === 0);
    }

    $recommendedIds = array_map(
        fn($activity) => $activity->getId(),
        $recommendedActivities
    );

    return $this->render('activite/activite.html.twig', [
        'activites' => $activites,
        'recommendedActivities' => $recommendedActivities,
        'recommendedIds' => $recommendedIds,
        'waitlistActivities' => $waitlistActivities,
        'holdActivities' => $holdActivities,
        'activityCanBook' => $activityCanBook,
        'activityNotifications' => $activityNotifications,
        'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
        'filters' => [
            'destination' => $destination,
            'date_debut' => $dateDebut,
            'date_fin' => $dateFin,
            'prix_max' => $prixMax,
        ]
    ]);
}    #[Route('/activite/{id}', name: 'activite_show')]
    public function show(
        Activite $activite,
        Request $request,
        EntityManagerInterface $em,
        AvisRepository $avisRepository,
        PersonneRepository $personneRepository,
        AvisService $avisService
    ): Response
    {
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            $this->addFlash('danger', 'Vous devez être connecté.');
            return $this->redirectToRoute('app_login');
        }

        $personne = $personneRepository->find($userId);

        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('activite');
        }

        $existingAvis = $avisRepository->findOneBy([
            'personne' => $personne,
            'activite' => $activite,
        ]);

        $isEditMode = $request->query->getBoolean('editAvis', false);

        if ($existingAvis && $isEditMode) {
            $avis = $existingAvis;
        } else {
            $avis = new Avis();
            $avis->setActivite($activite);
            $avis->setPersonne($personne);
        }

        $form = $this->createForm(AvisType::class, $avis);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($existingAvis && $isEditMode) {
                $existingAvis->setNote($avis->getNote());
                $existingAvis->setCommentaire($avis->getCommentaire());
                $existingAvis->setDateAvis(new \DateTime());
            } elseif (!$existingAvis) {
                $avis->setDateAvis(new \DateTime());
                $em->persist($avis);
            } else {
                $this->addFlash('danger', 'Vous avez déjà publié un avis pour cette activité. Cliquez sur modifier pour le mettre à jour.');

                return $this->redirectToRoute('activite_show', [
                    'id' => $activite->getId()
                ]);
            }

            $em->flush();

            $avisService->recalculerNoteMoyenne($activite);
            $em->flush();

            $this->addFlash(
                'success',
                ($existingAvis && $isEditMode) ? 'Votre avis a été modifié.' : 'Votre avis a été ajouté.'
            );

            return $this->redirectToRoute('activite_show', [
                'id' => $activite->getId()
            ]);
        }

        $aviss = $avisRepository->findBy(
            ['activite' => $activite],
            ['dateAvis' => 'DESC']
        );

        $distribution = [1 => 0, 2 => 0, 3 => 0, 4 => 0, 5 => 0];
        $totalAvis = count($aviss);

        foreach ($aviss as $item) {
            $note = $item->getNote();
            if (isset($distribution[$note])) {
                $distribution[$note]++;
            }
        }

        return $this->render('activite/show.html.twig', [
            'activite' => $activite,
            'aviss' => $aviss,
            'distribution' => $distribution,
            'totalAvis' => $totalAvis,
            'avisForm' => $form->createView(),
            'userAvis' => $existingAvis,
            'isEditMode' => $isEditMode,
        ]);
    }

    #[Route('/avis/{id}/delete', name: 'avis_delete_front', methods: ['POST'])]
    public function deleteAvisFront(
        Avis $avis,
        Request $request,
        EntityManagerInterface $em,
        PersonneRepository $personneRepository,
        AvisService $avisService
    ): Response
    {
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            $this->addFlash('danger', 'Vous devez être connecté.');
            return $this->redirectToRoute('app_login');
        }

        $personne = $personneRepository->find($userId);
        $activite = $avis->getActivite();

        if (!$activite) {
            $this->addFlash('danger', 'Activité introuvable.');
            return $this->redirectToRoute('activite');
        }

        if (!$personne || $avis->getPersonne()?->getId() !== $personne->getId()) {
            $this->addFlash('danger', 'Vous ne pouvez pas supprimer cet avis.');
            return $this->redirectToRoute('activite_show', [
                'id' => $activite->getId()
            ]);
        }

        if (
            !$this->isCsrfTokenValid(
                'delete_avis_front_' . $avis->getId(),
                $request->request->get('_token')
            )
        ) {
            $this->addFlash('danger', 'Token CSRF invalide.');
            return $this->redirectToRoute('activite_show', [
                'id' => $activite->getId()
            ]);
        }

        $em->remove($avis);
        $em->flush();

        $avisService->recalculerNoteMoyenne($activite);
        $em->flush();

        $this->addFlash('success', 'Votre avis a été supprimé.');

        return $this->redirectToRoute('activite_show', [
            'id' => $activite->getId()
        ]);
    }
    #[Route('/activite/{id}/reserver', name: 'activite_reserver', methods: ['POST'])]
public function reserver(
    Activite $activite,
    Request $request,
    EntityManagerInterface $em,
    PersonneRepository $personneRepository,
    BookingEmailService $bookingEmailService
): Response {
    $userId = $request->getSession()->get('user_id');

    if (!$userId) {
        $this->addFlash('danger', 'Vous devez être connecté pour réserver.');
        return $this->redirectToRoute('app_login');
    }

    $personne = $personneRepository->find($userId);

    if (!$personne) {
        $this->addFlash('danger', 'Utilisateur introuvable.');
        return $this->redirectToRoute('activite');
    }

    $nbTickets = (int) $request->request->get('nb_tickets', 1);

    if ($nbTickets < 1) {
        $this->addFlash('danger', 'Le nombre de tickets doit être au moins 1.');
        return $this->redirectToRoute('activite');
    }

    if ($nbTickets > $activite->getMaxPlaces()) {
        $this->addFlash('danger', 'Pas assez de places disponibles.');
        return $this->redirectToRoute('activite');
    }

    if (!$activite->getDestination()) {
        $this->addFlash('danger', 'Cette activité n’a pas de destination.');
        return $this->redirectToRoute('activite');
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

$reservation->setCoutTotal($unitPrice * $nbTickets);
    $reservation->setNb_tickets($nbTickets);
    $reservation->setPersonne_id($personne);
    $reservation->setDestination($activite->getDestination());

    $em->persist($reservation);

    for ($i = 0; $i < $nbTickets; $i++) {
        $ticket = new Ticket();
        $ticket->setType('Activité');
        $ticket->setStatut('Reservé');
        $ticket->setPrix($unitPrice);
        $ticket->setReservation_id($reservation);
        $ticket->setActivite($activite);
        $ticket->setDestination($activite->getDestination());

        $em->persist($ticket);
    }

    $activite->setMaxPlaces($activite->getMaxPlaces() - $nbTickets);

    

    $em->flush();
    try {
    if ($personne->getEmail()) {
        $bookingEmailService->sendBookingConfirmation(
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
    dd($e->getMessage());
}


    $this->addFlash('success', 'Réservation créée avec succès.');
    return $this->redirectToRoute('activite');
}
private function buildUserProfileText($preferences): string
{
    $budgetMin = null;
    $budgetMax = null;
    $typesVoyage = [];
    $centresInteret = [];

    foreach ($preferences as $preference) {
        if ($preference->getBudgetMin() !== null) {
            $budgetMin = $preference->getBudgetMin();
        }

        if ($preference->getBudgetMax() !== null) {
            $budgetMax = $preference->getBudgetMax();
        }

        if ($preference->getTypesVoyage()) {
            $typesVoyage[] = $preference->getTypesVoyage();
        }

        if ($preference->getCentresInteret()) {
            $centresInteret[] = $preference->getCentresInteret();
        }
    }

    return sprintf(
        "budgetMin: %s\nbudgetMax: %s\ntypesVoyage: %s\ncentresInteret: %s",
        $budgetMin ?? 'not specified',
        $budgetMax ?? 'not specified',
        !empty($typesVoyage) ? implode(', ', $typesVoyage) : 'not specified',
        !empty($centresInteret) ? implode(', ', $centresInteret) : 'not specified'
    );
}
}