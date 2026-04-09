<?php

namespace App\Repository;

use App\Entity\Post;

use App\Entity\Commentaire;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class PostRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Post::class);
    }


   public function findLatestPosts()
{
    return $this->createQueryBuilder('p')
        ->leftJoin('p.commentaires', 'c')
        ->addSelect('c')
        ->orderBy('p.datePublication', 'DESC')
        ->getQuery()
        ->getResult();
}

    
}