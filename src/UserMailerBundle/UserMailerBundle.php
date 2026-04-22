<?php

namespace App\UserMailerBundle;

use Symfony\Component\HttpKernel\Bundle\Bundle;

/**
 * Bundle centralisé pour tous les emails du module Gestion Utilisateurs.
 * Gère : email de bienvenue, reset mot de passe, suspension de compte, relance d'inactivité.
 */
class UserMailerBundle extends Bundle
{
}
