<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: \App\Repository\NotificationRepository::class)]
#[ORM\Table(name: 'notification')]
class Notification
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null;

    #[ORM\Column(type: 'text')]
    private ?string $message = null;

    #[ORM\Column(type: 'string', length: 50)]
    private ?string $type = null;

    #[ORM\ManyToOne(targetEntity: Post::class)]
    #[ORM\JoinColumn(name: 'post_id', referencedColumnName: 'id', nullable: true, onDelete: 'CASCADE')]
    private ?Post $post_id = null;

    #[ORM\ManyToOne(targetEntity: Commentaire::class)]
    #[ORM\JoinColumn(name: 'comment_id', referencedColumnName: 'id', nullable: true, onDelete: 'CASCADE')]
    private ?Commentaire $comment_id = null;

    #[ORM\ManyToOne(targetEntity: Activite::class)]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', nullable: true, onDelete: 'CASCADE')]
    private ?Activite $activite_id = null;

    #[ORM\ManyToOne(targetEntity: Personne::class)]
    #[ORM\JoinColumn(name: 'sender_id', referencedColumnName: 'id', nullable: true, onDelete: 'CASCADE')]
    private ?Personne $sender_id = null;

    #[ORM\ManyToOne(targetEntity: Personne::class)]
    #[ORM\JoinColumn(name: 'receiver_id', referencedColumnName: 'id', nullable: true, onDelete: 'CASCADE')]
    private ?Personne $receiver_id = null;

    #[ORM\Column(type: 'boolean', options: ['default' => false])]
    private bool $is_read = false;

    #[ORM\Column(type: 'datetime')]
    private ?\DateTimeInterface $created_at = null;

    #[ORM\Column(type: 'boolean', options: ['default' => false])]
    private bool $is_sent_sms = false;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getMessage(): ?string
    {
        return $this->message;
    }

    public function setMessage(?string $value): self
    {
        $this->message = $value;
        return $this;
    }

    public function getType(): ?string
    {
        return $this->type;
    }

    public function setType(?string $value): self
    {
        $this->type = $value;
        return $this;
    }

    public function getPost_id(): ?Post
    {
        return $this->post_id;
    }

    public function setPost_id(?Post $value): self
    {
        $this->post_id = $value;
        return $this;
    }

    public function getComment_id(): ?Commentaire
    {
        return $this->comment_id;
    }

    public function setComment_id(?Commentaire $value): self
    {
        $this->comment_id = $value;
        return $this;
    }

    public function getActivite_id(): ?Activite
    {
        return $this->activite_id;
    }

    public function setActivite_id(?Activite $value): self
    {
        $this->activite_id = $value;
        return $this;
    }

    public function getSender_id(): ?Personne
    {
        return $this->sender_id;
    }

    public function setSender_id(?Personne $value): self
    {
        $this->sender_id = $value;
        return $this;
    }

    public function getReceiver_id(): ?Personne
    {
        return $this->receiver_id;
    }

    public function setReceiver_id(?Personne $value): self
    {
        $this->receiver_id = $value;
        return $this;
    }

    public function getIs_read(): bool
    {
        return $this->is_read;
    }

    public function setIs_read(bool $value): self
    {
        $this->is_read = $value;
        return $this;
    }

    public function getCreated_at(): ?\DateTimeInterface
    {
        return $this->created_at;
    }

    public function setCreated_at(?\DateTimeInterface $value): self
    {
        $this->created_at = $value;
        return $this;
    }

    public function getIs_sent_sms(): bool
    {
        return $this->is_sent_sms;
    }

    public function setIs_sent_sms(bool $value): self
    {
        $this->is_sent_sms = $value;
        return $this;
    }
}