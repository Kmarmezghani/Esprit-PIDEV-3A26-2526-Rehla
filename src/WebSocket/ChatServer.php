<?php

namespace App\WebSocket;

use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;

class ChatServer implements MessageComponentInterface
{
    /**
     * @var \SplObjectStorage<ConnectionInterface, null>
     */
    protected \SplObjectStorage $clients;

    public function __construct()
    {
        $this->clients = new \SplObjectStorage();
    }

    public function onOpen(ConnectionInterface $conn): void
    {
        $this->clients->attach($conn);
    }

    public function onMessage(ConnectionInterface $from, $msg): void
    {
        $data = json_decode($msg, true);

        if ($data['type'] === 'init') {
            /** @phpstan-ignore-next-line */
            $from->userId = $data['userId'];
        }

        /** @var ConnectionInterface $client */
        foreach ($this->clients as $client) {
            $client->send($msg);
        }
    }

    public function onClose(ConnectionInterface $conn): void
    {
        $this->clients->detach($conn);
    }

    public function onError(ConnectionInterface $conn, \Exception $e): void
    {
        $conn->close();
    }
}