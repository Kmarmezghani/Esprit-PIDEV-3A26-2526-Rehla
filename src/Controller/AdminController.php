<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Entity\Avis;
use App\Entity\Commentaire;
use App\Entity\Post;
use App\Form\ActiviteType;
use App\Repository\ActiviteRepository;
use App\Repository\AvisRepository;
use App\Repository\CommentRepository;
use App\Repository\PostRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;
use App\Service\AvisService;
use App\Entity\Notification;
use App\Entity\Personne;

final class AdminController extends AbstractController
{
        #[Route('/admin', name: 'app_admin')]
    public function index(Request $request, EntityManagerInterface $em): Response
    {
        $admin = $this->getConnectedUser($request, $em);
        $notifications = $this->getNotifications($admin, $em);

        return $this->render('admin/base_admin.html.twig', [
            'controller_name' => 'AdminController',
            'notifications' => $notifications
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


        #[Route('/admin/blog', name: 'admin_blog')]
       public function blog( Request $request, EntityManagerInterface $em, PostRepository $postRepository,CommentRepository $commentRepository): Response
   {
    $admin = $this->getConnectedUser($request, $em);
    $notifications = $this->getNotifications($admin, $em);

    return $this->render('admin/blog_admin.html.twig', [
        'posts' => $postRepository->findAll(),
        'comments' => $commentRepository->findAll(),
        'notifications' => $notifications
    ]);
   }



        #[Route('/admin/post/{id}/delete', name: 'admin_post_delete', methods: ['POST'])]
        public function deletePost(Post $post, EntityManagerInterface $em): Response
        {
            $em->remove($post);
            $em->flush();

            return $this->redirectToRoute('admin_blog');
        }

        #[Route('/admin/comment/{id}/delete', name: 'admin_comment_delete', methods: ['POST'])]
        public function deleteComment(Commentaire $comment, EntityManagerInterface $em): Response
        {
            $em->remove($comment);
            $em->flush();

            return $this->redirectToRoute('admin_blog');
        }

            private function getConnectedUser(Request $request, EntityManagerInterface $em): ?Personne
            {
                $userId = $request->getSession()->get('user_id');
                return $em->getRepository(Personne::class)->find($userId);
            }

            private function getNotifications(Personne $admin, EntityManagerInterface $em)
            {
                return $em->getRepository(Notification::class)
                    ->findBy(
                        ['receiver_id' => $admin],
                        ['created_at' => 'DESC']
                    );
            }



               #[Route('/admin/notifications/clear', name: 'admin_notifications_clear')]
            public function clearNotifications(Request $request,EntityManagerInterface $em  ): Response
            {
                $admin = $this->getConnectedUser($request, $em);
                    //supprimer que les notifs ayant le receiver admin ----------
                $notifications = $em->getRepository(Notification::class)
                    ->findBy(['receiver_id' => $admin]);

                foreach ($notifications as $notif) {
                    $em->remove($notif);
                }

                $em->flush();

                return $this->redirectToRoute('app_admin'); // ou la page actuelle
            }



}