<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Post;
use Doctrine\Common\Collections\Collection;
use App\Entity\Notification;

#[ORM\Entity]
class Commentaire
{

    #[ORM\Id]
     #[ORM\GeneratedValue] 
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "text")]
    private string $contenu;

    #[ORM\Column(name: "dateCommentaire", type: "datetime")]
    private \DateTimeInterface $dateCommentaire;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "commentaires")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

        #[ORM\ManyToOne(targetEntity: Post::class, inversedBy: "commentaires")]
    #[ORM\JoinColumn(name: 'post_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Post $post_id;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getContenu()
    {
        return $this->contenu;
    }

    public function setContenu($value)
    {
        $this->contenu = $value;
    }

    public function getDateCommentaire()
    {
        return $this->dateCommentaire;
    }

    public function setDateCommentaire($value)
    {
        $this->dateCommentaire = $value;
    }

    public function getPersonne_id()
    {
        return $this->personne_id;
    }

    public function setPersonne_id($value)
    {
        $this->personne_id = $value;
    }

    public function getPost_id()
    {
        return $this->post_id;
    }

    public function setPost_id($value)
    {
        $this->post_id = $value;
    }

    #[ORM\OneToMany(mappedBy: "comment_id", targetEntity: Notification::class)]
    private Collection $notifications;

        public function getNotifications(): Collection
        {
            return $this->notifications;
        }
    
        public function addNotification(Notification $notification): self
        {
            if (!$this->notifications->contains($notification)) {
                $this->notifications[] = $notification;
                $notification->setComment_id($this);
            }
    
            return $this;
        }
    
        public function removeNotification(Notification $notification): self
        {
            if ($this->notifications->removeElement($notification)) {
                // set the owning side to null (unless already changed)
                if ($notification->getComment_id() === $this) {
                    $notification->setComment_id(null);
                }
            }
    
            return $this;
        }
}
