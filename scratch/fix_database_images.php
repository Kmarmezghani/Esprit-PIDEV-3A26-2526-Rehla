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
echo "Using DATABASE_URL: $dsn\n";

// Basic parsing of mysql dsn: mysql://user:pass@host:port/db
preg_match('/mysql:\/\/(.*):(.*)@(.*):(.*)\/(.*)/', $dsn, $matches);
if (!$matches) {
    die("Could not parse DATABASE_URL\n");
}

$user = $matches[1];
$pass = $matches[2];
$host = $matches[3];
$port = $matches[4];
$db = $matches[5];

try {
    $pdo = new PDO("mysql:host=$host;port=$port;dbname=$db", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (Exception $e) {
    die("Connection failed: " . $e->getMessage() . "\n");
}

$uploadDir = __DIR__ . '/../public/uploads/destinations/';
if (!is_dir($uploadDir)) {
    die("Upload directory not found: $uploadDir\n");
}

$files = scandir($uploadDir);
$imageFiles = [];
foreach ($files as $file) {
    if ($file !== '.' && $file !== '..' && is_file($uploadDir . $file)) {
        $imageFiles[] = [
            'name' => $file,
            'time' => filemtime($uploadDir . $file)
        ];
    }
}

// Sort by time descending so we pick the newest first
usort($imageFiles, function($a, $b) {
    return $b['time'] - $a['time'];
});

echo "Found " . count($imageFiles) . " images in uploads folder.\n";

// 1. Update PAYS
$stmt = $pdo->query("SELECT id, nom FROM pays");
$countries = $stmt->fetchAll(PDO::FETCH_ASSOC);
$updatedPays = 0;

foreach ($countries as $country) {
    $name = strtolower($country['nom']);
    foreach ($imageFiles as $img) {
        // Check if file starts with country name (case insensitive)
        if (strpos(strtolower($img['name']), $name . '_') === 0) {
            $update = $pdo->prepare("UPDATE pays SET image = ? WHERE id = ?");
            $update->execute([$img['name'], $country['id']]);
            echo "Linked PAYS: {$country['nom']} -> {$img['name']}\n";
            $updatedPays++;
            break; // Stop at the first (newest) match
        }
    }
}

// 2. Update VILLE
$stmt = $pdo->query("SELECT id, nom FROM ville");
$villes = $stmt->fetchAll(PDO::FETCH_ASSOC);
$updatedVilles = 0;

foreach ($villes as $ville) {
    $name = strtolower($ville['nom']);
    foreach ($imageFiles as $img) {
        // Check if file starts with city name
        if (strpos(strtolower($img['name']), $name . '_') === 0) {
            $update = $pdo->prepare("UPDATE ville SET image = ? WHERE id = ?");
            $update->execute([$img['name'], $ville['id']]);
            echo "Linked VILLE: {$ville['nom']} -> {$img['name']}\n";
            $updatedVilles++;
            break; // Stop at the first (newest) match
        }
    }
}

echo "\nSummary:\n";
echo "- $updatedPays Countries updated.\n";
echo "- $updatedVilles Cities updated.\n";
echo "\nDONE! You can now check your front-end.\n";
echo "Note: If images still don't appear, make sure the GD extension is enabled in php.ini.\n";
