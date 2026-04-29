<?php

namespace App\Command;

use App\Entity\Notification;
use App\Entity\Personne;
use App\SmsBundle\Service\SmsSender;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Attribute\AsCommand;

#[AsCommand(name: 'app:send-sms')]
class SendSmsNotificationsCommand extends Command
{
    protected static $defaultName = 'app:send-sms';

    private EntityManagerInterface $em;
    private SmsSender $smsSender;

    public function __construct(EntityManagerInterface $em, SmsSender $smsSender)
    {
        parent::__construct();
        $this->em = $em;
        $this->smsSender = $smsSender;
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        date_default_timezone_set('Africa/Tunis');

        while (true) {

            $output->writeln("🚀 Vérification SMS...");

            $now = new \DateTime();
            $currentTime = $now->format('H:i');
            $output->writeln("🚀 current time SMS: " . $currentTime);

            $personnes = $this->em->getRepository(Personne::class)
                ->findBy(['notifSmsActive' => true]);

            foreach ($personnes as $personne) {

                if (!$personne->getHeureNotif()) continue;
                if (!$personne->getTelephone()) continue;

                $userTime = $personne->getHeureNotif()->format('H:i');

                $diff = abs(strtotime($userTime) - strtotime($currentTime));
                if ($diff > 60) continue;

                $notifs = $this->em->getRepository(Notification::class)
                    ->createQueryBuilder('n')
                    ->where('n.receiver_id = :user')
                    ->andWhere('n.is_sent_sms = false')
                    ->andWhere('n.type IN (:types)')
                    ->setParameter('user', $personne)
                    ->setParameter('types', ['LIKE', 'COMMENT'])
                    ->getQuery()
                    ->getResult();

                if (count($notifs) === 0) continue;

                $messages = array_map(fn($n) => $n->getMessage(), $notifs);
                $finalMessage = implode("\n", $messages);

                try {
                    $this->smsSender->send($personne->getTelephone(), $finalMessage);

                    foreach ($notifs as $notif) {
                        $notif->setIs_sent_sms(true);
                    }

                    $this->em->flush();

                    $output->writeln("✅ SMS envoyé !");
                } catch (\Exception $e) {
                    $output->writeln("❌ Erreur : " . $e->getMessage());
                }
            }

            sleep(60);
        }

        return Command::SUCCESS;
    }
}