<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Pays;
use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;
use App\Entity\Attraction;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Bridge\Doctrine\Validator\Constraints\UniqueEntity;

#[ORM\Entity]
#[UniqueEntity(fields: ['nom', 'pays_id'], message: 'Cette ville existe déjà dans ce pays.')]
class Ville
{
    public function __construct()
    {
        $this->attractions = new ArrayCollection();
        $this->activites = new ArrayCollection();
        $this->tickets = new ArrayCollection();
    }

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(type: "string", length: 100)]
    #[Assert\NotBlank(message: 'Le nom est obligatoire')]
    #[Assert\Regex(
        pattern: '/^[\p{L}0-9\s\-]+$/u',
        message: 'Le nom ne doit contenir que des lettres, chiffres, espaces et tirets (pas de caractères spéciaux comme ; ? !)'
    )]
    private string $nom;

    #[ORM\ManyToOne(targetEntity: Pays::class, inversedBy: "villes")]
    #[ORM\JoinColumn(name: 'pays_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Pays $pays_id;

    #[ORM\Column(type: "integer", nullable: true)]
    #[Assert\PositiveOrZero(message: 'Le nombre de visites ne peut pas être négatif.')]
    private ?int $visit_count;

    #[ORM\Column(type: "float", nullable: true)]
    private ?float $latitude;

    #[ORM\Column(type: "float", nullable: true)]
    private ?float $longitude;

    #[ORM\Column(name: "typeTourisme", type: "string")]
    private string $typeTourisme;

    #[ORM\Column(type: "string")]
    private string $saison;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private ?string $image = null;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getNom()
    {
        return $this->nom;
    }

    public function setNom($value)
    {
        $this->nom = $value;
    }

    public function getPaysId(): Pays
    {
        return $this->pays_id;
    }

    public function setPaysId(Pays $value): self
    {
        $this->pays_id = $value;
        return $this;
    }

    public function getVisitCount(): ?int
    {
        return $this->visit_count;
    }

    public function setVisitCount(?int $value): self
    {
        $this->visit_count = $value !== null ? max(0, $value) : null;
        return $this;
    }

    public function getLatitude(): ?float
    {
        return $this->latitude;
    }

    public function setLatitude(?float $value): self
    {
        $this->latitude = $value;
        return $this;
    }

    public function getLongitude(): ?float
    {
        return $this->longitude;
    }

    public function setLongitude(?float $value): self
    {
        $this->longitude = $value;
        return $this;
    }

    public function getTypeTourisme()
    {
        return $this->typeTourisme;
    }

    public function setTypeTourisme($value)
    {
        $this->typeTourisme = $value;
    }

    public function getSaison()
    {
        return $this->saison;
    }

    public function setSaison($value)
    {
        $this->saison = $value;
    }

    public function getImage(): ?string
    {
        return $this->image;
    }

    public function setImage(?string $image): self
    {
        $this->image = $image;
        return $this;
    }

    #[ORM\OneToMany(mappedBy: "ville_id", targetEntity: Attraction::class)]
    private Collection $attractions;

    public function getAttractions(): Collection
    {
        return $this->attractions;
    }

    #[ORM\OneToMany(mappedBy: "destination", targetEntity: Activite::class)]
    private Collection $activites;

    public function getActivites(): Collection
    {
        return $this->activites;
    }

    public function addActivite(Activite $activite): self
    {
        if (!$this->activites->contains($activite)) {
            $this->activites[] = $activite;
            $activite->setDestination($this);
        }

        return $this;
    }

    public function removeActivite(Activite $activite): self
    {
        if ($this->activites->removeElement($activite)) {
            // set the owning side to null (unless already changed)
            if ($activite->getDestination() === $this) {
                $activite->setDestination(null);
            }
        }

        return $this;
    }

    #[ORM\OneToMany(mappedBy: "destination", targetEntity: Ticket::class)]
    private Collection $tickets;
}
