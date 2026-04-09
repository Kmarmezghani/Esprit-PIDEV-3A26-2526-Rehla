<?php

namespace App\Controller;

use App\Entity\Pays;
use App\Entity\Ville;
use App\Repository\PaysRepository;
use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
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
    public function villesByPays(Pays $pays): Response
    {
        return $this->render('destination/villes.html.twig', [
            'pays' => $pays,
            'villes' => $pays->getVilles(),
        ]);
    }

    #[Route('/destination/ville/{id}', name: 'app_destination_attractions')]
    public function attractionsByVille(Ville $ville): Response
    {
        return $this->render('destination/attractions.html.twig', [
            'ville' => $ville,
            'attractions' => $ville->getAttractions(),
        ]);
    }
}
