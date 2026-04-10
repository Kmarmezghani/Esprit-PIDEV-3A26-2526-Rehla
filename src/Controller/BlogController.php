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

final class BlogController extends AbstractController

{

#[Route('/blog', name: 'blog')]
public function index(Request $request, EntityManagerInterface $em)
{
      $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    $post = new Post();
    $form = $this->createForm(PostType::class, $post);
    $form->handleRequest($request);

    $likes = $request->query->get('likes');
    $date = $request->query->get('date');
    $search = $request->query->get('search');
    $dateExact = $request->query->get('date_exact');
    $sort = $request->query->get('sort');
    $order = $request->query->get('order');

    if ($form->isSubmitted() && $form->isValid()) {

        $imageFile = $form->get('image')->getData();

        if ($imageFile) {
            $newFilename = uniqid().'.'.$imageFile->guessExtension();

            try {
                $imageFile->move(
                    $this->getParameter('images_directory'),
                    $newFilename
                );
            } catch (FileException $e) {}

            $post->setImage('uploads/'.$newFilename);
        }

        $post->setDatePublication(new \DateTime());
        $post->setPopularite(0);
        $post->setPersonne_id($personne);

        $em->persist($post);
        $em->flush();

        return $this->redirectToRoute('blog');
    }

        if ($search) {
        $posts = $em->getRepository(Post::class)->searchByContent($search);
        }
        elseif ($likes) {
            $posts = $em->getRepository(Post::class)->findByLikes($likes);
        }
        elseif ($dateExact) {
        $posts = $em->getRepository(Post::class)->findByExactDate($dateExact);
        }
        elseif ($date) {
            $posts = $em->getRepository(Post::class)->findByDateFilter($date);
        }
        elseif ($sort && $order) {
            $posts = $em->getRepository(Post::class)->findSorted($sort, $order);
        }
        else {
            $posts = $em->getRepository(Post::class)->findLatestPosts();
        }

    return $this->render('blog/blog.html.twig', [
        'posts' => $posts,
        'form' => $form->createView()
    ]);
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
public function addComment(Request $request, Post $post, EntityManagerInterface $em)
{   $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    $contenu = $request->request->get('contenu');

    if (!$contenu) {
        return $this->redirectToRoute('blog');
    }

    $comment = new Commentaire();
    $comment->setContenu($contenu);
    $comment->setDateCommentaire(new \DateTime());
    $comment->setPost_id($post);
    $comment->setPersonne_id($personne);

    $em->persist($comment);
    $em->flush();

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

}
