<?php

use Ratchet\Http\HttpServer;
use Ratchet\Server\IoServer;
use Ratchet\WebSocket\WsServer;
use App\WebSocket\ChatServer;

require dirname(__DIR__) . '/vendor/autoload.php';

require 'src/WebSocket/ChatServer.php';

$server = IoServer::factory(
    new HttpServer(
        new WsServer(
            new ChatServer()
        )
    ),
    9001
);

$server->run();