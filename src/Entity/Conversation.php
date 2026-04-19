<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use App\Entity\Personne;

#[ORM\Entity]
class Conversation
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "conversationsUser1")]
    #[ORM\JoinColumn(name: 'user1_id', referencedColumnName: 'id', onDelete: 'CASCADE', nullable: true)]
    private ?Personne $user1_id = null;

    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "conversationsUser2")]
    #[ORM\JoinColumn(name: 'user2_id', referencedColumnName: 'id', onDelete: 'CASCADE', nullable: true)]
    private ?Personne $user2_id = null;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $created_at;

    #[ORM\OneToOne(inversedBy: "conversation", targetEntity: Groupe::class)]
    #[ORM\JoinColumn(name: "groupe_id", referencedColumnName: "id", onDelete: "CASCADE", nullable: true)]
    private ?Groupe $groupe = null;    


    // ------------------- Getters & Setters -------------------

        public function getGroupe(): ?Groupe
        {
            return $this->groupe;
        }

        public function setGroupe(?Groupe $groupe): self
        {
            $this->groupe = $groupe;
            return $this;
        }




    public function getId(): ?int
    {
        return $this->id;
    }

    public function getUser1_id(): ?Personne
    {
        return $this->user1_id;
    }

    public function setUser1_id(?Personne $user1_id): self
    {
        $this->user1_id = $user1_id;
        return $this;
    }

    public function getUser2_id(): ?Personne
    {
        return $this->user2_id;
    }

    public function setUser2_id(?Personne $user2_id): self
    {
        $this->user2_id = $user2_id;
        return $this;
    }

    public function getCreated_at(): \DateTimeInterface
    {
        return $this->created_at;
    }

    public function setCreated_at(\DateTimeInterface $created_at): self
    {
        $this->created_at = $created_at;
        return $this;
    }
}