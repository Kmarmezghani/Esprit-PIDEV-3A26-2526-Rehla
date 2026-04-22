<?php

namespace App\Controller;

use App\Service\CircuitOptimizer;
use App\Repository\VilleRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

class CircuitController extends AbstractController
{
    #[Route('/destination/planifier', name: 'app_circuit_planner')]
    public function index(Request $request, VilleRepository $villeRepository): Response
    {
        $paysId = $request->query->get('paysId');

        // Get unique tourism types for the dropdown
        $dbTypes = $villeRepository->createQueryBuilder('v')
            ->select('DISTINCT v.typeTourisme')
            ->where('v.typeTourisme IS NOT NULL')
            ->getQuery()
            ->getResult();
            
        $types = array_filter(array_column($dbTypes, 'typeTourisme'));
        
        // Add defaults if DB is empty to ensure user can always select something
        if (empty($types)) {
            $types = ['Culturel', 'Aventure', 'Détente', 'Gastronomie'];
        }

        return $this->render('circuit/index.html.twig', [
            'tourismTypes' => $types,
            'preselectedPaysId' => $paysId
        ]);
    }

    #[Route('/api/circuit/generate', name: 'api_circuit_generate', methods: ['POST'])]
    public function generate(Request $request, CircuitOptimizer $optimizer): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        
        $budget = (float)($data['budget'] ?? 500);
        $type = (string)($data['type'] ?? '');
        $paysId = isset($data['paysId']) ? (int)$data['paysId'] : null;

        $result = $optimizer->generateCircuit($budget, $type, $paysId);

        return $this->json($result);
    }
}
