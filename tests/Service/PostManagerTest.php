<?php

namespace App\Tests;

namespace App\Tests\Service;

use App\Entity\Post;
use App\Service\PostManager;
use PHPUnit\Framework\TestCase;

class PostManagerTest extends TestCase
{
    //  CAS VALIDE
    public function testValidPost()
    {
        $post = new Post();
        $post->setContenu('Bonjour tout le monde');

        $manager = new PostManager();

        $this->assertTrue($manager->validate($post));
    }

    //  CONTENU VIDE
    public function testContenuVide()
    {
        $this->expectException(\InvalidArgumentException::class);

        $post = new Post();
        $post->setContenu('');

        $manager = new PostManager();
        $manager->validate($post);
    }

    //  CONTENU TROP COURT
    public function testContenuCourt()
    {
        $this->expectException(\InvalidArgumentException::class);

        $post = new Post();
        $post->setContenu('ab');

        $manager = new PostManager();
        $manager->validate($post);
    }

    //  MAUVAIS FORMAT
    public function testContenuInvalide()
    {
        $this->expectException(\InvalidArgumentException::class);

        $post = new Post();
        $post->setContenu('@hello');

        $manager = new PostManager();
        $manager->validate($post);
    }
}
