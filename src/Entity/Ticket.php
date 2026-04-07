<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Activite;

#[ORM\Entity]
class Ticket
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "string", length: 100)]
    private string $type;

    #[ORM\Column(type: "date")]
    private \DateTimeInterface $dateDebut;

    #[ORM\Column(type: "date")]
    private \DateTimeInterface $dateFin;

    #[ORM\Column(type: "string", length: 50)]
    private string $statut;

    #[ORM\Column(type: "float")]
    private float $prix;

        #[ORM\ManyToOne(targetEntity: Reservation::class, inversedBy: "tickets")]
    #[ORM\JoinColumn(name: 'reservation_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Reservation $reservation_id;

        #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "tickets")]
    #[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Ville $destination_id;

        #[ORM\ManyToOne(targetEntity: Activite::class, inversedBy: "tickets")]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Activite $activite_id;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getType()
    {
        return $this->type;
    }

    public function setType($value)
    {
        $this->type = $value;
    }

    public function getDateDebut()
    {
        return $this->dateDebut;
    }

    public function setDateDebut($value)
    {
        $this->dateDebut = $value;
    }

    public function getDateFin()
    {
        return $this->dateFin;
    }

    public function setDateFin($value)
    {
        $this->dateFin = $value;
    }

    public function getStatut()
    {
        return $this->statut;
    }

    public function setStatut($value)
    {
        $this->statut = $value;
    }

    public function getPrix()
    {
        return $this->prix;
    }

    public function setPrix($value)
    {
        $this->prix = $value;
    }

    public function getReservation_id()
    {
        return $this->reservation_id;
    }

    public function setReservation_id($value)
    {
        $this->reservation_id = $value;
    }

    public function getDestination_id()
    {
        return $this->destination_id;
    }

    public function setDestination_id($value)
    {
        $this->destination_id = $value;
    }

    public function getActivite_id()
    {
        return $this->activite_id;
    }

    public function setActivite_id($value)
    {
        $this->activite_id = $value;
    }
}
