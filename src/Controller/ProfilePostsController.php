<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class ProfilePostsController extends AbstractController
{
    #[Route('/profile/posts', name: 'profile_posts')]
    public function index(): Response
    {
        return $this->render('profile_posts/profilePosts.html.twig', [
            'controller_name' => 'ProfilePostsController',
        ]);
    }
}
