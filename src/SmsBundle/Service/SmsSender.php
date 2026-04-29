<?php

namespace App\SmsBundle\Service;

use Twilio\Rest\Client;

class SmsSender
{
    private Client $client;
    private string $from;

    public function __construct(string $sid, string $token, string $from)
    {
        $this->client = new Client($sid, $token);
        $this->from = $from;
    }

    public function send(string $to, string $message): void
    {
       
        if (!str_starts_with($to, '+216')) {
            $to = '+216' . ltrim($to, '0');
        }

        // 🔥 IMPORTANT : format WhatsApp
        $to = 'whatsapp:' . $to;

        $this->client->messages->create($to, [
            "from" => $this->from, 
            "body" => $message
        ]);

    }
}