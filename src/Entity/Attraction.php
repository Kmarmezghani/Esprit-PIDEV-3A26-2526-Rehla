<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Ville;
use Symfony\Bridge\Doctrine\Validator\Constraints\UniqueEntity;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
#[UniqueEntity(fields: ['nom'], message: 'Cette attraction existe déjà.')]
class Attraction
{

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

    #[ORM\Column(type: "text", nullable: true)]
    private ?string $description;

    #[ORM\Column(type: "string")]
    private string $type;

    #[ORM\Column(type: "decimal", precision: 10, scale: 2)]
    #[Assert\Positive(message: 'Le prix doit être positif')]
    private ?float $prix = null;

        #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "attractions")]
    #[ORM\JoinColumn(name: 'ville_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Ville $ville_id;

    #[ORM\Column(type: "time")]
    private ?\DateTimeInterface $heure_ouverture = null;

    #[ORM\Column(type: "time")]
    private ?\DateTimeInterface $heure_fermeture = null;

    #[ORM\Column(type: "boolean", nullable: true)]
    private ?bool $est_ferme;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getNom(): string
    {
        return $this->nom;
    }

    public function setNom(string $value): self
    {
        $this->nom = $value;
        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(?string $value): self
    {
        $this->description = $value;
        return $this;
    }

    public function getType(): string
    {
        return $this->type;
    }

    public function setType(string $value): self
    {
        $this->type = $value;
        return $this;
    }

    public function getPrix(): ?float
    {
        return $this->prix;
    }

    public function setPrix(?float $value): self
    {
        $this->prix = $value;
        return $this;
    }

    public function getVilleId(): Ville
    {
        return $this->ville_id;
    }

    public function setVilleId(Ville $value): self
    {
        $this->ville_id = $value;
        return $this;
    }

    public function getHeureOuverture(): ?\DateTimeInterface
    {
        return $this->heure_ouverture;
    }

    public function setHeureOuverture(?\DateTimeInterface $value): self
    {
        $this->heure_ouverture = $value;
        return $this;
    }

    public function getHeureFermeture(): ?\DateTimeInterface
    {
        return $this->heure_fermeture;
    }

    public function setHeureFermeture(?\DateTimeInterface $value): self
    {
        $this->heure_fermeture = $value;
        return $this;
    }

    public function getEstFerme(): ?bool
    {
        return $this->est_ferme;
    }

    public function setEstFerme(?bool $value): self
    {
        $this->est_ferme = $value;
        return $this;
    }
}
