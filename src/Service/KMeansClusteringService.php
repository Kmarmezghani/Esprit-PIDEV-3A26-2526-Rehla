<?php

namespace App\Service;

use App\Entity\Ville;
use App\Repository\VilleRepository;

/**
 * K-Means Clustering Service
 * 
 * Algorithme de clustering pour grouper les villes similaires
 * Basé sur: coordonnées géographiques, typeTourisme, saison, prix moyen des attractions
 * 
 * @author Your Name
 */
class KMeansClusteringService
{
    private VilleRepository $villeRepository;

    public function __construct(VilleRepository $villeRepository)
    {
        $this->villeRepository = $villeRepository;
    }

    /**
     * Exécute l'algorithme K-Means sur les villes
     * 
     * @param int $k Nombre de clusters (généralement 3-5)
     * @param int $maxIterations Nombre maximum d'itérations (généralement 100)
     * @return array Clusters avec leurs villes et centroïdes
     */
    public function clusterCities(int $k = 3, int $maxIterations = 100): array
    {
        // 1. Récupérer toutes les villes avec coordonnées
        $cities = $this->villeRepository->findAll();
        $cities = array_filter($cities, fn($v) => $v->getLatitude() !== null && $v->getLongitude() !== null);
        $cities = array_values($cities);

        if (count($cities) < $k) {
            return ['error' => 'Pas assez de villes pour créer ' . $k . ' clusters'];
        }

        // 2. Normaliser les données (mise à l'échelle 0-1)
        $normalizedData = $this->normalizeData($cities);

        // 3. Initialiser les centroïdes aléatoirement (K-Means++)
        $centroids = $this->initializeCentroids($normalizedData, $k);

        // 4. Itérations K-Means
        for ($iteration = 0; $iteration < $maxIterations; $iteration++) {
            // 4.1 Assigner chaque ville au centroïde le plus proche
            $clusters = $this->assignToClusters($normalizedData, $centroids);

            // 4.2 Recalculer les centroïdes
            $newCentroids = $this->recalculateCentroids($clusters, $centroids);

            // 4.3 Vérifier la convergence (centroïdes stables)
            if ($this->hasConverged($centroids, $newCentroids)) {
                break;
            }

            $centroids = $newCentroids;
        }

        // 5. Formater les résultats pour l'affichage
        return $this->formatResults($cities, $clusters, $centroids, $iteration);
    }

    /**
     * Normalise les données pour l'algorithme (mise à l'échelle 0-1)
     * 
     * Features normalisées:
     * - latitude (0-1)
     * - longitude (0-1)
     * - typeTourisme (encodé: Seaside=0, Desert=1, Mountain=2, Urban=3, Cultural=4)
     * - saison (encodée: Winter=0, Spring=1, Summer=2, Autumn=3, All Year=4)
     * - prix moyen attractions (0-1)
     */
    private function normalizeData(array $cities): array
    {
        $data = [];
        
        // Trouver les min/max pour la normalisation
        $lats = array_map(fn($v) => $v->getLatitude(), $cities);
        $lngs = array_map(fn($v) => $v->getLongitude(), $cities);
        $prices = [];

        foreach ($cities as $city) {
            $avgPrice = $this->calculateAverageAttractionPrice($city);
            $prices[] = $avgPrice;
        }

        $minLat = min($lats);
        $maxLat = max($lats);
        $minLng = min($lngs);
        $maxLng = max($lngs);
        $minPrice = min($prices);
        $maxPrice = max($prices);

        foreach ($cities as $index => $city) {
            $data[] = [
                'index' => $index,
                'city' => $city,
                'lat' => $this->normalize($city->getLatitude(), $minLat, $maxLat),
                'lng' => $this->normalize($city->getLongitude(), $minLng, $maxLng),
                'typeTourisme' => $this->encodeTypeTourisme($city->getTypeTourisme()),
                'saison' => $this->encodeSaison($city->getSaison()),
                'price' => $this->normalize($prices[$index], $minPrice, $maxPrice)
            ];
        }

        return $data;
    }

    /**
     * Initialise les centroïdes avec l'algorithme K-Means++
     * Meilleure initialisation que purement aléatoire
     */
    private function initializeCentroids(array $data, int $k): array
    {
        $centroids = [];
        
        // Premier centroïde: ville aléatoire
        $firstIndex = array_rand($data);
        $centroids[] = $this->extractFeatures($data[$firstIndex]);

        // Centroïdes suivants: probabilité proportionnelle à la distance
        for ($i = 1; $i < $k; $i++) {
            $distances = [];
            $totalDistance = 0;

            foreach ($data as $point) {
                $minDist = PHP_FLOAT_MAX;
                foreach ($centroids as $centroid) {
                    $dist = $this->euclideanDistance($this->extractFeatures($point), $centroid);
                    $minDist = min($minDist, $dist);
                }
                $distances[] = $minDist * $minDist; // Carré de la distance
                $totalDistance += $distances[count($distances) - 1];
            }

            // Choisir avec probabilité proportionnelle
            $rand = mt_rand(0, (int)($totalDistance * 1000)) / 1000;
            $cumulative = 0;
            $selectedIndex = 0;

            foreach ($distances as $index => $dist) {
                $cumulative += $dist;
                if ($cumulative >= $rand) {
                    $selectedIndex = $index;
                    break;
                }
            }

            $centroids[] = $this->extractFeatures($data[$selectedIndex]);
        }

        return $centroids;
    }

    /**
     * Assigne chaque point au centroïde le plus proche
     */
    private function assignToClusters(array $data, array $centroids): array
    {
        $clusters = array_fill(0, count($centroids), []);

        foreach ($data as $point) {
            $features = $this->extractFeatures($point);
            $minDist = PHP_FLOAT_MAX;
            $closestCluster = 0;

            foreach ($centroids as $index => $centroid) {
                $dist = $this->euclideanDistance($features, $centroid);
                if ($dist < $minDist) {
                    $minDist = $dist;
                    $closestCluster = $index;
                }
            }

            $clusters[$closestCluster][] = $point;
        }

        return $clusters;
    }

    /**
     * Recalcule les centroïdes comme moyenne des points du cluster
     */
    private function recalculateCentroids(array $clusters, array $oldCentroids): array
    {
        $newCentroids = [];

        foreach ($clusters as $index => $cluster) {
            if (empty($cluster)) {
                // Si cluster vide, garder l'ancien centroïde
                $newCentroids[] = $oldCentroids[$index];
                continue;
            }

            $sum = [0, 0, 0, 0, 0]; // lat, lng, type, saison, price
            $count = count($cluster);

            foreach ($cluster as $point) {
                $features = $this->extractFeatures($point);
                for ($i = 0; $i < 5; $i++) {
                    $sum[$i] += $features[$i];
                }
            }

            $newCentroids[] = [
                $sum[0] / $count,
                $sum[1] / $count,
                $sum[2] / $count,
                $sum[3] / $count,
                $sum[4] / $count
            ];
        }

        return $newCentroids;
    }

    /**
     * Vérifie si les centroïdes ont convergé (changement minimal)
     */
    private function hasConverged(array $old, array $new, float $threshold = 0.001): bool
    {
        foreach ($old as $index => $oldCentroid) {
            $dist = $this->euclideanDistance($oldCentroid, $new[$index]);
            if ($dist > $threshold) {
                return false;
            }
        }
        return true;
    }

    /**
     * Distance euclidienne entre deux points
     */
    private function euclideanDistance(array $a, array $b): float
    {
        $sum = 0;
        for ($i = 0; $i < count($a); $i++) {
            $sum += pow($a[$i] - $b[$i], 2);
        }
        return sqrt($sum);
    }

    /**
     * Extrait les features d'un point
     */
    private function extractFeatures(array $point): array
    {
        return [
            $point['lat'],
            $point['lng'],
            $point['typeTourisme'],
            $point['saison'],
            $point['price']
        ];
    }

    /**
     * Normalise une valeur entre 0 et 1
     */
    private function normalize(?float $value, float $min, float $max): float
    {
        if ($max === $min) return 0;
        return ($value - $min) / ($max - $min);
    }

    /**
     * Encode le type de tourisme en nombre
     */
    private function encodeTypeTourisme(string $type): float
    {
        $mapping = [
            'Seaside' => 0,
            'Desert' => 1,
            'Mountain' => 2,
            'Urban' => 3,
            'Cultural' => 4
        ];
        return $mapping[$type] ?? 2; // Default Mountain
    }

    /**
     * Encode la saison en nombre
     */
    private function encodeSaison(string $saison): float
    {
        $mapping = [
            'Winter' => 0,
            'Spring' => 1,
            'Summer' => 2,
            'Autumn' => 3,
            'All Year' => 4
        ];
        return $mapping[$saison] ?? 4; // Default All Year
    }

    /**
     * Calcule le prix moyen des attractions d'une ville
     */
    private function calculateAverageAttractionPrice(Ville $ville): float
    {
        $attractions = $ville->getAttractions();
        if ($attractions->count() === 0) return 0;

        $total = 0;
        foreach ($attractions as $attr) {
            $total += $attr->getPrix() ?? 0;
        }
        return $total / $attractions->count();
    }

    /**
     * Formate les résultats pour l'affichage
     */
    private function formatResults(array $cities, array $clusters, array $centroids, int $iterations): array
    {
        $result = [
            'iterations' => $iterations,
            'k' => count($centroids),
            'clusters' => []
        ];

        foreach ($clusters as $clusterIndex => $cluster) {
            $clusterCities = [];
            foreach ($cluster as $point) {
                $clusterCities[] = $point['city'];
            }

            // Dénormaliser le centroïde pour l'affichage
            $centroid = $centroids[$clusterIndex];
            
            $result['clusters'][] = [
                'id' => $clusterIndex,
                'centroid' => $centroid,
                'cities' => $clusterCities,
                'count' => count($clusterCities),
                'color' => $this->getClusterColor($clusterIndex)
            ];
        }

        return $result;
    }

    /**
     * Retourne une couleur pour le cluster (pour la visualisation)
     */
    private function getClusterColor(int $index): string
    {
        $colors = [
            '#223f91', // Bleu
            '#f06d06', // Orange
            '#16a34a', // Vert
            '#dc3545', // Rouge
            '#9b59b6', // Violet
            '#f39c12', // Jaune
        ];
        return $colors[$index % count($colors)];
    }

    /**
     * Trouve des villes similaires à une ville donnée
     * 
     * @param Ville $city Ville de référence
     * @param int $limit Nombre de villes similaires à retourner
     * @return array Villes similaires avec scores de similarité
     */
    public function findSimilarCities(Ville $city, int $limit = 5): array
    {
        $cities = $this->villeRepository->findAll();
        $cities = array_filter($cities, fn($v) => $v->getId() !== $city->getId());
        $cities = array_values($cities);

        if (empty($cities)) return [];

        // Normaliser la ville de référence
        $referenceData = $this->normalizeData([$city])[0];
        $referenceFeatures = $this->extractFeatures($referenceData);

        // Normaliser toutes les villes
        $allData = $this->normalizeData($cities);

        // Calculer la similarité (distance euclidienne)
        $similarities = [];
        foreach ($allData as $point) {
            $features = $this->extractFeatures($point);
            $distance = $this->euclideanDistance($referenceFeatures, $features);
            $similarity = 1 / (1 + $distance); // Convertir distance en similarité (0-1)
            
            $similarities[] = [
                'city' => $point['city'],
                'similarity' => $similarity,
                'distance' => $distance
            ];
        }

        // Trier par similarité décroissante
        usort($similarities, fn($a, $b) => $b['similarity'] <=> $a['similarity']);

        // Retourner les N plus similaires
        return array_slice($similarities, 0, $limit);
    }
}
