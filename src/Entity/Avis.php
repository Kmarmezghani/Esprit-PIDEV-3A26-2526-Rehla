<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;

#[ORM\Entity]
class Avis
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "integer")]
    private int $note;

    #[ORM\Column(type: "text")]
    private string $commentaire;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $dateAvis;

        #[ORM\ManyToOne(targetEntity: Activite::class, inversedBy: "aviss")]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Activite $activite_id;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "aviss")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getNote()
    {
        return $this->note;
    }

    public function setNote($value)
    {
        $this->note = $value;
    }

    public function getCommentaire()
    {
        return $this->commentaire;
    }

    public function setCommentaire($value)
    {
        $this->commentaire = $value;
    }

    public function getDateAvis()
    {
        return $this->dateAvis;
    }

    public function setDateAvis($value)
    {
        $this->dateAvis = $value;
    }

    public function getActivite_id()
    {
        return $this->activite_id;
    }

    public function setActivite_id($value)
    {
        $this->activite_id = $value;
    }

    public function getPersonne_id()
    {
        return $this->personne_id;
    }

    public function setPersonne_id($value)
    {
        $this->personne_id = $value;
    }
}
