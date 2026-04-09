<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Avis;
use App\Form\ActiviteType;
use App\Repository\ActiviteRepository;
use App\Repository\AvisRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;
use App\Service\AvisService;

final class AdminController extends AbstractController
{
    #[Route('/admin', name: 'app_admin')]
    public function index(): Response
    {
        return $this->render('admin/base_admin.html.twig', [
            'controller_name' => 'AdminController',
        ]);
    }
#[Route('/admin/activite', name: 'activite_admin')]
public function activiteList(
    Request $request,
    ActiviteRepository $activiteRepository,
    AvisRepository $avisRepository
): Response
{
    $tab = $request->query->get('tab', 'activite');
    $activiteId = $request->query->get('activiteId');

    $search = trim((string) $request->query->get('search', ''));
    $sort = (string) $request->query->get('sort', 'date_debut');
    $direction = strtolower((string) $request->query->get('direction', 'desc'));

    $allowedSorts = ['nom', 'prix', 'noteMoyenne', 'date_debut', 'status'];
    if (!in_array($sort, $allowedSorts, true)) {
        $sort = 'date_debut';
    }

    $direction = $direction === 'asc' ? 'asc' : 'desc';

    $activites = $activiteRepository->searchAndSortAdmin($search, $sort, $direction);

    $aviss = [];
    $selectedActivite = null;

    if ($activiteId) {
        $selectedActivite = $activiteRepository->find($activiteId);

        if ($selectedActivite) {
            $aviss = $avisRepository->findBy(
                ['activite' => $selectedActivite],
                ['dateAvis' => 'DESC']
            );
        }
    }

    return $this->render('admin/activite_admin.html.twig', [
        'activites' => $activites,
        'aviss' => $aviss,
        'selectedTab' => $tab,
        'selectedActivite' => $selectedActivite,
        'search' => $search,
        'sort' => $sort,
        'direction' => $direction,
    ]);
}
#[Route('/admin/activite/search', name: 'activite_admin_search', methods: ['GET'])]
public function activiteSearch(
    Request $request,
    ActiviteRepository $activiteRepository
): Response
{
    $search = trim((string) $request->query->get('search', ''));
    $sort = (string) $request->query->get('sort', 'date_debut');
    $direction = strtolower((string) $request->query->get('direction', 'desc'));

    $allowedSorts = ['nom', 'prix', 'noteMoyenne', 'date_debut', 'status'];
    if (!in_array($sort, $allowedSorts, true)) {
        $sort = 'date_debut';
    }

    $direction = $direction === 'asc' ? 'asc' : 'desc';

    $activites = $activiteRepository->searchAndSortAdmin($search, $sort, $direction);

    return $this->render('admin/_activite_rows.html.twig', [
        'activites' => $activites,
    ]);
}

    #[Route('/admin/activite/new', name: 'activite_new')]
    public function activiteNew(
        Request $request,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response
    {
        $activite = new Activite();

        $form = $this->createForm(ActiviteType::class, $activite, [
            'is_edit' => false,
            'show_max_places' => false,
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
                    $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image');
                }

                $physicalPath = $destinationPath . '\\' . $newFilename;
                $activite->setImage($physicalPath);
            }

            $em->persist($activite);
            $em->flush();

            $this->addFlash('success', 'Activité créée avec succès !');

            return $this->redirectToRoute('activite_admin');
        }

        return $this->render('admin/activite_new.html.twig', [
            'form' => $form->createView()
        ]);
    }

    #[Route('/admin/activite/{id}/edit', name: 'activite_edit')]
    public function edit(
        Request $request,
        Activite $activite,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response
    {
        $form = $this->createForm(ActiviteType::class, $activite, [
            'is_edit' => true,
            'show_max_places' => false,
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
                    $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image');
                }

                $physicalPath = $destinationPath . '\\' . $newFilename;
                $activite->setImage($physicalPath);
            }

            $em->flush();

            $this->addFlash('success', 'Activité modifiée avec succès !');

            return $this->redirectToRoute('activite_admin');
        }

        return $this->render('admin/activite_edit.html.twig', [
            'form' => $form->createView(),
            'activite' => $activite
        ]);
    }

    #[Route('/admin/activite/{id}/delete', name: 'activite_delete', methods: ['POST'])]
    public function delete(
        Request $request,
        Activite $activite,
        EntityManagerInterface $em
    ): Response
    {
        if ($this->isCsrfTokenValid('delete' . $activite->getId(), $request->request->get('_token'))) {
            $em->remove($activite);
            $em->flush();

            $this->addFlash('success', 'Activité supprimée avec succès !');
        }

        return $this->redirectToRoute('activite_admin');
    }

    #[Route('/admin/avis/{id}/delete', name: 'avis_delete', methods: ['POST'])]
    public function deleteAvis(
    Request $request,
    Avis $avis,
    EntityManagerInterface $em,
    AvisService $avisService
): Response
{
    $activite = $avis->getActivite();
    $activiteId = $activite ? $activite->getId() : null;

    if ($this->isCsrfTokenValid('delete_avis' . $avis->getId(), $request->request->get('_token'))) {

        if ($activite) {
            $activite->getAviss()->removeElement($avis);
        }

        $em->remove($avis);

        if ($activite) {
            $avisService->recalculerNoteMoyenne($activite);
        }

        $em->flush();

        $this->addFlash('success', 'Avis supprimé avec succès !');
    }

    return $this->redirectToRoute('activite_admin', [
        'tab' => 'avis',
        'activiteId' => $activiteId,
    ]);
}
}