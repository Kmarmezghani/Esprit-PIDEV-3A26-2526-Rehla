<?php
require 'vendor/autoload.php';
use Symfony\Component\Dotenv\Dotenv;

$dotenv = new Dotenv();
if (file_exists(__DIR__.'/../.env.local')) {
    $dotenv->load(__DIR__.'/../.env', __DIR__.'/../.env.local');
} else {
    $dotenv->load(__DIR__.'/../.env');
}

echo "Current DATABASE_URL: " . $_ENV['DATABASE_URL'] . "\n";
echo "GD Extension: " . (extension_loaded('gd') ? "ENABLED" : "DISABLED") . "\n";

try {
    $dsn = "mysql:host=127.0.0.1;port=3306;dbname=rehla09";
    $user = "REHLA";
    $pass = "Rehla123";
    echo "Checking rehla09 database...\n";
    $pdo = new PDO($dsn, $user, $pass);
    
    $stmt = $pdo->query("SELECT id, nom, image FROM pays WHERE image IS NOT NULL AND image != ''");
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "Countries with images in rehla09 DB: " . count($results) . "\n";
    foreach ($results as $r) {
        echo "  - " . $r['nom'] . ": " . $r['image'] . "\n";
    }
    
    $stmt = $pdo->query("SELECT id, nom, image FROM ville WHERE image IS NOT NULL AND image != ''");
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "Cities with images in rehla09 DB: " . count($results) . "\n";
    foreach ($results as $r) {
        echo "  - " . $r['nom'] . ": " . $r['image'] . "\n";
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
