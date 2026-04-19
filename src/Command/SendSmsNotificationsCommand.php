<?php

namespace App\Command;

use App\Entity\Notification;
use App\Entity\Personne;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Twilio\Rest\Client;

// class SendSmsNotificationsCommand extends Command
// {
//     protected static $defaultName = 'app:send-sms';

//     private EntityManagerInterface $em;

//     public function __construct(EntityManagerInterface $em)
//     {
//         parent::__construct();
//         $this->em = $em;
//     }

//     protected function execute(InputInterface $input, OutputInterface $output): int
//     {
//         date_default_timezone_set('Africa/Tunis');
//         $output->writeln("🚀 Cron SMS démarré...");

//         // ⚠️ CONFIG TWILIO (remplace avec tes infos)
//         $sid = "ACe4376d7ba0131231d67b17607832b155";
//         $token = "99f28cee70a315ddf2151972cf20cb9c";
//         $from = "+13203350905";

//         $twilio = new Client($sid, $token);

//         while (true) {

//             $now = new \DateTime();
//             $currentTime = $now->format('H:i');

//             $output->writeln("⏰ Vérification : " . $currentTime);

//             // récupérer users actifs
//             $personnes = $this->em->getRepository(Personne::class)
//                 ->findBy(['notifSmsActive' => true]);

//             foreach ($personnes as $personne) {

//                 if (!$personne->getHeureNotif()) continue;
//                 if (!$personne->getTelephone()) continue;

//                 $userTime = $personne->getHeureNotif()->format('H:i');

//                 // vérifier heure exacte
//                 if ($userTime !== $currentTime) continue;

//                 $output->writeln("📱 Envoi SMS à " . $personne->getTelephone());

//                 // récupérer notifications non envoyées
//                 $notifs = $this->em->getRepository(Notification::class)
//                     ->createQueryBuilder('n')
//                     ->where('n.receiver_id = :user')
//                     ->andWhere('n.is_sent_sms = false')
//                     ->andWhere('n.type IN (:types)')
//                     ->setParameter('user', $personne)
//                     ->setParameter('types', ['LIKE', 'COMMENT'])
//                     ->getQuery()
//                     ->getResult();

//                 if (count($notifs) === 0) {
//                     $output->writeln("⚠️ Aucune notif à envoyer");
//                     continue;
//                 }

//                 // construire message
//                 $messages = [];
//                 foreach ($notifs as $notif) {
//                     $messages[] = $notif->getMessage();
//                 }

//                 $finalMessage = implode("\n", $messages);
                
//                 $phone = $personne->getTelephone();
//                 if (!str_starts_with($phone, '+216')) {
//                     $phone = '+216' . ltrim($phone, '0');
//                 }
//                 try {
//                     // envoyer SMS
//                     $twilio->messages->create(
//                          $phone,
//                         [
//                             "from" => $from,
//                             "body" => $finalMessage
//                         ]
//                     );

//                     // update notifications
//                     foreach ($notifs as $notif) {
//                         $notif->setIs_sent_sms(true);
//                     }

//                     $this->em->flush();

//                     $output->writeln("✅ SMS envoyé !");
//                 } catch (\Exception $e) {
//                     $output->writeln("❌ Erreur Twilio : " . $e->getMessage());
//                 }
//             }

//             // attendre 60 secondes
//             sleep(60);
//         }

//         return Command::SUCCESS;
//     }
// }