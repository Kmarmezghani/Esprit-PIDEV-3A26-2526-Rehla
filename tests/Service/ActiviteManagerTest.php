<?php

namespace App\Tests\Service;

use App\Entity\Activite;
use App\Service\ActiviteManager;
use PHPUnit\Framework\TestCase;

class ActiviteManagerTest extends TestCase
{
    public function testActiviteValide(): void
    {
        $a = new Activite();
        $a->setNom("Randonnée");
        $a->setPrix(100);
        $a->setDateDebut(new \DateTime('2026-06-01'));
        
        $a->setDateFin(new \DateTime('2026-06-02'));
        $a->setGuide(null);
        $a->setMaxPlaces(0);

        $manager = new ActiviteManager();

        $this->assertTrue($manager->validate($a));
    }

    public function testNomVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $a = new Activite();
        $a->setNom("");
        $a->setPrix(100);
        $a->setDateDebut(new \DateTime('2026-06-01'));
        $a->setDateFin(new \DateTime('2026-06-02'));
        $a->setGuide(null);
        $a->setMaxPlaces(0);

        $manager = new ActiviteManager();
        $manager->validate($a);
    }

    public function testPrixInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $a = new Activite();
        $a->setNom("Test");
        $a->setPrix(0);
        $a->setDateDebut(new \DateTime('2026-06-01'));
        $a->setDateFin(new \DateTime('2026-06-02'));
        $a->setGuide(null);
        $a->setMaxPlaces(0);

        $manager = new ActiviteManager();
        $manager->validate($a);
    }

    public function testDateInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $a = new Activite();
        $a->setNom("Test");
        $a->setPrix(100);
        $a->setDateDebut(new \DateTime('2026-06-02'));
        $a->setDateFin(new \DateTime('2026-06-01'));
        $a->setGuide(null);
        $a->setMaxPlaces(0);

        $manager = new ActiviteManager();
        $manager->validate($a);
    }

    public function testGuideNullPlaces(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $a = new Activite();
        $a->setNom("Test");
        $a->setPrix(100);
        $a->setDateDebut(new \DateTime('2026-06-01'));
        $a->setDateFin(new \DateTime('2026-06-02'));
        $a->setGuide(null);
        $a->setMaxPlaces(5);

        $manager = new ActiviteManager();
        $manager->validate($a);
    }
}