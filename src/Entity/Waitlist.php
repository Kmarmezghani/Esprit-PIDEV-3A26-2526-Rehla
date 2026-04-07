<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;

#[ORM\Entity]
class Waitlist
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

        #[ORM\ManyToOne(targetEntity: Activite::class, inversedBy: "waitlists")]
    #[ORM\JoinColumn(name: 'activite_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Activite $activite_id;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "waitlists")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

    #[ORM\Column(type: "string")]
    private string $status;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $created_at;

    #[ORM\Column(type: "datetime")]
    private \DateTimeInterface $hold_expires_at;

    #[ORM\Column(type: "string", length: 64)]
    private string $hold_token;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
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

    public function getStatus()
    {
        return $this->status;
    }

    public function setStatus($value)
    {
        $this->status = $value;
    }

    public function getCreated_at()
    {
        return $this->created_at;
    }

    public function setCreated_at($value)
    {
        $this->created_at = $value;
    }

    public function getHold_expires_at()
    {
        return $this->hold_expires_at;
    }

    public function setHold_expires_at($value)
    {
        $this->hold_expires_at = $value;
    }

    public function getHold_token()
    {
        return $this->hold_token;
    }

    public function setHold_token($value)
    {
        $this->hold_token = $value;
    }
}
