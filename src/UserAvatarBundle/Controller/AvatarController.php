<?php

namespace App\UserAvatarBundle\Controller;

use App\UserAvatarBundle\Service\AvatarService;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class AvatarController
{
    public function __construct(private AvatarService $avatarService) {}

    #[Route('/avatar/{prenom}/{nom}', name: 'user_avatar', methods: ['GET'])]
    public function generate(string $prenom, string $nom): Response
    {
        return $this->avatarService->generateResponse($prenom, $nom);
    }
}
