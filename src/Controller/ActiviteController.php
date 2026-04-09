<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Avis;
use App\Form\AvisType;
use App\Repository\ActiviteRepository;
use App\Repository\AvisRepository;
use App\Repository\PersonneRepository;
use App\Service\AvisService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
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

    #[Route('/activite/{id}', name: 'activite_show')]
public function show(
    Activite $activite,
    Request $request,
    EntityManagerInterface $em,
    AvisRepository $avisRepository,
    PersonneRepository $personneRepository,
    AvisService $avisService
): Response
{
    $personne = $personneRepository->find(1);

    if (!$personne) {
        $this->addFlash('danger', 'Utilisateur introuvable.');
        return $this->redirectToRoute('activite');
    }

    $existingAvis = $avisRepository->findOneBy([
        'personne' => $personne,
        'activite' => $activite,
    ]);

    $avis = $existingAvis ?? new Avis();

    if (!$existingAvis) {
        $avis->setActivite($activite);
        $avis->setPersonne($personne);
    }

    $form = $this->createForm(AvisType::class, $avis);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $avis->setDateAvis(new \DateTime());

        if (!$existingAvis) {
            $em->persist($avis);
        }

        $em->flush();

        $avisService->recalculerNoteMoyenne($activite);
        $em->flush();

        $this->addFlash(
            'success',
            $existingAvis ? 'Votre avis a été modifié.' : 'Votre avis a été ajouté.'
        );

        return $this->redirectToRoute('activite_show', [
            'id' => $activite->getId()
        ]);
    }

    $aviss = $avisRepository->findBy(
        ['activite' => $activite],
        ['dateAvis' => 'DESC']
    );

    $distribution = [1 => 0, 2 => 0, 3 => 0, 4 => 0, 5 => 0];
    $totalAvis = count($aviss);

    foreach ($aviss as $item) {
        $note = $item->getNote();
        if (isset($distribution[$note])) {
            $distribution[$note]++;
        }
    }

    return $this->render('activite/show.html.twig', [
        'activite' => $activite,
        'aviss' => $aviss,
        'distribution' => $distribution,
        'totalAvis' => $totalAvis,
        'avisForm' => $form->createView(),
        'userAvis' => $existingAvis,
    ]);
}
    #[Route('/avis/{id}/delete', name: 'avis_delete_front', methods: ['POST'])]
    public function deleteAvisFront(
        Avis $avis,
        Request $request,
        EntityManagerInterface $em,
        PersonneRepository $personneRepository,
        AvisService $avisService
    ): Response
    {
        $personne = $personneRepository->find(1);

        $activite = $avis->getActivite();

        if (!$activite) {
            $this->addFlash('danger', 'Activité introuvable.');
            return $this->redirectToRoute('activite');
        }

        if (!$personne || $avis->getPersonne()?->getId() !== $personne->getId()) {
            $this->addFlash('danger', 'Vous ne pouvez pas supprimer cet avis.');
            return $this->redirectToRoute('activite_show', [
                'id' => $activite->getId()
            ]);
        }

        if (!$this->isCsrfTokenValid(
            'delete_avis_front_' . $avis->getId(),
            $request->request->get('_token')
        )) {
            $this->addFlash('danger', 'Token CSRF invalide.');
            return $this->redirectToRoute('activite_show', [
                'id' => $activite->getId()
            ]);
        }

        $em->remove($avis);
        $em->flush();

        $avisService->recalculerNoteMoyenne($activite);
        $em->flush();

        $this->addFlash('success', 'Votre avis a été supprimé.');

        return $this->redirectToRoute('activite_show', [
            'id' => $activite->getId()
        ]);
    }
}