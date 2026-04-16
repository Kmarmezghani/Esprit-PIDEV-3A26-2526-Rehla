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
use App\Entity\Favoris;
use App\Entity\Favoris_post;
use Symfony\Component\HttpFoundation\JsonResponse;

final class FavorisController extends AbstractController
{
     #[Route('/profil/posts/favoris', name: 'favoris_posts')]
    public function favorisPosts(Request $request, EntityManagerInterface $em): Response
    {
        // 1. user session
        $userId = $request->getSession()->get('user_id');

        if (!$userId) {
            return $this->redirectToRoute('home');
        }

        // 2. personne connectée
        $personne = $em->getRepository(Personne::class)->find($userId);

        if (!$personne) {
            return $this->redirectToRoute('home');
        }

        // 3. récupérer favoris du user
        $favoris = $em->getRepository(Favoris::class)
            ->findOneBy(['personne_id' => $personne]);

        $posts = [];

        // 4. récupérer les posts favoris
        if ($favoris) {
            $favorisPosts = $em->getRepository(Favoris_post::class)
                ->findBy(['favoris_id' => $favoris]);

            foreach ($favorisPosts as $fp) {
                if ($fp->getPost_id()) {
                    $posts[] = $fp->getPost_id();
                }
            }
        }

        // 5. formulaire (si tu veux garder publier post ici)
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

            return $this->redirectToRoute('favoris_posts');
        }

        // 6. render
        return $this->render('favorisPost/favoris.html.twig', [
            'personne' => $personne,
            'posts' => $posts,
            'form' => $form->createView(),
        ]);
    }
    #[Route('/favoris/toggle/{id}', name: 'favoris_toggle')]
public function toggleFavoris(Post $post, EntityManagerInterface $em, Request $request): JsonResponse
{
    $userId = $request->getSession()->get('user_id');

    if (!$userId) {
        return new JsonResponse(['success' => false]);
    }

    // récupérer la personne
    $personne = $em->getRepository(Personne::class)->find($userId);

    // récupérer ou créer favoris
    $favoris = $em->getRepository(Favoris::class)
        ->findOneBy(['personne_id' => $personne]);

    if (!$favoris) {
        $favoris = new Favoris();
        $favoris->setPersonne_id($personne);
        $favoris->setDateCreation(new \DateTime());

        $em->persist($favoris);
    }

    // vérifier si déjà favori
    $existing = $em->getRepository(Favoris_post::class)->findOneBy([
        'favoris_id' => $favoris,
        'post_id' => $post
    ]);

    if ($existing) {
        // supprimer (toggle off)
        $em->remove($existing);
        $isFavori = false;
    } else {
        $fp = new Favoris_post();
        $fp->setFavoris_id($favoris);
        $fp->setPost_id($post);
        $fp->setDateAjout(new \DateTime());

        $em->persist($fp);
        $isFavori = true;
    }

    $em->flush();

    return new JsonResponse([
        'success' => true,
        'favori' => $isFavori
    ]);
}


}
