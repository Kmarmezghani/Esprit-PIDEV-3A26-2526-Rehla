<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Post;

#[ORM\Entity]
class Favoris_post
{

    #[ORM\Id]
        #[ORM\ManyToOne(targetEntity: Favoris::class, inversedBy: "favoris_posts")]
    #[ORM\JoinColumn(name: 'favoris_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Favoris $favoris_id;

    #[ORM\Id]
        #[ORM\ManyToOne(targetEntity: Post::class, inversedBy: "favoris_posts")]
    #[ORM\JoinColumn(name: 'post_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Post $post_id;

    #[ORM\Column(name: "dateAjout",type: "datetime")]
    private \DateTimeInterface $dateAjout;

    public function getFavoris_id()
    {
        return $this->favoris_id;
    }

    public function setFavoris_id($value)
    {
        $this->favoris_id = $value;
    }

    public function getPost_id()
    {
        return $this->post_id;
    }

    public function setPost_id($value)
    {
        $this->post_id = $value;
    }

    public function getDateAjout()
    {
        return $this->dateAjout;
    }

    public function setDateAjout($value)
    {
        $this->dateAjout = $value;
    }
}
