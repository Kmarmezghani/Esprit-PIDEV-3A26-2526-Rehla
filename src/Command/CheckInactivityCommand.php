<?php

namespace App\Command;

use App\Entity\Personne;
use App\UserMailerBundle\Service\UserMailerService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:check-inactivity',
    description: 'Détecte les comptes inactifs, les passe à INACTIF et envoie un email de relance.',
)]
class CheckInactivityCommand extends Command
{
    public function __construct(
        private EntityManagerInterface $em,
        private UserMailerService $mailer
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this->addOption(
            'seuil',
            null,
            InputOption::VALUE_OPTIONAL,
            'Nombre de jours d\'inactivité avant marquage (défaut: 30)',
            30
        );
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $seuilJours = (int) $input->getOption('seuil');
        $limite     = new \DateTime("-{$seuilJours} days");
        $now        = new \DateTime();

        $io->title('Vérification des comptes inactifs');
        $io->text("Seuil : {$seuilJours} jours | Date limite : " . $limite->format('d/m/Y H:i'));

        $users = $this->em->getRepository(Personne::class)->findAll();
        $count = 0;
        $rows  = [];

        foreach ($users as $personne) {
            if ($personne->getRole() === 'ADMIN') continue;
            if ($personne->getStatutCompte() !== 'ACTIF') continue;

            $derniereConnexion = $personne->getDerniereConnexion()
                ?? $personne->getDateInscription();

            if ($derniereConnexion && $derniereConnexion < $limite) {
                $joursInactif = (int) $derniereConnexion->diff($now)->days;

                $personne->setStatutCompte('INACTIF');
                $this->em->flush();

                try {
                    $this->mailer->sendInactivityEmail($personne, $joursInactif);
                    $emailStatus = 'Email envoyé';
                } catch (\Throwable $e) {
                    $emailStatus = 'Erreur email: ' . $e->getMessage();
                }

                $rows[] = [
                    $personne->getId(),
                    $personne->getNom() . ' ' . $personne->getPrenom(),
                    $personne->getEmail(),
                    $joursInactif . ' jours',
                    $emailStatus,
                ];

                $count++;
            }
        }

        if ($count > 0) {
            $io->table(
                ['ID', 'Nom', 'Email', 'Inactivité', 'Statut email'],
                $rows
            );
            $io->success("{$count} compte(s) marqué(s) INACTIF et notifié(s) par email.");
        } else {
            $io->success('Aucun compte inactif détecté.');
        }

        $io->text('Exécuté le : ' . $now->format('d/m/Y à H:i:s'));

        return Command::SUCCESS;
    }
}