<?php

namespace App\Controller;

use App\Entity\Pays;
use App\Entity\Ville;
use App\Entity\Attraction;
use App\Form\PaysType;
use App\Form\VilleType;
use App\Form\AttractionType;
use App\Repository\PaysRepository;
use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/destination')]
class AdminDestinationController extends AbstractController
{
    #[Route('', name: 'admin_destination_index')]
    public function index(
        Request $request,
        PaysRepository $paysRepo,
        VilleRepository $villeRepo,
        AttractionRepository $attractionRepo
    ): Response {
        $tab = $request->query->get('tab', 'pays');
        $search = $request->query->get('search');
        $sort = $request->query->get('sort', 'nom');
        $direction = $request->query->get('direction', 'asc');

        return $this->render('admin/destination_admin.html.twig', [
            'pays' => ($tab === 'pays') ? $paysRepo->searchAndSort($search, $sort, $direction) : $paysRepo->searchAndSort(null, 'nom', 'asc'),
            'villes' => ($tab === 'ville') ? $villeRepo->searchAndSort($search, $sort, $direction) : $villeRepo->searchAndSort(null, 'nom', 'asc'),
            'attractions' => ($tab === 'attraction') ? $attractionRepo->searchAndSort($search, $sort, $direction) : $attractionRepo->searchAndSort(null, 'nom', 'asc'),
            'selectedTab' => $tab,
            'search' => $search,
            'sort' => $sort,
            'direction' => $direction,
        ]);
    }

    // --- PAYS ACTIONS ---
    #[Route('/pays/new', name: 'admin_pays_new')]
    #[Route('/pays/{id}/edit', name: 'admin_pays_edit')]
    public function paysForm(Request $request, EntityManagerInterface $em, ?Pays $pays = null): Response
    {
        $pays = $pays ?? new Pays();
        $form = $this->createForm(PaysType::class, $pays);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($pays);
            $em->flush();
            $this->addFlash('success', 'Pays enregistré avec succès !');
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'pays']);
        }

        return $this->render('admin/destination_form.html.twig', [
            'form' => $form->createView(),
            'title' => $pays->getId() ? 'Modifier Pays' : 'Nouveau Pays',
            'entity' => 'pays'
        ]);
    }

    #[Route('/pays/{id}/delete', name: 'admin_pays_delete', methods: ['POST'])]
    public function paysDelete(Request $request, Pays $pays, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $pays->getId(), $request->request->get('_token'))) {
            $em->remove($pays);
            $em->flush();
            $this->addFlash('success', 'Pays supprimé !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'pays']);
    }

    // --- VILLE ACTIONS ---
    #[Route('/ville/new', name: 'admin_ville_new')]
    #[Route('/ville/{id}/edit', name: 'admin_ville_edit')]
    public function villeForm(Request $request, EntityManagerInterface $em, ?Ville $ville = null): Response
    {
        $ville = $ville ?? new Ville();
        $form = $this->createForm(VilleType::class, $ville);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($ville);
            $em->flush();
            $this->addFlash('success', 'Ville enregistrée !');
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'ville']);
        }

        return $this->render('admin/destination_form.html.twig', [
            'form' => $form->createView(),
            'title' => $ville->getId() ? 'Modifier Ville' : 'Nouvelle Ville',
            'entity' => 'ville'
        ]);
    }

    #[Route('/ville/{id}/delete', name: 'admin_ville_delete', methods: ['POST'])]
    public function villeDelete(Request $request, Ville $ville, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $ville->getId(), $request->request->get('_token'))) {
            $em->remove($ville);
            $em->flush();
            $this->addFlash('success', 'Ville supprimée !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'ville']);
    }

    // --- ATTRACTION ACTIONS ---
    #[Route('/attraction/new', name: 'admin_attraction_new')]
    #[Route('/attraction/{id}/edit', name: 'admin_attraction_edit')]
    public function attractionForm(Request $request, EntityManagerInterface $em, ?Attraction $attraction = null): Response
    {
        $attraction = $attraction ?? new Attraction();
        $form = $this->createForm(AttractionType::class, $attraction);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($attraction);
            $em->flush();
            $this->addFlash('success', 'Attraction enregistrée !');
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'attraction']);
        }

        return $this->render('admin/destination_form.html.twig', [
            'form' => $form->createView(),
            'title' => $attraction->getId() ? 'Modifier Attraction' : 'Nouvelle Attraction',
            'entity' => 'attraction'
        ]);
    }

    #[Route('/attraction/{id}/delete', name: 'admin_attraction_delete', methods: ['POST'])]
    public function attractionDelete(Request $request, Attraction $attraction, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $attraction->getId(), $request->request->get('_token'))) {
            $em->remove($attraction);
            $em->flush();
            $this->addFlash('success', 'Attraction supprimée !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'attraction']);
    }
}
