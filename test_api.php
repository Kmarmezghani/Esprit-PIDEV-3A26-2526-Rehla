<?php

require 'vendor/autoload.php';

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\HttpClient\NativeHttpClient;

echo "Testing REST Countries API directly...\n\n";

$client = new NativeHttpClient();

try {
    echo "Testing: https://restcountries.com/v3.1/all?fields=name,capital\n";
    $response = $client->request('GET', 'https://restcountries.com/v3.1/all', [
        'query' => ['fields' => 'name,capital,region,subregion,population,flags'],
        'timeout' => 15,
    ]);
    
    echo "Status: " . $response->getStatusCode() . "\n";
    
    $data = $response->toArray();
    echo "Found " . count($data) . " countries\n\n";
    
    if (count($data) > 0) {
        echo "First 3 countries:\n";
        for ($i = 0; $i < min(3, count($data)); $i++) {
            echo "  - " . ($data[$i]['name']['common'] ?? 'N/A') . "\n";
        }
    } else {
        echo "No data received!\n";
        print_r($data);
    }
    
} catch (\Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}

echo "\n\nTesting search...\n";
try {
    $response = $client->request('GET', 'https://restcountries.com/v3.1/name/france', [
        'query' => ['fields' => 'name,capital'],
        'timeout' => 10,
    ]);
    
    echo "Search Status: " . $response->getStatusCode() . "\n";
    $data = $response->toArray();
    echo "Found " . count($data) . " results\n";
    
} catch (\Exception $e) {
    echo "Search ERROR: " . $e->getMessage() . "\n";
}
