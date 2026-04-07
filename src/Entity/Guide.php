<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;
use Doctrine\Common\Collections\Collection;
use App\Entity\Activite;

#[ORM\Entity]
class Guide
{

    #[ORM\Id]
        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "guides")]
    #[ORM\JoinColumn(name: 'id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $id;

    #[ORM\Column(type: "string", length: 150)]
    private string $specialite;

    #[ORM\Column(type: "string", length: 250)]
    private string $langues;

    #[ORM\Column(type: "string")]
    private string $experience;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getSpecialite()
    {
        return $this->specialite;
    }

    public function setSpecialite($value)
    {
        $this->specialite = $value;
    }

    public function getLangues()
    {
        return $this->langues;
    }

    public function setLangues($value)
    {
        $this->langues = $value;
    }

    public function getExperience()
    {
        return $this->experience;
    }

    public function setExperience($value)
    {
        $this->experience = $value;
    }

    #[ORM\OneToMany(mappedBy: "guide_id", targetEntity: Activite::class)]
    private Collection $activites;

        public function getActivites(): Collection
        {
            return $this->activites;
        }
    
        public function addActivite(Activite $activite): self
        {
            if (!$this->activites->contains($activite)) {
                $this->activites[] = $activite;
                $activite->setGuide_id($this);
            }
    
            return $this;
        }
    
        public function removeActivite(Activite $activite): self
        {
            if ($this->activites->removeElement($activite)) {
                // set the owning side to null (unless already changed)
                if ($activite->getGuide_id() === $this) {
                    $activite->setGuide_id(null);
                }
            }
    
            return $this;
        }
}
