<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
class Activite
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "string", length: 150, nullable: true)]
    #[Assert\NotBlank(message: "Le nom est obligatoire.")]
    private ?string $nom = '';

    #[ORM\Column(type: "text", nullable: true)]
    #[Assert\NotBlank(message: "La description est obligatoire.")]
    private ?string $description = '';

    #[ORM\Column(type: "float", nullable: true)]
    #[Assert\NotBlank(message: "Le prix est obligatoire.")]
    #[Assert\Positive(message: "Le prix doit être positif.")]
    private ?float $prix = 0;

    #[ORM\Column(name: "typeActivite", type: "string", length: 100, nullable: true)]
    #[Assert\NotBlank(message: "Le type d'activité est obligatoire.")]
    private ?string $typeActivite = '';

    #[ORM\Column(name: "noteMoyenne", type: "float", nullable: true)]
    private ?float $noteMoyenne = 0;

    #[ORM\ManyToOne(targetEntity: Guide::class, inversedBy: "activites")]
    #[ORM\JoinColumn(name: 'guide_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private ?Guide $guide = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    #[Assert\NotBlank(message: "La date de début est obligatoire.")]
    #[Assert\Type(\DateTimeInterface::class)]
    #[Assert\GreaterThanOrEqual(
        "today",
        message: "La date de début ne peut pas être antérieure à aujourd'hui."
    )]
    private ?\DateTimeInterface $date_debut = null;

    #[ORM\Column(type: "datetime", nullable: true)]
    #[Assert\NotBlank(message: "La date de fin est obligatoire.")]
    #[Assert\Type(\DateTimeInterface::class)]
    private ?\DateTimeInterface $date_fin = null;

    #[ORM\Column(type: "string", nullable: false, options: ["default" => "DISPONIBLE"])]
    private string $status = 'DISPONIBLE';

    #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "activites")]
    #[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    #[Assert\NotNull(message: "La destination est obligatoire.")]
    private ?Ville $destination = null;

    #[ORM\Column(type: "integer", nullable: true)]
    #[Assert\NotBlank(message: "Le nombre maximum de places est obligatoire.")]
    #[Assert\PositiveOrZero(message: "Le nombre de places doit être positif ou nul.")]
    private ?int $max_places = 0;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private ?string $image = '';

    #[ORM\Column(type: "boolean", nullable: false, options: ["default" => false])]
    private bool $is_flash_sale = false;

    #[ORM\Column(type: "float", nullable: true)]
    private ?float $flash_price = 0;

    #[ORM\Column(type: "datetime", nullable: true)]
    private ?\DateTimeInterface $flash_expires_at = null;

    #[ORM\OneToMany(mappedBy: "activite", targetEntity: Avis::class)]
    private Collection $aviss;

    #[ORM\OneToMany(mappedBy: "activite", targetEntity: Waitlist::class)]
    private Collection $waitlists;

    #[ORM\OneToMany(mappedBy: "activite", targetEntity: Ticket::class)]
    private Collection $tickets;

    #[ORM\OneToMany(mappedBy: "activite", targetEntity: Notification::class)]
    private Collection $notifications;

    public function __construct()
    {
        $this->aviss = new ArrayCollection();
        $this->waitlists = new ArrayCollection();
        $this->tickets = new ArrayCollection();
        $this->notifications = new ArrayCollection();

        $this->date_debut = new \DateTime();
        $this->date_fin = new \DateTime();
        $this->flash_expires_at = new \DateTime();
    }

    // --- Getters et Setters ---
    public function getId(): int { return $this->id; }

    public function getNom(): string { return $this->nom ?? ''; }
    public function setNom(?string $nom): self { $this->nom = $nom ?? ''; return $this; }

    public function getDescription(): string { return $this->description ?? ''; }
    public function setDescription(?string $description): self { $this->description = $description ?? ''; return $this; }

    public function getPrix(): float { return $this->prix ?? 0; }
    public function setPrix(?float $prix): self { $this->prix = $prix ?? 0; return $this; }

    public function getTypeActivite(): string { return $this->typeActivite ?? ''; }
    public function setTypeActivite(?string $typeActivite): self { $this->typeActivite = $typeActivite ?? ''; return $this; }

    public function getNoteMoyenne(): float { return $this->noteMoyenne ?? 0; }
    public function setNoteMoyenne(?float $noteMoyenne): self { $this->noteMoyenne = $noteMoyenne ?? 0; return $this; }

    public function getGuide(): ?Guide { return $this->guide; }
    public function setGuide(?Guide $guide): self { $this->guide = $guide; return $this; }

    public function getDateDebut(): \DateTimeInterface { return $this->date_debut ?? new \DateTime(); }
    public function setDateDebut(?\DateTimeInterface $date_debut): self { $this->date_debut = $date_debut ?? new \DateTime(); return $this; }

    public function getDateFin(): \DateTimeInterface { return $this->date_fin ?? new \DateTime(); }
    public function setDateFin(?\DateTimeInterface $date_fin): self { $this->date_fin = $date_fin ?? new \DateTime(); return $this; }

    public function getStatus(): string { return $this->status; }
    public function setStatus(string $status): self { $this->status = $status; return $this; }

    public function getDestination(): ?Ville { return $this->destination; }
    public function setDestination(?Ville $destination): self { $this->destination = $destination; return $this; }

    public function getMaxPlaces(): int { return $this->max_places ?? 0; }
    public function setMaxPlaces(?int $max_places): self { $this->max_places = $max_places ?? 0; return $this; }

    public function getImage(): string { return $this->image ?? ''; }
    public function setImage(?string $image): self { $this->image = $image ?? ''; return $this; }

    public function getIsFlashSale(): bool { return $this->is_flash_sale; }
    public function setIsFlashSale(bool $is_flash_sale): self { $this->is_flash_sale = $is_flash_sale; return $this; }

    public function getFlashPrice(): float { return $this->flash_price ?? 0; }
    public function setFlashPrice(?float $flash_price): self { $this->flash_price = $flash_price ?? 0; return $this; }

    public function getFlashExpiresAt(): ?\DateTimeInterface { return $this->flash_expires_at; }
    public function setFlashExpiresAt(?\DateTimeInterface $flash_expires_at): self { $this->flash_expires_at = $flash_expires_at ?? new \DateTime(); return $this; }

    // --- Collections ---
    public function getAviss(): Collection { return $this->aviss; }
    public function addAvis(Avis $avis): self {
        if (!$this->aviss->contains($avis)) {
            $this->aviss[] = $avis;
            $avis->setActivite($this);
        }
        return $this;
    }
    public function removeAvis(Avis $avis): self {
        if ($this->aviss->removeElement($avis)) {
            if ($avis->getActivite() === $this) { $avis->setActivite(null); }
        }
        return $this;
    }

    public function getWaitlists(): Collection { return $this->waitlists; }
    public function getTickets(): Collection { return $this->tickets; }
    public function getNotifications(): Collection { return $this->notifications; }
}