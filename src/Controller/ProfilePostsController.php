<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\Request;
use Doctrine\ORM\EntityManagerInterface;
use App\Entity\Post;
use App\Entity\Personne;
use App\Form\PostType; 
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\File\Exception\FileException;

final class ProfilePostsController extends AbstractController
{
    #[Route('/profil/posts', name: 'profile_posts')]
    public function profilePosts(Request $request, EntityManagerInterface $em): Response
    {
        // récupérer l'utilisateur connecté depuis la session
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            return $this->redirectToRoute('profile_posts'); // ou login
        }

        // récupérer la personne
        $personne = $em->getRepository(Personne::class)->find($userId);

        // récupérer ses posts uniquement
        $posts = $em->getRepository(Post::class)->findBy(
            ['personne_id' => $personne],
            ['datePublication' => 'DESC']
        );

        // créer le formulaire pour ajouter un nouveau post
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

        return $this->redirectToRoute('profile_posts');
    }

        return $this->render('profile_posts/profilePosts.html.twig', [
            'personne' => $personne,
            'posts' => $posts,
            'form' => $form->createView(), 
        ]);
    }

 


}
