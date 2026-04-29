<?php

namespace App\Service;

use App\Entity\Ville;
use App\Repository\VilleRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class CircuitOptimizer
{
    private VilleRepository $villeRepository;
    private HttpClientInterface $httpClient;

    public function __construct(VilleRepository $villeRepository, HttpClientInterface $httpClient)
    {
        $this->villeRepository = $villeRepository;
        $this->httpClient = $httpClient;
    }

    /**
     * Optimized circuit generation using Nearest Neighbor Algorithm
     */
    public function generateCircuit(float $maxBudget, string $typeTourisme, ?int $paysId = null): array
    {
        // 1. Fetch potential cities
        $criteria = [];
        if ($paysId)
            $criteria['pays_id'] = $paysId;
        if (!empty($typeTourisme))
            $criteria['typeTourisme'] = $typeTourisme;

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
            return ['itinerary' => [], 'totalCost' => 0, 'cityCount' => 0, 'aiMessage' => null];
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

        // 5. Generate AI Travel Agent Text
        $aiMessage = $this->generateAITravelAgentText($circuit, $currentBudget, $typeTourisme);

        return [
            'itinerary' => $circuit,
            'totalCost' => round($currentBudget, 2),
            'cityCount' => count($circuit),
            'aiMessage' => $aiMessage
        ];
    }

    private function generateAITravelAgentText(array $itinerary, float $budget, string $typeTourisme): ?string
    {
        // Use Gemini API Key
        $apiKey = $_SERVER['GEMINI_API_KEY3'] ?? null;
        if (!$apiKey || empty($itinerary)) {
            return "Erreur technique : La clé API Gemini est manquante. Vérifiez le fichier .env.";
        }

        $cityNames = array_map(fn($city) => $city['nom'], $itinerary);
        $citiesStr = implode(', ', $cityNames);

        $prompt = "Tu es un agent de voyage très enthousiaste et professionnel travaillant pour 'Rehla Travel Agency'. Ton client a un budget de {$budget} euros et souhaite un voyage de type '{$typeTourisme}'. Tu viens de lui préparer un itinéraire passant par les villes suivantes : {$citiesStr}. Écris un message de bienvenue personnalisé de 3 ou 4 phrases maximum, très chaleureux et motivant, pour lui présenter ce super circuit. Parle directement au client en le tutoyant. Ne mets pas de titres, juste le texte.";

        try {
            $response = $this->httpClient->request('POST', 'https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=' . $apiKey, [
                'verify_peer' => false,
                'verify_host' => false,
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ]
                ]
            ]);

            $data = $response->toArray();
            if (isset($data['candidates'][0]['content']['parts'][0]['text'])) {
                // Remove potential markdown asterisks returned by AI for a cleaner text
                return str_replace(['**', '*'], '', $data['candidates'][0]['content']['parts'][0]['text']);
            }
        } catch (\Exception $e) {
            // Return exact error to understand why it fails
            return "Erreur technique : " . $e->getMessage();
        }

        return "Erreur technique : Aucune réponse valide reçue de l'IA.";
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
