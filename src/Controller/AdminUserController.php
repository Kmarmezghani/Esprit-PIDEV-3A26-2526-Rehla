<?php

namespace App\Controller;

use App\Entity\Personne;
use App\Entity\Preference;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class AdminUserController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  Helper : vérifie que l'administrateur est connecté
    // ─────────────────────────────────────────────────────────────
    private function requireAdmin(Request $request): ?Response
    {
        $session = $request->getSession();
        if (!$session->get('user_id') || $session->get('user_role') !== 'ADMIN') {
            return $this->redirectToRoute('app_login');
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  LISTE DES UTILISATEURS
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs', name: 'admin_users')]
    public function list(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        $users = $em->getRepository(Personne::class)->findAll();

        return $this->render('admin/users_admin.html.twig', [
            'users' => $users,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MODIFIER UN UTILISATEUR (admin)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/modifier', name: 'admin_user_edit')]
    public function edit(int $id, Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        if ($request->isMethod('POST')) {
            $role   = $request->request->get('role', 'CLIENT');
            $statut = $request->request->get('statutCompte', 'ACTIF');

            $personne->setRole($role);
            $personne->setStatutCompte($statut);
            $em->flush();

            $this->addFlash('success', 'Rôle et statut mis à jour avec succès !');
            return $this->redirectToRoute('admin_users');
        }

        return $this->render('admin/user_edit.html.twig', [
            'personne' => $personne,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  SUPPRIMER UN UTILISATEUR
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/supprimer', name: 'admin_user_delete', methods: ['POST'])]
    public function delete(int $id, Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        // Empêcher l'admin de se supprimer lui-même
        if ($personne->getId() === $request->getSession()->get('user_id')) {
            $this->addFlash('danger', 'Vous ne pouvez pas supprimer votre propre compte administrateur.');
            return $this->redirectToRoute('admin_users');
        }

        $em->remove($personne);
        $em->flush();

        $this->addFlash('success', 'Utilisateur supprimé avec succès.');
        return $this->redirectToRoute('admin_users');
    }

    // ─────────────────────────────────────────────────────────────
    //  VOIR LES PRÉFÉRENCES D'UN UTILISATEUR (lecture seule)
    // ─────────────────────────────────────────────────────────────
    #[Route('/admin/utilisateurs/{id}/preferences', name: 'admin_user_preferences')]
    public function viewPreferences(int $id, Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireAdmin($request);
        if ($redirect) return $redirect;

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($id);
        if (!$personne) {
            $this->addFlash('danger', 'Utilisateur introuvable.');
            return $this->redirectToRoute('admin_users');
        }

        /** @var Preference|null $preference */
        $preference = $em->getRepository(Preference::class)->findOneBy(['personne_id' => $personne]);

        return $this->render('admin/user_preferences_show.html.twig', [
            'personne'   => $personne,
            'preference' => $preference,
        ]);
    }
}
