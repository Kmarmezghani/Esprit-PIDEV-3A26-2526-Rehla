<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Activite;

use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
class Ticket
{

    #[ORM\Id]
#[ORM\GeneratedValue]
#[ORM\Column(type: "integer")]
private ?int $id = null;

    #[ORM\Column(type: "string", length: 100)]
#[Assert\NotBlank(message: "Le type est obligatoire")]
#[Assert\Choice(
    choices: ["Vol","hotel","Transport","Activité"],
    message: "Type de ticket invalide"
)]
private string $type; 

    #[ORM\Column(name: "dateDebut",type: "date")]
    private \DateTimeInterface $dateDebut;

    #[ORM\Column(name: "dateFin",type: "date")]
    private \DateTimeInterface $dateFin;

#[ORM\Column(type: "string", length: 50, options: ["default" => "Disponible"])]
#[Assert\NotBlank(message: "Le statut est obligatoire")]
#[Assert\Choice(
    choices: ["Disponible","Reservé","Annulé"],
    message: "Statut invalide"
)]
private ?string $statut = null;

    #[ORM\Column(type: "float")]
#[Assert\NotBlank(message: "Le prix est obligatoire")]
#[Assert\Positive(message: "Le prix doit être positif")]
#[Assert\LessThan(100000, message: "Le prix est trop élevé")]
private float $prix;

        #[ORM\ManyToOne(targetEntity: Reservation::class, inversedBy: "tickets")]
    #[ORM\JoinColumn(name: 'reservation_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Reservation $reservation_id;

       #[ORM\ManyToOne(targetEntity: Ville::class)]
#[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id')]
#[Assert\NotNull(message: "Veuillez sélectionner une destination")]
private ?Ville $destination = null;


    #[ORM\ManyToOne(targetEntity: Activite::class)]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private ?Activite $activite = null;


    public function getId(): ?int
{
    return $this->id;
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

    public function getDestination(): ?Ville
{
    return $this->destination;
}

public function setDestination(?Ville $destination): self
{
    $this->destination = $destination;
    return $this;
}

    public function getActivite(): ?Activite
    {
        return $this->activite;
    }

    // setter
    public function setActivite(?Activite $activite): self
    {
        $this->activite = $activite;
        return $this;
    }
    public function __construct()
{
    $this->statut = 'Disponible';
}
}

