<?php
namespace App\Service;

use App\Entity\Reservation;

class ReservationManager
{
    public function validate(Reservation $reservation): bool
    {
        // 1️⃣ Dates obligatoires
        if (!$reservation->getDateDebut() || !$reservation->getDateFin()) {
            throw new \InvalidArgumentException("Les dates sont obligatoires");
        }

        // 2️⃣ Date fin > date debut
        if ($reservation->getDateFin() <= $reservation->getDateDebut()) {
            throw new \InvalidArgumentException("Date fin invalide");
        }

        // 3️⃣ Destination obligatoire
        if (!$reservation->getDestination()) {
            throw new \InvalidArgumentException("Destination obligatoire");
        }

        // 4️⃣ Au moins 1 ticket
        if ($reservation->getTickets()->count() === 0) {
            throw new \InvalidArgumentException("Au moins un ticket requis");
        }

        return true;
    }
}