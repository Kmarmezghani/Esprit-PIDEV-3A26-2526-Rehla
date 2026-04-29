<?php

namespace App\Service;

use App\Entity\Ville;
use App\Repository\VilleRepository;

class CircuitOptimizer
{
    private VilleRepository $villeRepository;

    public function __construct(VilleRepository $villeRepository)
    {
        $this->villeRepository = $villeRepository;
    }

    public function generateCircuit(float $maxBudget, string $typeTourisme, ?int $paysId = null): array
    {
        // 1. Fetch potential cities
        $criteria = [];
        if ($paysId) $criteria['pays_id'] = $paysId;
        if (!empty($typeTourisme)) $criteria['typeTourisme'] = $typeTourisme;

        $cities = $this->villeRepository->findBy($criteria);

        // Fallback: if specific type returns nothing, try finding any cities in that country
        if (empty($cities) && $paysId) {
            $cities = $this->villeRepository->findBy(['pays_id' => $paysId]);
        }

        // 2. Filter cities that have coordinates
        $remainingCities = array_filter($cities, fn($v) => $v->getLatitude() !== null && $v->getLongitude() !== null);
        $remainingCities = array_values($remainingCities);

        $circuit = [];
        $currentBudget = 0;
        $cityBaseCost = 0.0; // Budget only spent on attractions as requested

        if (empty($remainingCities)) {
            return ['itinerary' => [], 'totalCost' => 0, 'cityCount' => 0];
        }

        // 3. Start from first city
        $currentCity = array_shift($remainingCities);
        $cityData = $this->processCitySelection($currentCity, $maxBudget, $currentBudget, $cityBaseCost);
        
        if ($cityData) {
            $circuit[] = $cityData;
            $currentBudget += $cityData['totalCityCost'];
        } else {
             return ['itinerary' => [], 'totalCost' => 0, 'cityCount' => 0];
        }

        // 4. Greedy path find
        while ($currentBudget < $maxBudget && !empty($remainingCities)) {
            $nearestIndex = $this->findNearestCityIndex($currentCity, $remainingCities);
            $nextCity = $remainingCities[$nearestIndex];

            $cityData = $this->processCitySelection($nextCity, $maxBudget, $currentBudget, $cityBaseCost);

            if (!$cityData) {
                // If we can't even afford the base cost of the next nearest city, we stop
                break;
            }

            $currentBudget += $cityData['totalCityCost'];
            $currentCity = $nextCity;
            $circuit[] = $cityData;

            array_splice($remainingCities, $nearestIndex, 1);
        }

        return [
            'itinerary' => $circuit,
            'totalCost' => round($currentBudget, 2),
            'cityCount' => count($circuit),
            'maxBudget' => $maxBudget
        ];
    }

    private function processCitySelection(Ville $ville, float $maxBudget, float $currentBudget, float $cityBaseCost): ?array
    {
        if ($currentBudget + $cityBaseCost > $maxBudget) {
            return null;
        }

        $selectedAttractions = [];
        $cityAttractionsCost = 0;

        foreach ($ville->getAttractions() as $attr) {
            if ($attr->getEstFerme()) continue;
            
            $price = (float)$attr->getPrix();
            if ($currentBudget + $cityBaseCost + $cityAttractionsCost + $price <= $maxBudget) {
                $selectedAttractions[] = [
                    'id' => $attr->getId(),
                    'nom' => $attr->getNom(),
                    'prix' => $price,
                    'type' => $attr->getType(),
                    'description' => $attr->getDescription(),
                    'ouverture' => $attr->getHeureOuverture()?->format('H:i'),
                    'fermeture' => $attr->getHeureFermeture()?->format('H:i')
                ];
                $cityAttractionsCost += $price;
            }
        }

        return [
            'id' => $ville->getId(),
            'nom' => $ville->getNom(),
            'lat' => $ville->getLatitude(),
            'lng' => $ville->getLongitude(),
            'type' => $ville->getTypeTourisme(),
            'attractions' => $selectedAttractions,
            'totalCityCost' => round($cityBaseCost + $cityAttractionsCost, 2),
            'baseCost' => $cityBaseCost
        ];
    }

    private function findNearestCityIndex(Ville $current, array $others): int
    {
        $shortestDistance = -1;
        $nearestIndex = 0;

        foreach ($others as $index => $candidate) {
            $dist = $this->haversineDistance(
                $current->getLatitude(),
                $current->getLongitude(),
                $candidate->getLatitude(),
                $candidate->getLongitude()
            );

            if ($shortestDistance == -1 || $dist < $shortestDistance) {
                $shortestDistance = $dist;
                $nearestIndex = $index;
            }
        }

        return $nearestIndex;
    }

    /**
     * Haversine formula to calculate distance between coordinates
     */
    private function haversineDistance($lat1, $lon1, $lat2, $lon2): float
    {
        if ($lat1 === null || $lon1 === null || $lat2 === null || $lon2 === null)
            return 999999;

        $earthRadius = 6371; // km
        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);

        $a = sin($dLat / 2) * sin($dLat / 2) +
            cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
            sin($dLon / 2) * sin($dLon / 2);

        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));

        return $earthRadius * $c;
    }
}
