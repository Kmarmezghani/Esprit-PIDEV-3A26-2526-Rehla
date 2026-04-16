<?php

namespace App\Entity;

use App\Repository\WaitlistRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: WaitlistRepository::class)]
#[ORM\Table(name: 'waitlist')]
class Waitlist
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Activite::class, inversedBy: 'waitlists')]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?Activite $activite = null;

    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: 'waitlists')]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?Personne $personne = null;

    #[ORM\Column(type: 'string', length: 20, options: ['default' => 'WAITING'])]
    private ?string $status = 'WAITING';

    #[ORM\Column(name: 'created_at', type: 'datetime')]
    private ?\DateTimeInterface $createdAt = null;

    #[ORM\Column(name: 'hold_expires_at', type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $holdExpiresAt = null;

    #[ORM\Column(name: 'hold_token', type: 'string', length: 64, nullable: true)]
    private ?string $holdToken = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getActivite(): ?Activite
    {
        return $this->activite;
    }

    public function setActivite(?Activite $activite): self
    {
        $this->activite = $activite;
        return $this;
    }

    public function getPersonne(): ?Personne
    {
        return $this->personne;
    }

    public function setPersonne(?Personne $personne): self
    {
        $this->personne = $personne;
        return $this;
    }

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(string $status): self
    {
        $this->status = $status;
        return $this;
    }

    public function getCreatedAt(): ?\DateTimeInterface
    {
        return $this->createdAt;
    }

    public function setCreatedAt(\DateTimeInterface $createdAt): self
    {
        $this->createdAt = $createdAt;
        return $this;
    }

    public function getHoldExpiresAt(): ?\DateTimeInterface
    {
        return $this->holdExpiresAt;
    }

    public function setHoldExpiresAt(?\DateTimeInterface $holdExpiresAt): self
    {
        $this->holdExpiresAt = $holdExpiresAt;
        return $this;
    }

    public function getHoldToken(): ?string
    {
        return $this->holdToken;
    }

    public function setHoldToken(?string $holdToken): self
    {
        $this->holdToken = $holdToken;
        return $this;
    }
}