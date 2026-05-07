<?php

namespace App\Service;

use App\Entity\Personne;

class UserManager
{
    private const ROLES_VALIDES   = ['CLIENT', 'GUIDE', 'ADMIN'];
    private const STATUTS_VALIDES = ['ACTIF', 'INACTIF', 'SUSPENDU'];

    public function validate(Personne $personne): bool
    {
        if (empty(trim($personne->getNom()))) {
            throw new \InvalidArgumentException('Le nom est obligatoire.');
        }

        if (empty(trim($personne->getPrenom()))) {
            throw new \InvalidArgumentException('Le prénom est obligatoire.');
        }

        if (empty($personne->getEmail()) || !filter_var($personne->getEmail(), FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException('L\'adresse email est invalide.');
        }

        if (empty($personne->getMotDePasse()) || strlen($personne->getMotDePasse()) < 4) {
            throw new \InvalidArgumentException('Le mot de passe doit contenir au moins 4 caractères.');
        }

        if (!in_array($personne->getRole(), self::ROLES_VALIDES, true)) {
            throw new \InvalidArgumentException('Le rôle est invalide. Valeurs acceptées : CLIENT, GUIDE, ADMIN.');
        }

        if (!in_array($personne->getStatutCompte(), self::STATUTS_VALIDES, true)) {
            throw new \InvalidArgumentException('Le statut du compte est invalide. Valeurs acceptées : ACTIF, INACTIF, SUSPENDU.');
        }

        return true;
    }
}
