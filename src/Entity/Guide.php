<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;
use App\Entity\Personne;
use App\Entity\Activite;

#[ORM\Entity]
class Guide
{
    #[ORM\Id]
    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "guides")]
    #[ORM\JoinColumn(name: 'id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne;

    #[ORM\Column(type: "string", length: 150)]
    private string $specialite;

    #[ORM\Column(type: "string", length: 250)]
    private string $langues;

    #[ORM\Column(type: "string")]
    private string $experience;

    #[ORM\OneToMany(mappedBy: "guide_id", targetEntity: Activite::class)]
    private Collection $activites;

    public function __construct()
    {
        $this->activites = new ArrayCollection();
    }

    // ------------------- Getters et setters -------------------
    public function getPersonne(): ?Personne
    {
        return $this->personne;
    }

    public function setPersonne(?Personne $personne): self
    {
        $this->personne = $personne;
        return $this;
    }

    public function getSpecialite(): string
    {
        return $this->specialite;
    }

    public function setSpecialite(string $specialite): self
    {
        $this->specialite = $specialite;
        return $this;
    }

    public function getLangues(): string
    {
        return $this->langues;
    }

    public function setLangues(string $langues): self
    {
        $this->langues = $langues;
        return $this;
    }

    public function getExperience(): string
    {
        return $this->experience;
    }

    public function setExperience(string $experience): self
    {
        $this->experience = $experience;
        return $this;
    }

    // ------------------- Redirection vers Personne -------------------
    public function getNom(): ?string
    {
        return $this->personne ? $this->personne->getNom() : null;
    }

    public function getPrenom(): ?string
    {
        return $this->personne ? $this->personne->getPrenom() : null;
    }

    // ------------------- Activites -------------------
    public function getActivites(): Collection
    {
        return $this->activites;
    }

    public function addActivite(Activite $activite): self
    {
        if (!$this->activites->contains($activite)) {
            $this->activites[] = $activite;
            $activite->setGuide($this);
        }
        return $this;
    }

    public function removeActivite(Activite $activite): self
    {
        if ($this->activites->removeElement($activite)) {
            if ($activite->getGuide() === $this) {
                $activite->setGuide(null);
            }
        }
        return $this;
    }
}