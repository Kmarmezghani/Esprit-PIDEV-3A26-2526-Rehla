<?php

namespace App\Service\Messagerie;

use App\Entity\Personne;
use App\Entity\Conversation;
use Doctrine\ORM\EntityManagerInterface;

class ConversationService
{
    public function getOrCreateConversation(Personne $u1, Personne $u2, EntityManagerInterface $em) : Conversation
    {

        if ($u1->getId() > $u2->getId()) {
            [$u1, $u2] = [$u2, $u1];
        }

        $repo = $em->getRepository(Conversation::class);

            $conv = $repo->createQueryBuilder('c')
            ->where('(c.user1_id = :u1 AND c.user2_id = :u2) OR (c.user1_id = :u2 AND c.user2_id = :u1)')
            ->setParameter('u1', $u1)
            ->setParameter('u2', $u2)
            ->getQuery()
            ->getOneOrNullResult();

        if (!$conv) {
            $conv = new Conversation();
            $conv->setUser1_id($u1);
            $conv->setUser2_id($u2);
            $conv->setCreated_at(new \DateTime());

            $em->persist($conv);
            $em->flush();
        }

        return $conv;
    }
}