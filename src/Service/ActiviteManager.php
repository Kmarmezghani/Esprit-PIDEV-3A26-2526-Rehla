<?php

namespace App\Service;

use App\Entity\Activite;

class ActiviteManager
{
    public function validate(Activite $a): bool
    {
        if (empty($a->getNom())) {
            throw new \InvalidArgumentException('Le nom est obligatoire');
        }

        if ($a->getPrix() <= 0) {
            throw new \InvalidArgumentException('Le prix doit être supérieur à 0');
        }

        if ($a->getDateFin() <= $a->getDateDebut()) {
            throw new \InvalidArgumentException('La date de fin doit être après la date de début');
        }

        if ($a->getGuide() === null && $a->getMaxPlaces() !== 0) {
            throw new \InvalidArgumentException('Sans guide, max_places doit être 0');
        }

        return true;
    }
}