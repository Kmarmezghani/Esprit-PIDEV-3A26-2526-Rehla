<?php

namespace App\Tests\Service;

use App\Entity\Attraction;
use App\Entity\Ville;
use App\Service\AttractionValidator;
use PHPUnit\Framework\TestCase;

/**
 * Tests unitaires pour AttractionValidator
 * 
 * Règles métier testées :
 * - Règle 1 : Le prix ne doit pas être négatif
 * - Règle 2 : L'heure d'ouverture doit être avant l'heure de fermeture
 */
class AttractionValidatorTest extends TestCase
{
    private AttractionValidator $validator;

    protected function setUp(): void
    {
        $this->validator = new AttractionValidator();
    }

    /**
     * Test 1 : Prix positif → validation OK
     */
    public function testPrixPositif(): void
    {
        $attraction = $this->creerAttraction();
        $attraction->setPrix(25.50);

        $result = $this->validator->validerPrix($attraction);

        $this->assertTrue($result);
    }

    /**
     * Test 2 : Prix nul (gratuit) → validation OK
     */
    public function testPrixNul(): void
    {
        $attraction = $this->creerAttraction();
        $attraction->setPrix(0.0);

        $result = $this->validator->validerPrix($attraction);

        $this->assertTrue($result);
    }

    /**
     * Test 3 : Prix négatif → exception attendue
     * Règle métier : Le prix ne peut pas être négatif
     */
    public function testPrixNegatif(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le prix ne peut pas être négatif');

        $attraction = $this->creerAttraction();
        $attraction->setPrix(-10.00);

        $this->validator->validerPrix($attraction);
    }

    /**
     * Test 4 : Heures valides (ouverture < fermeture) → OK
     */
    public function testHeuresValides(): void
    {
        $attraction = $this->creerAttraction();
        $attraction->setHeureOuverture(new \DateTime('09:00'));
        $attraction->setHeureFermeture(new \DateTime('18:00'));

        $result = $this->validator->validerHeures($attraction);

        $this->assertTrue($result);
    }

    /**
     * Test 5 : Ouverture = fermeture → exception
     * Règle métier : L'ouverture doit être avant la fermeture
     */
    public function testHeuresIdentiques(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('L\'heure d\'ouverture doit être avant l\'heure de fermeture');

        $attraction = $this->creerAttraction();
        $attraction->setHeureOuverture(new \DateTime('12:00'));
        $attraction->setHeureFermeture(new \DateTime('12:00'));

        $this->validator->validerHeures($attraction);
    }

    /**
     * Test 6 : Ouverture après fermeture → exception
     * Règle métier : L'ouverture doit être avant la fermeture
     */
    public function testOuvertureApresFermeture(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('L\'heure d\'ouverture doit être avant l\'heure de fermeture');

        $attraction = $this->creerAttraction();
        $attraction->setHeureOuverture(new \DateTime('20:00'));
        $attraction->setHeureFermeture(new \DateTime('08:00'));

        $this->validator->validerHeures($attraction);
    }

    /**
     * Test 7 : Validation complète OK
     */
    public function testValidationCompleteOK(): void
    {
        $attraction = $this->creerAttraction();
        $attraction->setPrix(15.00);
        $attraction->setHeureOuverture(new \DateTime('09:00'));
        $attraction->setHeureFermeture(new \DateTime('17:00'));

        $result = $this->validator->validate($attraction);

        $this->assertTrue($result);
    }

    /**
     * Test 8 : Validation complète échoue (prix négatif)
     */
    public function testValidationCompleteEchouePrix(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le prix ne peut pas être négatif');

        $attraction = $this->creerAttraction();
        $attraction->setPrix(-5.00);
        $attraction->setHeureOuverture(new \DateTime('09:00'));
        $attraction->setHeureFermeture(new \DateTime('17:00'));

        $this->validator->validate($attraction);
    }

    /**
     * Helper : Crée une attraction minimale pour les tests
     */
    private function creerAttraction(): Attraction
    {
        $ville = new Ville();
        $ville->setNom('Test Ville');

        $attraction = new Attraction();
        $attraction->setNom('Test Attraction');
        $attraction->setType('Museum');
        $attraction->setPrix(0.0);
        $attraction->setVilleId($ville);

        return $attraction;
    }
}
