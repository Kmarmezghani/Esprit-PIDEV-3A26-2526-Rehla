<?php

namespace App\WebSocket;

use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;

class ChatServer implements MessageComponentInterface
{
    protected $clients;

    public function __construct() {
        $this->clients = new \SplObjectStorage;
    }

    public function onOpen(ConnectionInterface $conn) {
        $this->clients->attach($conn);
    }

 public function onMessage(ConnectionInterface $from, $msg)
{
    $data = json_decode($msg, true);

    if ($data['type'] === 'init') {
        $from->userId = $data['userId'];
    }

    foreach ($this->clients as $client) {
        $client->send($msg);
    }
}

    public function onClose(ConnectionInterface $conn) {
        $this->clients->detach($conn);
    }

    public function onError(ConnectionInterface $conn, \Exception $e) {
        $conn->close();
    }
}