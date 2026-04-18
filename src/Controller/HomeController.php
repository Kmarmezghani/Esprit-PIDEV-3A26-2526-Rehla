<?php

namespace App\Controller;

use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use App\Repository\NotificationRepository;
use App\Repository\PersonneRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class HomeController extends AbstractController
{
   #[Route('/home', name: 'home')]
public function index(
    Request $request,
    VilleRepository $villeRepository,
    AttractionRepository $attractionRepository,
    PersonneRepository $personneRepository,
    NotificationRepository $notificationRepository
): Response
{
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

    return $this->render('home/index.html.twig', [
        'villes' => $villeRepository->findBy([], ['visit_count' => 'DESC'], 6),
        'featuredAttractions' => $attractionRepository->findBy([], ['id' => 'DESC'], 6),
        'activityNotifications' => $activityNotifications,
        'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
    ]);
}
}
