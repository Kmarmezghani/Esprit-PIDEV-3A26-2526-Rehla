<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Pays;
use Doctrine\Common\Collections\Collection;
use App\Entity\Attraction;

#[ORM\Entity]
class Ville
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "string", length: 100)]
    private string $nom;

        #[ORM\ManyToOne(targetEntity: Pays::class, inversedBy: "villes")]
    #[ORM\JoinColumn(name: 'pays_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Pays $pays_id;

    #[ORM\Column(type: "integer")]
    private int $visit_count;

    #[ORM\Column(type: "float")]
    private float $latitude;

    #[ORM\Column(type: "float")]
    private float $longitude;

    #[ORM\Column(name: "typeTourisme", type: "string")]
    private string $typeTourisme;

    #[ORM\Column(type: "string")]
    private string $saison;

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

    public function getPays_id()
    {
        return $this->pays_id;
    }

    public function setPays_id($value)
    {
        $this->pays_id = $value;
    }

    public function getVisit_count()
    {
        return $this->visit_count;
    }

    public function setVisit_count($value)
    {
        $this->visit_count = $value;
    }

    public function getLatitude()
    {
        return $this->latitude;
    }

    public function setLatitude($value)
    {
        $this->latitude = $value;
    }

    public function getLongitude()
    {
        return $this->longitude;
    }

    public function setLongitude($value)
    {
        $this->longitude = $value;
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

    #[ORM\OneToMany(mappedBy: "ville_id", targetEntity: Attraction::class)]
    private Collection $attractions;

    #[ORM\OneToMany(mappedBy: "destination_id", targetEntity: Activite::class)]
    private Collection $activites;

        public function getActivites(): Collection
        {
            return $this->activites;
        }
    
        public function addActivite(Activite $activite): self
        {
            if (!$this->activites->contains($activite)) {
                $this->activites[] = $activite;
                $activite->setDestination_id($this);
            }
    
            return $this;
        }
    
        public function removeActivite(Activite $activite): self
        {
            if ($this->activites->removeElement($activite)) {
                // set the owning side to null (unless already changed)
                if ($activite->getDestination_id() === $this) {
                    $activite->setDestination_id(null);
                }
            }
    
            return $this;
        }

    #[ORM\OneToMany(mappedBy: "destination_id", targetEntity: Ticket::class)]
    private Collection $tickets;
}
