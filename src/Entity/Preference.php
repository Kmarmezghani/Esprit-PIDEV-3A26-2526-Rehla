<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Personne;

#[ORM\Entity]
class Preference
{

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(name: "budgetMin", type: "float", nullable: true)]
    private ?float $budgetMin = null;

    #[ORM\Column(name: "budgetMax", type: "float", nullable: true)]
    private ?float $budgetMax = null;

    #[ORM\Column(name: "typesVoyage", type: "text", nullable: true)]
    private ?string $typesVoyage = null;

    #[ORM\Column(name: "centresInteret", type: "text", nullable: true)]
    private ?string $centresInteret = null;

    #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "preferences")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE', nullable: true)]
    private ?Personne $personne_id = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getBudgetMin(): ?float
    {
        return $this->budgetMin;
    }

    public function setBudgetMin(?float $value): void
    {
        $this->budgetMin = $value;
    }

    public function getBudgetMax(): ?float
    {
        return $this->budgetMax;
    }

    public function setBudgetMax(?float $value): void
    {
        $this->budgetMax = $value;
    }

    public function getTypesVoyage(): ?string
    {
        return $this->typesVoyage;
    }

    public function setTypesVoyage(?string $value): void
    {
        $this->typesVoyage = $value;
    }

    public function getCentresInteret(): ?string
    {
        return $this->centresInteret;
    }

    public function setCentresInteret(?string $value): void
    {
        $this->centresInteret = $value;
    }

    public function getPersonne_id(): ?Personne
    {
        return $this->personne_id;
    }

    public function setPersonne_id(?Personne $value): void
    {
        $this->personne_id = $value;
    }
}
