<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Attraction;
use App\Form\ActiviteType;
use App\Repository\ActiviteRepository;
use App\Repository\AttractionRepository;
use App\Repository\GuideRepository;
use App\Repository\NotificationRepository;
use App\Repository\PersonneRepository;
use App\Service\AiDescriptionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\HttpFoundation\JsonResponse;

final class GuideController extends AbstractController
{
    #[Route('/guide/mes-activites', name: 'mes_activites')]
public function mesActivites(
    Request $request,
PersonneRepository $personneRepository,
NotificationRepository $notificationRepository,
    ActiviteRepository $activiteRepository,
    GuideRepository $guideRepository
): Response
{
    $guideId = $request->getSession()->get('user_id');

    if (!$guideId) {
        $this->addFlash('danger', 'Vous devez être connecté.');
        return $this->redirectToRoute('app_login');
    }

    $guide = $guideRepository->find($guideId);

    if (!$guide) {
        throw $this->createNotFoundException('Guide introuvable.');
    }

   $activites = $activiteRepository->findBy(
    ['guide' => $guide],
    ['date_debut' => 'DESC']
);
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

    return $this->render('guide/mes_activites.html.twig', [
        'activites' => $activites,
        'activityNotifications' => $activityNotifications,
'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
    ]);
}
    #[Route('/guide/activite/modifier/{id}', name: 'modifier_activite')]
public function modifier(
    Request $request,
PersonneRepository $personneRepository,
NotificationRepository $notificationRepository,
    Activite $activite,
    EntityManagerInterface $em,
    SluggerInterface $slugger
): Response
{
    $form = $this->createForm(ActiviteType::class, $activite, [
    'is_edit' => true,
    'show_max_places' => true,
]);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {

        $imageFile = $form->get('image')->getData();

        if ($imageFile) {
            $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
            $safeFilename = $slugger->slug($originalFilename);
            $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

            $destinationPath = $this->getParameter('activities_directory');

            try {
                $imageFile->move($destinationPath, $newFilename);
            } catch (\Exception $e) {
                $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image.');
            }

            $physicalPath = $destinationPath . '\\' . $newFilename;
            $activite->setImage($physicalPath);
        }

        $em->flush();

        $this->addFlash('success', 'Activité modifiée avec succès !');

        return $this->redirectToRoute('mes_activites');
    }
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

    return $this->render('guide/modifier_activite.html.twig', [
        'form' => $form->createView(),
        'activite' => $activite,
        'activityNotifications' => $activityNotifications,
'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
    ]);
}
#[Route('/guide/activite/supprimer/{id}', name: 'supprimer_activite')]
public function supprimer(Activite $activite, EntityManagerInterface $em): Response
{
    $em->remove($activite);
    $em->flush();

    $this->addFlash('success', 'Activité supprimée avec succès !');

    return $this->redirectToRoute('mes_activites');
}
#[Route('/guide/activite/ajouter', name: 'ajouter_activite')]
public function ajouter(
    Request $request,
PersonneRepository $personneRepository,
NotificationRepository $notificationRepository,
    EntityManagerInterface $em,
    SluggerInterface $slugger,
    GuideRepository $guideRepository
): Response
{
    $activite = new Activite();

   $form = $this->createForm(ActiviteType::class, $activite, [
    'is_edit' => false,
    'show_max_places' => true,
]);

    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $guideId = $request->getSession()->get('user_id');
        $guide = $guideRepository->find($guideId);

        if (!$guide) {
            $this->addFlash('danger', 'Vous devez être connecté en tant que guide pour ajouter une activité.');
            return $this->redirectToRoute('app_login');
        }

        $activite->setGuide($guide);

        $imageFile = $form->get('image')->getData();

        if ($imageFile) {
            $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
            $safeFilename = $slugger->slug($originalFilename);
            $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

            $destinationPath = $this->getParameter('activities_directory');

            try {
                $imageFile->move($destinationPath, $newFilename);
            } catch (\Exception $e) {
                $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image.');
            }

            $activite->setImage($destinationPath . '\\' . $newFilename);
        }

        $em->persist($activite);
        $em->flush();

        $this->addFlash('success', 'Activité ajoutée avec succès !');

        return $this->redirectToRoute('mes_activites');
    }
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


    return $this->render('guide/ajouter_activite.html.twig', [
        'form' => $form->createView(),
        'activite' => $activite,
        'activityNotifications' => $activityNotifications,
'hasUnreadActivityNotifications' => $hasUnreadActivityNotifications,
    ]);
}
#[Route('/guide/activite/generate-description', name: 'guide_generate_description', methods: ['POST'])]
public function generateDescription(
    Request $request,
    AiDescriptionService $aiDescriptionService,
    AttractionRepository $attractionRepository
): JsonResponse {
    $data = json_decode($request->getContent(), true);

    $nom = trim($data['nom'] ?? '');
    $type = trim($data['typeActivite'] ?? '');
    $destination = trim($data['destination'] ?? '');
    $duration = trim($data['duration'] ?? '');

    if ($nom === '' || $type === '' || $destination === '') {
        return $this->json([
            'success' => false,
            'message' => 'Veuillez remplir au moins le nom, le type et la destination avant de générer la description.'
        ], 400);
    }

    try {
        $attractions = $attractionRepository->createQueryBuilder('a')
            ->join('a.ville_id', 'v')
            ->where('v.id = :villeId')
            ->setParameter('villeId', (int) $destination)
            ->getQuery()
            ->getResult();

        $attractionData = array_map(
            fn($attraction) => [
                'nom' => $attraction->getNom(),
                'type' => $attraction->getType(),
                'description' => $attraction->getDescription(),
            ],
            $attractions
        );

        $description = $aiDescriptionService->generate(
            $nom,
            $type,
            $destination,
            $duration,
            $attractionData
        );

        return $this->json([
            'success' => true,
            'description' => $description,
            'attractions_used' => $attractionData
        ]);
    } catch (\Throwable $e) {
        return $this->json([
            'success' => false,
            'message' => 'La génération de la description a échoué.',
            'error' => $e->getMessage(),
        ], 500);
    }
}
}