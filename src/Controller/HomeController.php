<?php

namespace App\Controller;

use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class HomeController extends AbstractController
{
    #[Route('/home', name: 'home')]
    public function index(VilleRepository $villeRepository, AttractionRepository $attractionRepository): Response
    {
        return $this->render('home/index.html.twig', [
            'villes' => $villeRepository->findBy([], ['visit_count' => 'DESC'], 6),
            'featuredAttractions' => $attractionRepository->findBy([], ['id' => 'DESC'], 6),
        ]);
    }
}
