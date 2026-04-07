<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Post;

#[ORM\Entity]
class Likes
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "boolean")]
    private bool $statut;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "likess")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

        #[ORM\ManyToOne(targetEntity: Post::class, inversedBy: "likess")]
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

    public function getStatut()
    {
        return $this->statut;
    }

    public function setStatut($value)
    {
        $this->statut = $value;
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
}
