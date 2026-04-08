<?php

namespace App\Repository;

use App\Entity\Post;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class PostRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Post::class);
    }

    /**
     * 🔥 Récupérer tous les posts triés du plus récent au plus ancien
     */
    public function findLatestPosts()
    {
        return $this->createQueryBuilder('p')
            ->orderBy('p.datePublication', 'DESC')
            ->getQuery()
            ->getResult();
    }

    
}