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

final class BlogController extends AbstractController
{

#[Route('/blog', name: 'blog')]
public function index(Request $request, EntityManagerInterface $em)
{   $personne = $em->getRepository(Personne::class)->find(4);
    $post = new Post();
    $form = $this->createForm(PostType::class, $post);

    $form->handleRequest($request);

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

    $posts = $em->getRepository(Post::class)->findLatestPosts();

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
{
    $personne = $em->getRepository(Personne::class)->find(4);

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

}
