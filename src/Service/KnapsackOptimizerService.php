<?php

namespace App\Service;

use App\Entity\Ville;
use App\Entity\Attraction;

class KnapsackOptimizerService
{
    /**
     * Optimizes a day itinerary based on budget and time.
     * Logic: Maximizes the number of attractions while ensuring a variety of types.
     */
    public function optimize(Ville $ville, float $budget, float $maxHours = 8.0): array
    {
        $attractions = $ville->getAttractions()->toArray();
        
        // Filter out closed or too expensive attractions
        $candidates = array_filter($attractions, function(Attraction $a) use ($budget) {
            return !$a->getEstFerme() && $a->getPrix() <= $budget;
        });

        // Sort by price (ascending) to get more attractions for the money
        usort($candidates, fn($a, $b) => $a->getPrix() <=> $b->getPrix());

        $selected = [];
        $totalCost = 0.0;
        $usedTypes = [];
        $totalTime = 0.0;
        $averageVisitTime = 2.0; // Assume 2 hours per attraction for the math

        foreach ($candidates as $attr) {
            $price = (float)$attr->getPrix();
            
            // Check constraints: Budget + Time
            if (($totalCost + $price <= $budget) && ($totalTime + $averageVisitTime <= $maxHours)) {
                
                // Heuristic: If we already have this type, we only add it if we have extra room later
                // This ensures "Variety"
                $type = $attr->getType();
                if (!in_array($type, $usedTypes)) {
                    $selected[] = $attr;
                    $totalCost += $price;
                    $totalTime += $averageVisitTime;
                    $usedTypes[] = $type;
                }
            }
        }

        // Fill remaining budget/time with other candidates we skipped
        foreach ($candidates as $attr) {
            if (in_array($attr, $selected)) continue;
            
            $price = (float)$attr->getPrix();
            if (($totalCost + $price <= $budget) && ($totalTime + $averageVisitTime <= $maxHours)) {
                $selected[] = $attr;
                $totalCost += $price;
                $totalTime += $averageVisitTime;
            }
        }

        $attractionData = array_map(fn(Attraction $a) => [
            'nom' => $a->getNom(),
            'prix' => $a->getPrix(),
            'type' => $a->getType(),
            'desc' => $a->getDescription()
        ], $selected);

        return [
            'ville' => $ville->getNom(),
            'selected' => $attractionData,
            'totalCost' => round($totalCost, 2),
            'totalTime' => $totalTime,
            'count' => count($selected),
            'budgetProvided' => $budget
        ];
    }
}
