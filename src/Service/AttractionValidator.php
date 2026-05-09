<?php

namespace App\Service;

use App\Entity\Attraction;

/**
 * Service de validation métier pour l'entité Attraction
 * 
 * Règles métier :
 * - Règle 1 : Le prix ne doit pas être négatif
 * - Règle 2 : L'heure d'ouverture doit être avant l'heure de fermeture
 */
class AttractionValidator
{
    /**
     * Règle 1 : Valide que le prix n'est pas négatif
     */
    public function validerPrix(Attraction $attraction): bool
    {
        $prix = $attraction->getPrix();

        if ($prix !== null && $prix < 0) {
            throw new \InvalidArgumentException(
                'Le prix ne peut pas être négatif'
            );
        }

        return true;
    }

    /**
     * Règle 2 : Valide que l'heure d'ouverture est avant l'heure de fermeture
     */
    public function validerHeures(Attraction $attraction): bool
    {
        $ouverture = $attraction->getHeureOuverture();
        $fermeture = $attraction->getHeureFermeture();

        if ($ouverture !== null && $fermeture !== null && $ouverture >= $fermeture) {
            throw new \InvalidArgumentException(
                'L\'heure d\'ouverture doit être avant l\'heure de fermeture'
            );
        }

        return true;
    }

    /**
     * Valide complètement une attraction (toutes les règles)
     */
    public function validate(Attraction $attraction): bool
    {
        $this->validerPrix($attraction);
        $this->validerHeures($attraction);

        return true;
    }
}
