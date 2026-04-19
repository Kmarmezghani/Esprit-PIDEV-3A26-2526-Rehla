<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;

#[ORM\Entity]
class Groupe
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(type: "string", length: 100)]
    private string $nom;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $created_at;

    #[ORM\Column(nullable: true)]
    private ?string $image = null;

    // ------------------- Relation ManyToMany avec Personne -------------------
    #[ORM\ManyToMany(targetEntity: Personne::class)]
    #[ORM\JoinTable(name: "groupe_personne")]
    private Collection $membres;

    // ------------------- Relation OneToOne avec Conversation -------------------
    #[ORM\OneToOne(mappedBy: "groupe", targetEntity: Conversation::class, cascade: ["persist", "remove"])]
    private ?Conversation $conversation = null;

    // ------------------- Constructor -------------------
    public function __construct()
    {
        $this->membres = new ArrayCollection();
    }

    // ------------------- Getters & Setters -------------------
        public function getImage(): ?string
        {
            return $this->image;
        }

        public function setImage(?string $image): self
        {
            $this->image = $image;
            return $this;
        }
    public function getId(): ?int
    {
        return $this->id;
    }

    public function getNom(): string
    {
        return $this->nom;
    }

    public function setNom(string $nom): void
    {
        $this->nom = $nom;
    }

    public function getCreated_at(): \DateTimeInterface
    {
        return $this->created_at;
    }

    public function setCreated_at(\DateTimeInterface $created_at): void
    {
        $this->created_at = $created_at;
    }

    // ------------------- Membres -------------------

    public function getMembres(): Collection
    {
        return $this->membres;
    }

    public function addMembre(Personne $personne): self
    {
        if (!$this->membres->contains($personne)) {
            $this->membres[] = $personne;
        }
        return $this;
    }

    public function removeMembre(Personne $personne): self
    {
        $this->membres->removeElement($personne);
        return $this;
    }

    // ------------------- Conversation -------------------

    public function getConversation(): ?Conversation
    {
        return $this->conversation;
    }

    public function setConversation(?Conversation $conversation): void
    {
        $this->conversation = $conversation;

        if ($conversation && $conversation->getGroupe() !== $this) {
            $conversation->setGroupe($this);
        }
    }
}
