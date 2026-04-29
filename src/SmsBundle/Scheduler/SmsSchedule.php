<?php

namespace App\SmsBundle\Scheduler;


use Symfony\Component\Scheduler\Attribute\AsSchedule;
use Symfony\Component\Scheduler\Schedule;
use Symfony\Component\Scheduler\RecurringMessage;

#[AsSchedule]
class SmsSchedule
{
    public function __invoke(): Schedule
    {
        return (new Schedule())
            ->add(
                RecurringMessage::every('1 minute', 'app:send-sms')
            );
    }
}