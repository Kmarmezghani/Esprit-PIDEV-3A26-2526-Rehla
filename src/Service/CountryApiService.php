<?php

namespace App\Service;

/**
 * Service to fetch country data from local JSON file
 * Data originally from REST Countries API (https://restcountries.com/)
 */
class CountryApiService
{
    private string $dataFile;

    public function __construct()
    {
        $this->dataFile = __DIR__ . '/../../data/countries.json';
    }

    /**
     * Load all countries from local JSON
     */
    private function loadCountries(): array
    {
        if (!file_exists($this->dataFile)) {
            return [];
        }

        $json = file_get_contents($this->dataFile);
        $countries = json_decode($json, true);
        
        return is_array($countries) ? $countries : [];
    }

    /**
     * Search countries by name (partial match)
     * Example: "fran" returns France, etc.
     * 
     * @return array Array of countries with name, capital, region, population
     */
    public function searchCountries(string $query): array
    {
        if (empty($query) || strlen($query) < 2) {
            return [];
        }

        $countries = $this->loadCountries();
        $query = strtolower($query);
        
        $results = [];
        foreach ($countries as $country) {
            if (isset($country['name']) && str_contains(strtolower($country['name']), $query)) {
                $results[] = $country;
            }
        }
        
        return $results;
    }

    /**
     * Get country by exact name
     */
    public function getCountryByName(string $name): ?array
    {
        $countries = $this->loadCountries();
        
        foreach ($countries as $country) {
            if (isset($country['name']) && strcasecmp($country['name'], $name) === 0) {
                return $country;
            }
        }
        
        return null;
    }

    /**
     * Get all countries
     */
    public function getAllCountries(): array
    {
        return $this->loadCountries();
    }

    /**
     * Get countries by region (e.g., Africa, Europe, Asia)
     */
    public function getCountriesByRegion(string $region): array
    {
        $countries = $this->loadCountries();
        $results = [];
        
        foreach ($countries as $country) {
            if (isset($country['region']) && strcasecmp($country['region'], $region) === 0) {
                $results[] = $country;
            }
        }
        
        return $results;
    }
}
