<?php

namespace App\Repository;

use App\Entity\Activite;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class ActiviteRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Activite::class);
    }

   
    public function save(Activite $activite, bool $flush = false): void
    {
        $this->_em->persist($activite);
        if ($flush) {
            $this->_em->flush();
        }
    }

    
    public function remove(Activite $activite, bool $flush = false): void
    {
        $this->_em->remove($activite);
        if ($flush) {
            $this->_em->flush();
        }
    }
}