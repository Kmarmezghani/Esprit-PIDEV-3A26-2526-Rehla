<?php

namespace App\Service;

/**
 * Service to fetch city data from local JSON file
 * Data originally from various geographic databases
 */
class CityApiService
{
    private string $dataFile;

    public function __construct()
    {
        $this->dataFile = __DIR__ . '/../../data/cities.json';
    }

    /**
     * Load all cities from local JSON
     */
    private function loadCities(): array
    {
        if (!file_exists($this->dataFile)) {
            return [];
        }

        $json = file_get_contents($this->dataFile);
        $cities = json_decode($json, true);
        
        return is_array($cities) ? $cities : [];
    }

    /**
     * Search cities in a specific country
     * 
     * @param string $countryName Country name (e.g., "France")
     * @param string $query City name partial (e.g., "par")
     * @return array Array of cities with name, latitude, longitude
     */
    public function searchCities(string $countryName, string $query): array
    {
        if (empty($query) || strlen($query) < 2 || empty($countryName)) {
            return [];
        }

        $cities = $this->loadCities();
        $query = strtolower($query);
        $countryName = strtolower($countryName);
        
        $results = [];
        foreach ($cities as $city) {
            $cityCountry = strtolower($city['country'] ?? '');
            $cityName = strtolower($city['name'] ?? '');
            
            // Match by country and partial city name
            if ($cityCountry === $countryName && str_contains($cityName, $query)) {
                $results[] = [
                    'name' => $city['name'],
                    'display_name' => $city['name'] . ', ' . $city['country'],
                    'latitude' => $city['latitude'],
                    'longitude' => $city['longitude'],
                    'country' => $city['country'],
                ];
            }
        }
        
        return $results;
    }

    /**
     * Get all cities for a specific country
     */
    public function getCitiesByCountry(string $countryName): array
    {
        $cities = $this->loadCities();
        $countryName = strtolower($countryName);
        
        $results = [];
        foreach ($cities as $city) {
            if (strtolower($city['country']) === $countryName) {
                $results[] = [
                    'name' => $city['name'],
                    'latitude' => $city['latitude'],
                    'longitude' => $city['longitude'],
                    'country' => $city['country'],
                ];
            }
        }
        
        return $results;
    }

    /**
     * Get city details by name and country
     */
    public function getCityDetails(string $cityName, string $countryName): ?array
    {
        $cities = $this->loadCities();
        
        foreach ($cities as $city) {
            if (strcasecmp($city['name'], $cityName) === 0 && 
                strcasecmp($city['country'], $countryName) === 0) {
                return [
                    'name' => $city['name'],
                    'latitude' => $city['latitude'],
                    'longitude' => $city['longitude'],
                    'country' => $city['country'],
                ];
            }
        }
        
        return null;
    }

    /**
     * Get all cities (paginated if needed)
     */
    public function getAllCities(): array
    {
        return $this->loadCities();
    }
}
