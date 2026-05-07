<?php
namespace App\Tests\Service;

use App\Entity\Reservation;
use App\Entity\Ville;
use App\Entity\Ticket;
use App\Service\ReservationManager;
use PHPUnit\Framework\TestCase;

class ReservationManagerTest extends TestCase
{
    private function createValidReservation(): Reservation
    {
        $reservation = new Reservation();

        // Dates valides
        $reservation->setDateDebut(new \DateTime('+1 day'));
        $reservation->setDateFin(new \DateTime('+5 day'));

        // Destination fake
        $ville = new Ville();
        $ville->setNom("Paris");
        $reservation->setDestination($ville);

        // Ajouter au moins 1 ticket (Doctrine => addTicket)
        $ticket = new Ticket();
        $reservation->addTicket($ticket);

        return $reservation;
    }

    // ✅ TEST 1 : réservation valide
    public function testValidReservation()
    {
        $manager = new ReservationManager();
        $reservation = $this->createValidReservation();

        $this->assertTrue($manager->validate($reservation));
    }

    // ❌ TEST 2 : date fin < date debut
    public function testDateFinBeforeDateDebut()
    {
        $this->expectException(\InvalidArgumentException::class);

        $manager = new ReservationManager();
        $reservation = $this->createValidReservation();

        $reservation->setDateFin(new \DateTime('-1 day'));

        $manager->validate($reservation);
    }

    // ❌ TEST 3 : destination manquante
    public function testReservationWithoutDestination()
    {
        $this->expectException(\InvalidArgumentException::class);

        $manager = new ReservationManager();
        $reservation = $this->createValidReservation();

        $reservation->setDestination(null);

        $manager->validate($reservation);
    }

    // ❌ TEST 4 : aucun ticket
    public function testReservationWithoutTicket()
    {
        $this->expectException(\InvalidArgumentException::class);

        $manager = new ReservationManager();
        $reservation = $this->createValidReservation();

        // supprimer tous les tickets (Doctrine => removeTicket)
        foreach ($reservation->getTickets() as $ticket) {
            $reservation->removeTicket($ticket);
        }

        $manager->validate($reservation);
    }
}