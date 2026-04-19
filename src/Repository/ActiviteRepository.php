<?php

namespace App\Repository;

use App\Entity\Activite;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;

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
    
    public function searchAndSortAdmin(?string $search, string $sort = 'date_debut', string $direction = 'desc'): array
{
    $qb = $this->createQueryBuilder('a')
        ->leftJoin('a.destination', 'd')
        ->leftJoin('a.guide', 'g')
        ->addSelect('d', 'g');

    if (!empty($search)) {
        $qb->andWhere('
            a.nom LIKE :search
            OR a.description LIKE :search
            OR a.typeActivite LIKE :search
            OR d.nom LIKE :search
        ')
        ->setParameter('search', '%' . $search . '%');
    }

    $sortMap = [
        'nom' => 'a.nom',
        'prix' => 'a.prix',
        'noteMoyenne' => 'a.noteMoyenne',
        'date_debut' => 'a.date_debut',
        'status' => 'a.status',
    ];

    $qb->orderBy($sortMap[$sort] ?? 'a.date_debut', strtoupper($direction));

    return $qb->getQuery()->getResult();
}
public function searchFront(
    ?string $destination,
    ?string $dateDebut,
    ?string $dateFin,
    $prixMax
): array
{
    $qb = $this->createQueryBuilder('a')
        ->leftJoin('a.destination', 'd')
        ->addSelect('d')
        ->andWhere('a.status = :status')
        ->setParameter('status', 'DISPONIBLE');

    if (!empty($destination)) {
        $qb->andWhere('
            d.nom LIKE :destination
            OR a.nom LIKE :destination
            OR a.typeActivite LIKE :destination
        ')
        ->setParameter('destination', '%' . $destination . '%');
    }

    if (!empty($dateDebut)) {
        try {
            $dateDebutObj = new \DateTime($dateDebut);
            $qb->andWhere('a.date_debut >= :dateDebut')
               ->setParameter('dateDebut', $dateDebutObj->format('Y-m-d 00:00:00'));
        } catch (\Exception $e) {
        }
    }

    if (!empty($dateFin)) {
        try {
            $dateFinObj = new \DateTime($dateFin);
            $qb->andWhere('a.date_fin <= :dateFin')
               ->setParameter('dateFin', $dateFinObj->format('Y-m-d 23:59:59'));
        } catch (\Exception $e) {
        }
    }

    if (!empty($prixMax) && is_numeric($prixMax)) {
        $qb->andWhere('a.prix <= :prixMax')
           ->setParameter('prixMax', (float) $prixMax);
    }

    return $qb->orderBy('a.date_debut', 'ASC')
              ->getQuery()
              ->getResult();
}
public function searchFrontQueryBuilder(
    ?string $destination,
    ?string $dateDebut,
    ?string $dateFin,
    ?string $prixMax
) {
    $qb = $this->createQueryBuilder('a')
        ->leftJoin('a.destination', 'd')
        ->addSelect('d');

    // ✅ IMPORTANT : seulement disponible
    $qb->andWhere('a.status = :status')
       ->setParameter('status', 'DISPONIBLE');

    if (!empty($destination)) {
        $qb->andWhere('d.nom LIKE :destination')
           ->setParameter('destination', '%' . $destination . '%');
    }

    if (!empty($dateDebut)) {
        $qb->andWhere('a.date_debut >= :dateDebut')
           ->setParameter('dateDebut', new \DateTime($dateDebut));
    }

    if (!empty($dateFin)) {
        $qb->andWhere('a.date_fin <= :dateFin')
           ->setParameter('dateFin', new \DateTime($dateFin));
    }

    if (!empty($prixMax)) {
        $qb->andWhere('a.prix <= :prixMax')
           ->setParameter('prixMax', (float) $prixMax);
    }

    $qb->orderBy('a.date_debut', 'ASC');

    return $qb;
}
}