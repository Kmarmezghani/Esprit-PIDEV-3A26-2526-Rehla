<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;
use Doctrine\Common\Collections\Collection;
use App\Entity\Favoris_post;

#[ORM\Entity]
class Favoris
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $dateCreation;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "favoriss")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getDateCreation()
    {
        return $this->dateCreation;
    }

    public function setDateCreation($value)
    {
        $this->dateCreation = $value;
    }

    public function getPersonne_id()
    {
        return $this->personne_id;
    }

    public function setPersonne_id($value)
    {
        $this->personne_id = $value;
    }

    #[ORM\OneToMany(mappedBy: "favoris_id", targetEntity: Favoris_post::class)]
    private Collection $favoris_posts;

        public function getFavoris_posts(): Collection
        {
            return $this->favoris_posts;
        }
    
        public function addFavoris_post(Favoris_post $favoris_post): self
        {
            if (!$this->favoris_posts->contains($favoris_post)) {
                $this->favoris_posts[] = $favoris_post;
                $favoris_post->setFavoris_id($this);
            }
    
            return $this;
        }
    
        public function removeFavoris_post(Favoris_post $favoris_post): self
        {
            if ($this->favoris_posts->removeElement($favoris_post)) {
                // set the owning side to null (unless already changed)
                if ($favoris_post->getFavoris_id() === $this) {
                    $favoris_post->setFavoris_id(null);
                }
            }
    
            return $this;
        }
}
