<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use App\Entity\Personne;
use Doctrine\Common\Collections\Collection;
use App\Entity\Notification;
use App\Repository\PostRepository;
#[ORM\Entity(repositoryClass: PostRepository::class)]
class Post
{

   #[ORM\Id]
#[ORM\GeneratedValue]
#[ORM\Column(type: "integer")]
private ?int $id = null;

    #[ORM\Column(type: "string", length: 150)]
    private string $titre;

    #[ORM\Column(type: "text")]
    private string $contenu;

    #[ORM\Column(name: "datePublication", type: "datetime")]
    private \DateTimeInterface $datePublication;

    #[ORM\Column(type: "integer")]
    private int $popularite;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "posts")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private ?string $image = null;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getTitre()
    {
        return $this->titre;
    }

    public function setTitre($value)
    {
        $this->titre = $value;
    }

    public function getContenu()
    {
        return $this->contenu;
    }

    public function setContenu($value)
    {
        $this->contenu = $value;
    }

    public function getDatePublication()
    {
        return $this->datePublication;
    }

    public function setDatePublication($value)
    {
        $this->datePublication = $value;
    }

    public function getPopularite()
    {
        return $this->popularite;
    }

    public function setPopularite($value)
    {
        $this->popularite = $value;
    }

    public function getPersonne_id()
    {
        return $this->personne_id;
    }

    public function setPersonne_id($value)
    {
        $this->personne_id = $value;
    }

    public function getImage()
    {
        return $this->image;
    }

    public function setImage($value)
    {
        $this->image = $value;
    }

    #[ORM\OneToMany(mappedBy: "post_id", targetEntity: Commentaire::class)]
    private Collection $commentaires;

        public function getCommentaires(): Collection
        {
            return $this->commentaires;
        }
    
        public function addCommentaire(Commentaire $commentaire): self
        {
            if (!$this->commentaires->contains($commentaire)) {
                $this->commentaires[] = $commentaire;
                $commentaire->setPost_id($this);
            }
    
            return $this;
        }
    
        public function removeCommentaire(Commentaire $commentaire): self
        {
            if ($this->commentaires->removeElement($commentaire)) {
                // set the owning side to null (unless already changed)
                if ($commentaire->getPost_id() === $this) {
                    $commentaire->setPost_id(null);
                }
            }
    
            return $this;
        }

    #[ORM\OneToMany(mappedBy: "post_id", targetEntity: Favoris_post::class)]
    private Collection $favoris_posts;

    #[ORM\OneToMany(mappedBy: "post_id", targetEntity: Likes::class)]
    private Collection $likess;

    #[ORM\OneToMany(mappedBy: "post_id", targetEntity: Notification::class)]
    private Collection $notifications;
}
