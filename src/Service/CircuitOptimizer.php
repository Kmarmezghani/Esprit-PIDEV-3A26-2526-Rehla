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

    /**
     * Optimized circuit generation using Nearest Neighbor Algorithm
     */
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

        if (empty($remainingCities)) {
            return ['itinerary' => [], 'totalCost' => 0, 'cityCount' => 0];
        }

        // 3. Start from first city
        $currentCity = array_shift($remainingCities);
        $circuit[] = $this->formatCityData($currentCity);
        $currentBudget += $this->calculateCityMinCost($currentCity);

        // 4. Greedy path find
        while ($currentBudget < $maxBudget && !empty($remainingCities)) {
            $nearestIndex = $this->findNearestCityIndex($currentCity, $remainingCities);
            $nextCity = $remainingCities[$nearestIndex];
            
            $cost = $this->calculateCityMinCost($nextCity);
            
            if (($currentBudget + $cost) > $maxBudget) {
                break;
            }

            $currentBudget += $cost;
            $currentCity = $nextCity;
            $circuit[] = $this->formatCityData($nextCity);
            
            array_splice($remainingCities, $nearestIndex, 1);
        }

        return [
            'itinerary' => $circuit,
            'totalCost' => round($currentBudget, 2),
            'cityCount' => count($circuit)
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
        if ($lat1 === null || $lon1 === null || $lat2 === null || $lon2 === null) return 999999;

        $earthRadius = 6371; // km
        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);

        $a = sin($dLat / 2) * sin($dLat / 2) +
             cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
             sin($dLon / 2) * sin($dLon / 2);
             
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        
        return $earthRadius * $c;
    }

    private function calculateCityMinCost(Ville $ville): float
    {
        $basePrice = 85.0; // Base cost for accommodation/transport per city
        $attractions = $ville->getAttractions();
        
        if ($attractions->count() > 0) {
            $total = 0;
            foreach ($attractions as $attr) {
                $total += $attr->getPrix();
            }
            $basePrice += ($total / $attractions->count());
        }
        
        return round($basePrice, 2);
    }

    private function formatCityData(Ville $ville): array
    {
        return [
            'id' => $ville->getId(),
            'nom' => $ville->getNom(),
            'lat' => $ville->getLatitude(),
            'lng' => $ville->getLongitude(),
            'type' => $ville->getTypeTourisme(),
            'attractionsCount' => $ville->getAttractions()->count()
        ];
    }
}
