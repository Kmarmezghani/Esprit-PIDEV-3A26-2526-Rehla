<?php

namespace App\Tests\Service;

use App\Entity\Personne;
use App\Service\UserManager;
use PHPUnit\Framework\TestCase;

class UserManagerTest extends TestCase
{
    private function createValidPersonne(): Personne
    {
        $p = new Personne();
        $p->setNom('El Ayech');
        $p->setPrenom('Rayen');
        $p->setEmail('rayen@rehla.com');
        $p->setMotDePasse('1234');
        $p->setRole('CLIENT');
        $p->setStatutCompte('ACTIF');
        return $p;
    }

    // ✅ TEST 1 : compte valide
    public function testCompteValide(): void
    {
        $manager = new UserManager();
        $this->assertTrue($manager->validate($this->createValidPersonne()));
    }

    // ❌ TEST 2 : nom vide
    public function testNomVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setNom('');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 3 : prénom vide
    public function testPrenomVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setPrenom('');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 4 : email invalide
    public function testEmailInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setEmail('pas-un-email');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 5 : email vide
    public function testEmailVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setEmail('');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 6 : mot de passe trop court
    public function testMotDePasseTropCourt(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setMotDePasse('abc');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 7 : mot de passe vide
    public function testMotDePasseVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setMotDePasse('');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 8 : rôle invalide
    public function testRoleInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setRole('SUPERADMIN');

        (new UserManager())->validate($p);
    }

    // ❌ TEST 9 : statut invalide
    public function testStatutInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $p = $this->createValidPersonne();
        $p->setStatutCompte('BANNI');

        (new UserManager())->validate($p);
    }

    // ✅ TEST 10 : rôle GUIDE accepté
    public function testRoleGuideValide(): void
    {
        $p = $this->createValidPersonne();
        $p->setRole('GUIDE');

        $this->assertTrue((new UserManager())->validate($p));
    }

    // ✅ TEST 11 : statut SUSPENDU accepté
    public function testStatutSuspenduValide(): void
    {
        $p = $this->createValidPersonne();
        $p->setStatutCompte('SUSPENDU');

        $this->assertTrue((new UserManager())->validate($p));
    }

    // ✅ TEST 12 : statut INACTIF accepté
    public function testStatutInactifValide(): void
    {
        $p = $this->createValidPersonne();
        $p->setStatutCompte('INACTIF');

        $this->assertTrue((new UserManager())->validate($p));
    }
}
