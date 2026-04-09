<?php

namespace App\Service;

use App\Entity\Activite;

class AvisService
{
    public function recalculerNoteMoyenne(Activite $activite): void
    {
        $avisList = $activite->getAviss();

        if ($avisList->isEmpty()) {
            $activite->setNoteMoyenne(0);
            return;
        }

        $total = 0;
        $count = count($avisList);

        foreach ($avisList as $avis) {
            $total += $avis->getNote();
        }

        $activite->setNoteMoyenne($total / $count);
    }
}