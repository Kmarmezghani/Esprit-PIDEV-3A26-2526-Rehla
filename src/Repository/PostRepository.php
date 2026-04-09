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

public function searchByContent($keyword)
{
    return $this->createQueryBuilder('p')
        ->leftJoin('p.commentaires', 'c')
        ->addSelect('c')
        ->where('p.contenu LIKE :keyword')
        ->setParameter('keyword', '%' . $keyword . '%')
        ->orderBy('p.datePublication', 'DESC')
        ->getQuery()
        ->getResult();
}
public function findByLikes($minLikes)
{
    return $this->createQueryBuilder('p')
        ->leftJoin('p.likess', 'l')
        ->groupBy('p.id')
        ->having('COUNT(l.id) >= :minLikes')
        ->setParameter('minLikes', $minLikes)
        ->orderBy('p.datePublication', 'DESC')
        ->getQuery()
        ->getResult();
}
public function findByDateFilter($filter)
{
    $qb = $this->createQueryBuilder('p');
    $now = new \DateTime();

    if ($filter === 'today') {

        $start = (clone $now)->setTime(0, 0, 0);
        $end = (clone $now)->setTime(23, 59, 59);

        $qb->andWhere('p.datePublication BETWEEN :start AND :end')
           ->setParameter('start', $start)
           ->setParameter('end', $end);
    }

    if ($filter === 'week') {

        $start = (clone $now)->modify('monday this week')->setTime(0,0);
        $end = (clone $start)->modify('+7 days');

        $qb->andWhere('p.datePublication BETWEEN :start AND :end')
           ->setParameter('start', $start)
           ->setParameter('end', $end);
    }

    if ($filter === 'month') {

        $start = (clone $now)->modify('first day of this month')->setTime(0,0);
        $end = (clone $now)->modify('last day of this month')->setTime(23,59,59);

        $qb->andWhere('p.datePublication BETWEEN :start AND :end')
           ->setParameter('start', $start)
           ->setParameter('end', $end);
    }

    return $qb->orderBy('p.datePublication', 'DESC')
              ->getQuery()
              ->getResult();
}
public function findByExactDate($date)
{
    $start = new \DateTime($date);
    $start->setTime(0, 0, 0);

    $end = (clone $start)->setTime(23, 59, 59);

    return $this->createQueryBuilder('p')
        ->where('p.datePublication BETWEEN :start AND :end')
        ->setParameter('start', $start)
        ->setParameter('end', $end)
        ->orderBy('p.datePublication', 'DESC')
        ->getQuery()
        ->getResult();
}
public function findSorted($sort, $order)
{
    $qb = $this->createQueryBuilder('p');

    if ($sort === 'likes') {
        $qb->leftJoin('p.likess', 'l')
           ->groupBy('p.id')
           ->orderBy('COUNT(l.id)', $order);
    }

    if ($sort === 'date') {
        $qb->orderBy('p.datePublication', $order);
    }

    if ($sort === 'author') {
        $qb->leftJoin('p.personne_id', 'u')
           ->orderBy('u.nom', $order);
    }

    return $qb->getQuery()->getResult();
}

    
}