<?php

require 'vendor/autoload.php';

use App\Service\CityApiService;

$cityService = new CityApiService();

echo "=== Testing getCitiesByCountry ===\n\n";

echo "Tunisia cities:\n";
$cities = $cityService->getCitiesByCountry('Tunisia');
echo "Found " . count($cities) . " cities\n";
foreach ($cities as $city) {
    echo " - {$city['name']} ({$city['latitude']}, {$city['longitude']})\n";
}

echo "\nFrance cities:\n";
$cities = $cityService->getCitiesByCountry('France');
echo "Found " . count($cities) . " cities\n";
foreach (array_slice($cities, 0, 5) as $city) {
    echo " - {$city['name']}\n";
}

echo "\n=== SUCCESS! ===\n";
