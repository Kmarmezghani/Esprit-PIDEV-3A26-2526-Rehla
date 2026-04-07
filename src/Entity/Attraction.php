<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Ville;

#[ORM\Entity]
class Attraction
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(type: "string", length: 100)]
    private string $nom;

    #[ORM\Column(type: "text")]
    private string $description;

    #[ORM\Column(type: "string")]
    private string $type;

    #[ORM\Column(type: "float")]
    private float $prix;

        #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "attractions")]
    #[ORM\JoinColumn(name: 'ville_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Ville $ville_id;

    #[ORM\Column(type: "string")]
    private string $heure_ouverture;

    #[ORM\Column(type: "string")]
    private string $heure_fermeture;

    #[ORM\Column(type: "boolean")]
    private bool $est_ferme;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getNom()
    {
        return $this->nom;
    }

    public function setNom($value)
    {
        $this->nom = $value;
    }

    public function getDescription()
    {
        return $this->description;
    }

    public function setDescription($value)
    {
        $this->description = $value;
    }

    public function getType()
    {
        return $this->type;
    }

    public function setType($value)
    {
        $this->type = $value;
    }

    public function getPrix()
    {
        return $this->prix;
    }

    public function setPrix($value)
    {
        $this->prix = $value;
    }

    public function getVille_id()
    {
        return $this->ville_id;
    }

    public function setVille_id($value)
    {
        $this->ville_id = $value;
    }

    public function getHeure_ouverture()
    {
        return $this->heure_ouverture;
    }

    public function setHeure_ouverture($value)
    {
        $this->heure_ouverture = $value;
    }

    public function getHeure_fermeture()
    {
        return $this->heure_fermeture;
    }

    public function setHeure_fermeture($value)
    {
        $this->heure_fermeture = $value;
    }

    public function getEst_ferme()
    {
        return $this->est_ferme;
    }

    public function setEst_ferme($value)
    {
        $this->est_ferme = $value;
    }
}
