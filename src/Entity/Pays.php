<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use Doctrine\Common\Collections\Collection;
use App\Entity\Reservation;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Bridge\Doctrine\Validator\Constraints\UniqueEntity;

#[ORM\Entity]
#[UniqueEntity(fields: ['nom'], message: 'Ce pays existe déjà.')]
class Pays
{

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(type: "string", length: 100, nullable: true)]
    private ?string $nom;

    #[ORM\Column(type: "text")]
    private string $description;

    #[ORM\Column(type: "integer", nullable: true)]
    #[Assert\PositiveOrZero(message: 'Le nombre de visites ne peut pas être négatif.')]
    private ?int $visit_count;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getNom(): ?string
    {
        return $this->nom;
    }

    public function setNom(?string $value): self
    {
        $this->nom = $value;
        return $this;
    }

    public function getDescription()
    {
        return $this->description;
    }

    public function setDescription($value)
    {
        $this->description = $value;
    }

    public function getVisitCount(): ?int
    {
        return $this->visit_count;
    }

    public function setVisitCount(?int $value): self
    {
        $this->visit_count = $value;
        return $this;
    }

    #[ORM\OneToMany(mappedBy: "pays_id", targetEntity: Ville::class)]
    private Collection $villes;

        public function getVilles(): Collection
        {
            return $this->villes;
        }
    
        public function addVille(Ville $ville): self
        {
            if (!$this->villes->contains($ville)) {
                $this->villes[] = $ville;
                $ville->setPays_id($this);
            }
    
            return $this;
        }
    
        public function removeVille(Ville $ville): self
        {
            if ($this->villes->removeElement($ville)) {
                // set the owning side to null (unless already changed)
                if ($ville->getPays_id() === $this) {
                    $ville->setPays_id(null);
                }
            }
    
            return $this;
        }

    #[ORM\OneToMany(mappedBy: "destination_id", targetEntity: Reservation::class)]
    private Collection $reservations;

        public function getReservations(): Collection
        {
            return $this->reservations;
        }
    
        public function addReservation(Reservation $reservation): self
        {
            if (!$this->reservations->contains($reservation)) {
                $this->reservations[] = $reservation;
                $reservation->setDestination_id($this);
            }
    
            return $this;
        }
    
        public function removeReservation(Reservation $reservation): self
        {
            if ($this->reservations->removeElement($reservation)) {
                // set the owning side to null (unless already changed)
                if ($reservation->getDestination_id() === $this) {
                    $reservation->setDestination_id(null);
                }
            }
    
            return $this;
        }
}
