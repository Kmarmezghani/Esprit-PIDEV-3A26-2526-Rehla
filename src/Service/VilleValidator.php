<?php

namespace App\Service;

use App\Entity\Ville;

/**
 * Service de validation métier pour l'entité Ville
 * 
 * Valide les règles métier spécifiques qui ne peuvent pas être
 * testées simplement avec les Assert de Symfony
 */
class VilleValidator
{
    /**
     * Valide les coordonnées géographiques d'une ville
     * 
     * Règle 1: La latitude doit être comprise entre -90 et 90
     * Règle 2: La longitude doit être comprise entre -180 et 180
     * 
     * @param Ville $ville La ville à valider
     * @return bool true si valide
     * @throws \InvalidArgumentException si une règle est violée
     */
    public function validerCoordonnees(Ville $ville): bool
    {
        $latitude = $ville->getLatitude();
        $longitude = $ville->getLongitude();

        // Règle 1: Latitude invalide
        if ($latitude !== null && ($latitude < -90 || $latitude > 90)) {
            throw new \InvalidArgumentException(
                sprintf(
                    'La latitude doit être comprise entre -90 et 90. Valeur fournie: %.2f',
                    $latitude
                )
            );
        }

        // Règle 2: Longitude invalide
        if ($longitude !== null && ($longitude < -180 || $longitude > 180)) {
            throw new \InvalidArgumentException(
                sprintf(
                    'La longitude doit être comprise entre -180 et 180. Valeur fournie: %.2f',
                    $longitude
                )
            );
        }

        return true;
    }

    /**
     * Valide la cohérence entre le type de tourisme et la saison
     * 
     * Règle 3: Une destination balnéaire (Seaside) ne doit pas avoir la saison Winter
     * 
     * @param Ville $ville La ville à valider
     * @return bool true si valide
     * @throws \InvalidArgumentException si une règle est violée
     */
    public function validerCohérenceSaisonType(Ville $ville): bool
    {
        $typeTourisme = $ville->getTypeTourisme();
        $saison = $ville->getSaison();

        // Règle 3: Station balnéaire en hiver? Pas logique!
        if ($typeTourisme === 'Seaside' && $saison === 'Winter') {
            throw new \InvalidArgumentException(
                'Une destination balnéaire ne peut pas être recommandée en hiver'
            );
        }

        return true;
    }

    /**
     * Valide complètement une ville (toutes les règles)
     * 
     * @param Ville $ville La ville à valider
     * @return bool true si toutes les règles sont respectées
     * @throws \InvalidArgumentException si au moins une règle est violée
     */
    public function validate(Ville $ville): bool
    {
        $this->validerCoordonnees($ville);
        $this->validerCohérenceSaisonType($ville);

        return true;
    }
}
