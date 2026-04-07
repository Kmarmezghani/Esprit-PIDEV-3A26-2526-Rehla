<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;

#[ORM\Entity]
class Preference
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "float")]
    private float $budgetMin;

    #[ORM\Column(type: "float")]
    private float $budgetMax;

    #[ORM\Column(type: "text")]
    private string $typesVoyage;

    #[ORM\Column(type: "text")]
    private string $centresInteret;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "preferences")]
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

    public function getBudgetMin()
    {
        return $this->budgetMin;
    }

    public function setBudgetMin($value)
    {
        $this->budgetMin = $value;
    }

    public function getBudgetMax()
    {
        return $this->budgetMax;
    }

    public function setBudgetMax($value)
    {
        $this->budgetMax = $value;
    }

    public function getTypesVoyage()
    {
        return $this->typesVoyage;
    }

    public function setTypesVoyage($value)
    {
        $this->typesVoyage = $value;
    }

    public function getCentresInteret()
    {
        return $this->centresInteret;
    }

    public function setCentresInteret($value)
    {
        $this->centresInteret = $value;
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
