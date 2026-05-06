<?php

namespace App\Service;
use App\Entity\Post;

class PostManager
{
    public function validate(Post $post): bool
    {
        $contenu = trim($post->getContenu() ?? '');

        if ($contenu === '') {
            throw new \InvalidArgumentException('Contenu obligatoire');
        }

   
        if (strlen($contenu) < 3) {
            throw new \InvalidArgumentException('Minimum 3 caractères');
        }

  
        if (!preg_match('/^[a-zA-Z0-9]/', $contenu)) {
            throw new \InvalidArgumentException('Doit commencer par lettre ou chiffre');
        }

        return true;
    }
}
