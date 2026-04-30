<?php

namespace App\Controller;

use App\Entity\Pays;
use App\Entity\Ville;
use App\Repository\PaysRepository;
use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class DestinationController extends AbstractController
{
    #[Route('/destination', name: 'app_destination')]
    public function index(
        Request $request,
        PaysRepository $paysRepository,
        PaginatorInterface $paginator
    ): Response {
        $search = $request->query->get('search', '');

        $qb = $paysRepository->createQueryBuilder('p');

        if (!empty($search)) {
            $qb->where('p.nom LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        $qb->orderBy('p.nom', 'ASC');

        $paysList = $paginator->paginate(
            $qb->getQuery(),
            $request->query->getInt('page', 1),
            6
        );

        return $this->render('destination/index.html.twig', [
            'paysList' => $paysList,
            'search' => $search,
        ]);
    }

    #[Route('/destination/pays/{id}', name: 'app_destination_villes')]
    public function villesByPays(
        Pays $pays,
        Request $request,
        EntityManagerInterface $entityManager,
        PaginatorInterface $paginator
    ): Response {
        // Increment visit count for the country
        $currentCount = $pays->getVisitCount() ?? 0;
        $pays->setVisitCount($currentCount + 1);
        $entityManager->flush();

        $query = $entityManager->getRepository(Ville::class)
            ->createQueryBuilder('v')
            ->where('v.pays_id = :pays')
            ->setParameter('pays', $pays)
            ->orderBy('v.nom', 'ASC')
            ->getQuery();

        $villes = $paginator->paginate(
            $query,
            $request->query->getInt('page', 1),
            6
        );

        return $this->render('destination/villes.html.twig', [
            'pays'   => $pays,
            'villes' => $villes,
        ]);
    }

    #[Route('/destination/ville/{id}', name: 'app_destination_attractions')]
    public function attractionsByVille(
        Ville $ville,
        Request $request,
        EntityManagerInterface $entityManager,
        PaginatorInterface $paginator
    ): Response {
        // Increment visit count for the city
        $currentCount = $ville->getVisitCount() ?? 0;
        $ville->setVisitCount($currentCount + 1);
        $entityManager->flush();

        $query = $entityManager->getRepository(\App\Entity\Attraction::class)
            ->createQueryBuilder('a')
            ->where('a.ville_id = :ville')
            ->setParameter('ville', $ville)
            ->orderBy('a.nom', 'ASC')
            ->getQuery();

        $attractions = $paginator->paginate(
            $query,
            $request->query->getInt('page', 1),
            9
        );

        return $this->render('destination/attractions.html.twig', [
            'ville'       => $ville,
            'attractions' => $attractions,
        ]);
    }
}
