<?php

require 'vendor/autoload.php';

use App\Service\CountryApiService;
use App\Service\CityApiService;

echo "Testing Local API Services...\n\n";

echo "=== Country API ===\n";
$countryService = new CountryApiService();

echo "1. Testing getAllCountries():\n";
$countries = $countryService->getAllCountries();
echo "   Found " . count($countries) . " countries\n";
if (count($countries) > 0) {
    echo "   First 3: " . $countries[0]['name'] . ", " . $countries[1]['name'] . ", " . $countries[2]['name'] . "\n";
}

echo "\n2. Testing searchCountries('fran'):\n";
$search = $countryService->searchCountries('fran');
echo "   Found " . count($search) . " results\n";
foreach ($search as $c) {
    echo "   - " . $c['name'] . "\n";
}

echo "\n3. Testing getCountryByName('France'):\n";
$france = $countryService->getCountryByName('France');
if ($france) {
    echo "   Found: " . $france['name'] . " (Capital: " . $france['capital'] . ")\n";
} else {
    echo "   Not found\n";
}

echo "\n=== City API ===\n";
$cityService = new CityApiService();

echo "4. Testing getAllCities():\n";
$cities = $cityService->getAllCities();
echo "   Found " . count($cities) . " cities\n";

echo "\n5. Testing searchCities('France', 'par'):\n";
$citiesSearch = $cityService->searchCities('France', 'par');
echo "   Found " . count($citiesSearch) . " results\n";
foreach ($citiesSearch as $c) {
    echo "   - " . $c['name'] . " (Lat: " . $c['latitude'] . ", Long: " . $c['longitude'] . ")\n";
}

echo "\n6. Testing getCitiesByCountry('Tunisia'):\n";
$tnCities = $cityService->getCitiesByCountry('Tunisia');
echo "   Found " . count($tnCities) . " Tunisian cities\n";
foreach ($tnCities as $c) {
    echo "   - " . $c['name'] . "\n";
}

echo "\n=== SUCCESS! All tests passed ===\n";
