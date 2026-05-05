<?php
require 'vendor/autoload.php';
use Symfony\Component\Dotenv\Dotenv;

$dotenv = new Dotenv();
if (file_exists(__DIR__.'/../.env.local')) {
    $dotenv->load(__DIR__.'/../.env', __DIR__.'/../.env.local');
} else {
    $dotenv->load(__DIR__.'/../.env');
}

$dsn = $_ENV['DATABASE_URL'];
preg_match('/mysql:\/\/(.*):(.*)@(.*):(.*)\/(.*)/', $dsn, $matches);
$pdo = new PDO("mysql:host={$matches[3]};port={$matches[4]};dbname={$matches[5]}", $matches[1], $matches[2]);

$images = [];

// Get all country images
$stmt = $pdo->query("SELECT image FROM pays WHERE image IS NOT NULL AND image != ''");
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    $images[] = 'uploads/destinations/' . $row['image'];
}

// Get all city images
$stmt = $pdo->query("SELECT image FROM ville WHERE image IS NOT NULL AND image != ''");
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    $images[] = 'uploads/destinations/' . $row['image'];
}

echo "Found " . count($images) . " images to warm up.\n";

foreach ($images as $img) {
    echo "Warming up: $img ... ";
    $output = shell_exec("php bin/console liip:imagine:cache:resolve $img --filter=squared_thumbnail");
    if (strpos($output, '(resolved)') !== false) {
        echo "OK\n";
    } else {
        echo "FAILED\n";
    }
}

echo "\nAll images warmed up! Please refresh your browser.\n";
