<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Ville;
use Doctrine\Common\Collections\Collection;
use App\Entity\Notification;

#[ORM\Entity]
class Activite
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "string", length: 150)]
    private string $nom;

    #[ORM\Column(type: "text")]
    private string $description;

    #[ORM\Column(type: "float")]
    private float $prix;

    #[ORM\Column(name: "typeActivite", type: "string", length: 100)]
    private string $typeActivite;

    #[ORM\Column(name: "noteMoyenne", type: "float")]
    private float $noteMoyenne;

        #[ORM\ManyToOne(targetEntity: Guide::class, inversedBy: "activites")]
    #[ORM\JoinColumn(name: 'guide_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Guide $guide_id;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $date_debut;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $date_fin;

    #[ORM\Column(type: "string")]
    private string $status;

        #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "activites")]
    #[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Ville $destination_id;

    #[ORM\Column(type: "integer")]
    private int $max_places;

    #[ORM\Column(type: "string", length: 255)]
    private string $image;

    #[ORM\Column(type: "boolean")]
    private bool $is_flash_sale;

    #[ORM\Column(type: "float")]
    private float $flash_price;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $flash_expires_at;

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

    public function getDescription()
    {
        return $this->description;
    }

    public function setDescription($value)
    {
        $this->description = $value;
    }

    public function getPrix()
    {
        return $this->prix;
    }

    public function setPrix($value)
    {
        $this->prix = $value;
    }

    public function getTypeActivite()
    {
        return $this->typeActivite;
    }

    public function setTypeActivite($value)
    {
        $this->typeActivite = $value;
    }

    public function getNoteMoyenne()
    {
        return $this->noteMoyenne;
    }

    public function setNoteMoyenne($value)
    {
        $this->noteMoyenne = $value;
    }

    public function getGuide_id()
    {
        return $this->guide_id;
    }

    public function setGuide_id($value)
    {
        $this->guide_id = $value;
    }

    public function getDate_debut()
    {
        return $this->date_debut;
    }

    public function setDate_debut($value)
    {
        $this->date_debut = $value;
    }

    public function getDate_fin()
    {
        return $this->date_fin;
    }

    public function setDate_fin($value)
    {
        $this->date_fin = $value;
    }

    public function getStatus()
    {
        return $this->status;
    }

    public function setStatus($value)
    {
        $this->status = $value;
    }

    public function getDestination_id()
    {
        return $this->destination_id;
    }

    public function setDestination_id($value)
    {
        $this->destination_id = $value;
    }

    public function getMax_places()
    {
        return $this->max_places;
    }

    public function setMax_places($value)
    {
        $this->max_places = $value;
    }

    public function getImage()
    {
        return $this->image;
    }

    public function setImage($value)
    {
        $this->image = $value;
    }

    public function getIs_flash_sale()
    {
        return $this->is_flash_sale;
    }

    public function setIs_flash_sale($value)
    {
        $this->is_flash_sale = $value;
    }

    public function getFlash_price()
    {
        return $this->flash_price;
    }

    public function setFlash_price($value)
    {
        $this->flash_price = $value;
    }

    public function getFlash_expires_at()
    {
        return $this->flash_expires_at;
    }

    public function setFlash_expires_at($value)
    {
        $this->flash_expires_at = $value;
    }

    #[ORM\OneToMany(mappedBy: "activite_id", targetEntity: Avis::class)]
    private Collection $aviss;

        public function getAviss(): Collection
        {
            return $this->aviss;
        }
    
        public function addAvis(Avis $avis): self
        {
            if (!$this->aviss->contains($avis)) {
                $this->aviss[] = $avis;
                $avis->setActivite_id($this);
            }
    
            return $this;
        }
    
        public function removeAvis(Avis $avis): self
        {
            if ($this->aviss->removeElement($avis)) {
                // set the owning side to null (unless already changed)
                if ($avis->getActivite_id() === $this) {
                    $avis->setActivite_id(null);
                }
            }
    
            return $this;
        }

    #[ORM\OneToMany(mappedBy: "activite_id", targetEntity: Waitlist::class)]
    private Collection $waitlists;

    #[ORM\OneToMany(mappedBy: "activite_id", targetEntity: Ticket::class)]
    private Collection $tickets;

    #[ORM\OneToMany(mappedBy: "activite_id", targetEntity: Notification::class)]
    private Collection $notifications;
}
