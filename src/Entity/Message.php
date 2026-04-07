<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use App\Entity\Personne;
use App\Entity\Conversation;

#[ORM\Entity]
class Message
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Conversation::class)]
    #[ORM\JoinColumn(name: "conversation_id", referencedColumnName: "id", onDelete: "CASCADE")]
    private Conversation $conversation;

    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "messages")]
    #[ORM\JoinColumn(name: 'sender_id', referencedColumnName: 'id', onDelete: 'CASCADE', nullable: true)]
    private ?Personne $sender_id = null;

    #[ORM\Column(type: "text")]
    private string $contenu;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $sent_at;

    #[ORM\Column(type: "boolean")]
    private bool $is_read;

    // ------------------- Getters & Setters -------------------
    public function getId(): ?int
    {
        return $this->id;
    }

    public function setId(?int $value): void
    {
        $this->id = $value;
    }

    public function getConversation(): Conversation
    {
        return $this->conversation;
    }

    public function setConversation(Conversation $conversation): void
    {
        $this->conversation = $conversation;
    }

    public function getSender_id(): ?Personne
    {
        return $this->sender_id;
    }

    public function setSender_id(?Personne $sender_id): void
    {
        $this->sender_id = $sender_id;
    }

    public function getContenu(): string
    {
        return $this->contenu;
    }

    public function setContenu(string $contenu): void
    {
        $this->contenu = $contenu;
    }

    public function getSent_at(): \DateTimeInterface
    {
        return $this->sent_at;
    }

    public function setSent_at(\DateTimeInterface $sent_at): void
    {
        $this->sent_at = $sent_at;
    }

    public function getIs_read(): bool
    {
        return $this->is_read;
    }

    public function setIs_read(bool $is_read): void
    {
        $this->is_read = $is_read;
    }
}