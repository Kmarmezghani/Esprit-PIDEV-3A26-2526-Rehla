<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;

#[ORM\Entity]
class Notification
{

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "text")]
    private string $message;

    #[ORM\Column(type: "string")]
    private string $type;

        #[ORM\ManyToOne(targetEntity: Post::class, inversedBy: "notifications")]
    #[ORM\JoinColumn(name: 'post_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Post $post_id;

        #[ORM\ManyToOne(targetEntity: Commentaire::class, inversedBy: "notifications")]
    #[ORM\JoinColumn(name: 'comment_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Commentaire $comment_id;

        #[ORM\ManyToOne(targetEntity: Activite::class, inversedBy: "notifications")]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Activite $activite_id;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "notifications")]
    #[ORM\JoinColumn(name: 'sender_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $sender_id;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "notifications")]
    #[ORM\JoinColumn(name: 'receiver_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $receiver_id;

    #[ORM\Column(type: "boolean")]
    private bool $is_read;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $created_at;

    #[ORM\Column(type: "boolean")]
    private bool $is_sent_sms;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getMessage()
    {
        return $this->message;
    }

    public function setMessage($value)
    {
        $this->message = $value;
    }

    public function getType()
    {
        return $this->type;
    }

    public function setType($value)
    {
        $this->type = $value;
    }

    public function getPost_id()
    {
        return $this->post_id;
    }

    public function setPost_id($value)
    {
        $this->post_id = $value;
    }

    public function getComment_id()
    {
        return $this->comment_id;
    }

    public function setComment_id($value)
    {
        $this->comment_id = $value;
    }

    public function getActivite_id()
    {
        return $this->activite_id;
    }

    public function setActivite_id($value)
    {
        $this->activite_id = $value;
    }

    public function getSender_id()
    {
        return $this->sender_id;
    }

    public function setSender_id($value)
    {
        $this->sender_id = $value;
    }

    public function getReceiver_id()
    {
        return $this->receiver_id;
    }

    public function setReceiver_id($value)
    {
        $this->receiver_id = $value;
    }

    public function getIs_read()
    {
        return $this->is_read;
    }

    public function setIs_read($value)
    {
        $this->is_read = $value;
    }

    public function getCreated_at()
    {
        return $this->created_at;
    }

    public function setCreated_at($value)
    {
        $this->created_at = $value;
    }

    public function getIs_sent_sms()
    {
        return $this->is_sent_sms;
    }

    public function setIs_sent_sms($value)
    {
        $this->is_sent_sms = $value;
    }
}
