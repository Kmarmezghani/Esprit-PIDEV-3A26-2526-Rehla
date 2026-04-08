<?php

namespace App\Controller;

use App\Repository\ActiviteRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class ActiviteController extends AbstractController
{
    #[Route('/activite', name: 'activite')]
    public function index(ActiviteRepository $activiteRepository): Response
    {
        $activites = $activiteRepository->findBy([
            'status' => 'DISPONIBLE'
        ]);

        return $this->render('activite/activite.html.twig', [
            'activites' => $activites
        ]);
    }
}