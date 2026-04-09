<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
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
        choices: ["Vol", "Hotel", "Transport", "Activité"],
        message: "Type de ticket invalide"
    )]
    private string $type;

    #[ORM\Column(type: "string", length: 50, options: ["default" => "Disponible"])]
    #[Assert\NotBlank(message: "Le statut est obligatoire")]
    #[Assert\Choice(
        choices: ["Disponible", "Reservé", "Annulé"],
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
    private ?Reservation $reservation_id = null;

    #[ORM\ManyToOne(targetEntity: Ville::class)]
    #[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id')]
    #[Assert\NotNull(message: "Veuillez sélectionner une destination")]
    private ?Ville $destination = null;

    #[ORM\ManyToOne(targetEntity: Activite::class)]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private ?Activite $activite = null;

    public function __construct()
    {
        $this->statut = 'Disponible';
    }

    public function getId(): ?int { return $this->id; }

    public function getType(): ?string { return $this->type; }
    public function setType(string $value): self { $this->type = $value; return $this; }

    public function getStatut(): ?string { return $this->statut; }
    public function setStatut(?string $value): self { $this->statut = $value; return $this; }

    public function getPrix(): ?float { return $this->prix; }
    public function setPrix(float $value): self { $this->prix = $value; return $this; }

    public function getReservation_id(): ?Reservation { return $this->reservation_id; }
    public function setReservation_id(?Reservation $value): self { $this->reservation_id = $value; return $this; }

    public function getDestination(): ?Ville { return $this->destination; }
    public function setDestination(?Ville $destination): self { $this->destination = $destination; return $this; }

    public function getActivite(): ?Activite { return $this->activite; }
    public function setActivite(?Activite $activite): self { $this->activite = $activite; return $this; }
}