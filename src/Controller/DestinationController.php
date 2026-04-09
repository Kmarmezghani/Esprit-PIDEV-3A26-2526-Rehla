<?php

namespace App\Controller;

use App\Entity\Pays;
use App\Entity\Ville;
use App\Repository\PaysRepository;
use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class DestinationController extends AbstractController
{
    #[Route('/destination', name: 'app_destination')]
    public function index(PaysRepository $paysRepository): Response
    {
        $paysList = $paysRepository->findAll();

        return $this->render('destination/index.html.twig', [
            'paysList' => $paysList,
        ]);
    }

    #[Route('/destination/pays/{id}', name: 'app_destination_villes')]
    public function villesByPays(Pays $pays, EntityManagerInterface $entityManager): Response
    {
        // Increment visit count for the country
        $currentCount = $pays->getVisitCount() ?? 0;
        $pays->setVisitCount($currentCount + 1);
        $entityManager->flush();

        return $this->render('destination/villes.html.twig', [
            'pays' => $pays,
            'villes' => $pays->getVilles(),
        ]);
    }

    #[Route('/destination/ville/{id}', name: 'app_destination_attractions')]
    public function attractionsByVille(Ville $ville, EntityManagerInterface $entityManager): Response
    {
        // Increment visit count for the city
        $currentCount = $ville->getVisitCount() ?? 0;
        $ville->setVisitCount($currentCount + 1);
        $entityManager->flush();

        return $this->render('destination/attractions.html.twig', [
            'ville' => $ville,
            'attractions' => $ville->getAttractions(),
        ]);
    }
}
