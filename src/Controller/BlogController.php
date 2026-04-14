<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use App\Entity\Post;
use App\Entity\Personne;
use App\Entity\Commentaire;
use Doctrine\ORM\EntityManagerInterface;
use App\Form\PostType;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\JsonResponse;
use App\Entity\Likes;
use App\Service\ImageUploader;
use App\Service\FlaskClient\ToxicityChecker;
use App\Entity\Notification;
use App\Service\FlaskClient\GeoLocalisationService;
final class BlogController extends AbstractController

{

#[Route('/blog', name: 'blog')]
public function index(Request $request, EntityManagerInterface $em, ImageUploader $uploader, ToxicityChecker $toxicityChecker): Response
{
    $personne = $this->getConnectedUser($request, $em);

    $post = new Post();
    $form = $this->createForm(PostType::class, $post);
    $form->handleRequest($request);

   $status = $this->handlePostCreation(
    $form,
    $post,
    $personne,
    $uploader,
    $em,
    $toxicityChecker
);

if ($status === 'refused') {
    $this->addFlash('error', '❌ Contenu refusé (toxique)');
    return $this->redirectToRoute('blog');
}

if ($status === 'warning') {
    $this->addFlash('warning', '⚠️ Contenu sensible publié (admin notifié)');
    return $this->redirectToRoute('blog');
}

if ($status === 'ok') {
    $this->addFlash('success', '✅ Publication ajoutée');
    return $this->redirectToRoute('blog');
}

    $posts = $this->getFilteredPosts($request, $em);

    return $this->render('blog/blog.html.twig', [
        'posts' => $posts,
        'form' => $form->createView()
    ]);
}


private function getConnectedUser(Request $request, EntityManagerInterface $em): ?Personne
{
    $userId = $request->getSession()->get('user_id');
    return $em->getRepository(Personne::class)->find($userId);
}
private function handlePostCreation( $form, Post $post, $personne, ImageUploader $uploader, EntityManagerInterface $em, ToxicityChecker $toxicityChecker ): ?string
{
    if (!$form->isSubmitted() || !$form->isValid()) {
        return null;
    }

    $contenu = $post->getContenu();

    $result = $toxicityChecker->check($contenu);
    $score = $result['score'];

    if ($score > 0.7) {
        return 'refused';
    }

    $status = ($score > 0.1) ? 'warning' : 'ok';


    $imageFile = $form->get('image')->getData();
    $imagePath = $uploader->upload($imageFile, $this->getParameter('images_directory'));

    if ($imagePath) {
        $post->setImage($imagePath);
    }

    $post->setDatePublication(new \DateTime());
    $post->setPopularite(0);
    $post->setPersonne_id($personne);

    $em->persist($post);
    $em->flush();
    if ($status === 'warning') {

    // récupérer tous les admins
    $admins = $em->getRepository(Personne::class)
                 ->findBy(['role' => 'ADMIN']); 

    foreach ($admins as $admin) {

        $notification = new Notification();

        $message = $personne->getNom() . ' ' . $personne->getPrenom()
            . ' a publié un post jugée suspect (score: ' . round($score, 2) . ') | Post ID: ' . $post->getId();

        $notification->setMessage($message);
        $notification->setType('POST');
        $notification->setPost_id($post); 
        $notification->setSender_id($personne);
        $notification->setReceiver_id($admin);
        $notification->setIs_read(false);
        $notification->setCreated_at(new \DateTime());
        $notification->setIs_sent_sms(false);

        $em->persist($notification);
    }

    $em->flush();
}

    return $status;
}

#[Route('/check-toxicity', name: 'check_toxicity', methods: ['POST'])]
public function checkToxicity(Request $request, ToxicityChecker $toxicityChecker): JsonResponse
{
    $data = json_decode($request->getContent(), true);

    $text = $data['text'] ?? '';

    if (!$text) {
        return new JsonResponse(['error' => 'empty text'], 400);
    }

    try {
        $result = $toxicityChecker->check($text);

        return new JsonResponse([
            'score' => $result['score'],
            'label' => $result['label'] ?? null
        ]);

    } catch (\Exception $e) {
        return new JsonResponse([
            'error' => 'Flask error'
        ], 500);
    }
}
private function getFilteredPosts(Request $request, EntityManagerInterface $em)
{
    $search = $request->query->get('search');
    $likes = $request->query->get('likes');
    $date = $request->query->get('date');
    $dateExact = $request->query->get('date_exact');
    $sort = $request->query->get('sort');
    $order = $request->query->get('order');

    $repo = $em->getRepository(Post::class);

    if ($search) {
        return $repo->searchByContent($search);
    }

    if ($likes) {
        return $repo->findByLikes($likes);
    }

    if ($dateExact) {
        return $repo->findByExactDate($dateExact);
    }

    if ($date) {
        return $repo->findByDateFilter($date);
    }

    if ($sort && $order) {
        return $repo->findSorted($sort, $order);
    }

    return $repo->findLatestPosts();
}

#[Route('/post/delete/{id}', name: 'post_delete', methods: ['POST'])]
public function delete(Post $post, EntityManagerInterface $em): Response
{
    $em->remove($post);
    $em->flush();

    return $this->redirectToRoute('blog');
}
#[Route('/post/edit/{id}', name: 'post_edit', methods: ['POST'])]
public function edit(Request $request, Post $post, EntityManagerInterface $em): Response
{
    $post->setContenu($request->request->get('contenu'));

    $imageFile = $request->files->get('image');
    if ($imageFile) {
        $newFilename = uniqid().'.'.$imageFile->guessExtension();
        $imageFile->move($this->getParameter('images_directory'), $newFilename);
        $post->setImage('uploads/'.$newFilename);
    }

    $em->flush();

    return $this->redirectToRoute('blog');
}


#[Route('/comment/add/{id}', name: 'comment_add', methods: ['POST'])]
public function addComment(
    Request $request, 
    Post $post, 
    EntityManagerInterface $em,
    ToxicityChecker $toxicityChecker
) {
    $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    $contenu = trim($request->request->get('contenu'));

    if (!$contenu) {
        return $this->redirectToRoute('blog');
    }

    // 🔥 Analyse toxicité
    $result = $toxicityChecker->check($contenu);
    $score = $result['score'];

    // ❌ REFUS
    if ($score > 0.7) {
        $this->addFlash('error', '❌ Commentaire refusé (toxique)');
        return $this->redirectToRoute('blog');
    }

    $status = ($score > 0.1) ? 'warning' : 'ok';

    // ✅ Création commentaire
    $comment = new Commentaire();
    $comment->setContenu($contenu);
    $comment->setDateCommentaire(new \DateTime());
    $comment->setPost_id($post);
    $comment->setPersonne_id($personne);

    $em->persist($comment);
    $em->flush();

    // ⚠️ Notification admin si warning
    if ($status === 'warning') {

        $admins = $em->getRepository(Personne::class)
                     ->findBy(['role' => 'ADMIN']);

        foreach ($admins as $admin) {

            $notification = new Notification();

            $message = $personne->getNom() . ' ' . $personne->getPrenom()
                . ' a ajouté  un commentaire jugée suspect (score: ' . round($score, 2) . ')'
                . ' | Comment ID: ' . $comment->getId();

            $notification->setMessage($message);
            $notification->setType('COMMENT');
            $notification->setComment_id($comment); 
            $notification->setPost_id($post);
            $notification->setSender_id($personne);
            $notification->setReceiver_id($admin);
            $notification->setIs_read(false);
            $notification->setCreated_at(new \DateTime());
            $notification->setIs_sent_sms(false);

            $em->persist($notification);
        }

        $em->flush();
    }

    // 🔥 Flash message UX
    if ($status === 'warning') {
        $this->addFlash('warning', '⚠️ Commentaire sensible publié');
    } else {
        $this->addFlash('success', '✅ Commentaire ajouté');
    }

    return $this->redirectToRoute('blog');
}
#[Route('/comment/delete/{id}', name: 'comment_delete', methods: ['POST'])]
public function deleteComment(Commentaire $comment, EntityManagerInterface $em): Response
{
    $em->remove($comment);
    $em->flush();

    return $this->redirectToRoute('blog');
}


#[Route('/comment/edit/{id}', name: 'comment_edit', methods: ['POST'])]
public function editComment(Request $request, Commentaire $comment, EntityManagerInterface $em)
{
    $contenu = $request->request->get('contenu');

    if (!$contenu) {
        return new JsonResponse(['error' => 'vide'], 400);
    }

    $comment->setContenu($contenu);
    $em->flush();

    return new JsonResponse([
        'contenu' => $contenu
    ]);
}

#[Route('/post/like/{id}', name: 'post_like', methods: ['POST'])]
public function like(Post $post, EntityManagerInterface $em, Request $request): JsonResponse
{
    $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    if (!$personne) {
        return new JsonResponse(['error' => 'user not connected'], 403);
    }

    $likeRepo = $em->getRepository(Likes::class);

    
    $existing = $likeRepo->findOneBy([
        'personne_id' => $personne,
        'post_id' => $post
    ]);

    if ($existing) {
        $em->remove($existing);
        $em->flush();

        return new JsonResponse([
            'liked' => false,
            'count' => $likeRepo->count(['post_id' => $post])
        ]);
    }

    $like = new Likes();
    $like->setPersonne_id($personne);
    $like->setPost_id($post);
    $like->setStatut(true);

    $em->persist($like);
    $em->flush();

    return new JsonResponse([
        'liked' => true,
        'count' => $likeRepo->count(['post_id' => $post])
    ]);
}


#[Route('/localiser-image', name: 'localiser_image', methods: ['POST'])]
    public function localiserImage(
        Request $request,
        GeoLocalisationService $geoService
    ): JsonResponse {

        $file = $request->files->get('image');

        if (!$file) {
            return $this->json([
                'error' => 'Aucune image envoyée'
            ], 400);
        }

        try {
            $result = $geoService->localize($file->getPathname());

            return $this->json($result);

        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Erreur serveur : ' . $e->getMessage()
            ], 500);
        }
    }


}
