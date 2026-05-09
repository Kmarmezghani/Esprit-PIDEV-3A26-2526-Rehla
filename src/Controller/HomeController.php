<?php

namespace App\Controller;

use App\Repository\PaysRepository;
use App\Repository\AttractionRepository;
use App\Repository\NotificationRepository;
use App\Repository\PersonneRepository;
use App\Repository\AvisRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class HomeController extends AbstractController
{
    #[Route('/home', name: 'home')]
    public function index(
        Request $request,
        PaysRepository $paysRepository,
        AttractionRepository $attractionRepository,
        PersonneRepository $personneRepository,
        NotificationRepository $notificationRepository,
        AvisRepository $avisRepository
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

        $avisList = $avisRepository->findBy([], ['id' => 'DESC'], 6);

        // Top destinations ranked by composite scoring algorithm
        $topDestinations = $paysRepository->findTopDestinations(6);

        return $this->render('home/index.html.twig', [
            'topDestinations'                => $topDestinations,
            'featuredAttractions'            => $attractionRepository->findBy([], ['id' => 'DESC'], 6),
            'activityNotifications'          => $activityNotifications,
            'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
            'avisList'                       => $avisList,
        ]);
    }
}
