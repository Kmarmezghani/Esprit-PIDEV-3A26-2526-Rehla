<?php

namespace App\Service;

use App\Entity\Activite;
use App\Repository\AvisRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class WeatherActivityTipsService
{
    public function __construct(
        private AvisRepository $avisRepository,
        private HttpClientInterface $client,
        private CacheInterface $cache
    ) {
    }

    public function getTipsForActivity(Activite $activite): array
    {
        $avisList = $this->avisRepository->findBy(['activite' => $activite]);

        $reviews = [];

        foreach ($avisList as $avis) {
            $text = trim((string) $avis->getCommentaire());

            if (mb_strlen($text) > 10) {
                $reviews[] = $text;
            }
        }

        $reviews = array_slice($reviews, 0, 10);

        $ville = $activite->getDestination()?->getNom();
        $dateDebut = $activite->getDateDebut();

        if (!$ville || !$dateDebut) {
            return [
                'tips' => [],
                'weather' => null
            ];
        }

        $today = new \DateTime('today');
        $maxForecastDate = (clone $today)->modify('+5 days');

        if ($dateDebut < $today || $dateDebut > $maxForecastDate) {
            $date = $today->format('Y-m-d');
        } else {
            $date = $dateDebut->format('Y-m-d');
        }

        $weather = $this->getWeather($ville, $date);

        if (!$weather) {
            return [
                'tips' => [],
                'weather' => null
            ];
        }

        $cacheKey = $this->buildCacheKey($activite, $reviews, $weather);

       $tips = $this->generateTips($activite, $reviews, $weather);

        return [
            'tips' => is_array($tips) ? $tips : [],
            'weather' => $weather
        ];
    }

    private function buildCacheKey(Activite $activite, array $reviews, array $weather): string
    {
        $reviewsHash = md5(json_encode($reviews, JSON_UNESCAPED_UNICODE));
        $weatherHash = md5(json_encode($weather, JSON_UNESCAPED_UNICODE));

        return 'weather_tips_activity_' . $activite->getId() . '_' . $reviewsHash . '_' . $weatherHash;
    }

    private function getWeather(string $ville, string $date): ?array
    {
        $apiKey = $_ENV['OPENWEATHER_API_KEY'] ?? null;

        if (!$apiKey) {
            return null;
        }

        try {
            $response = $this->client->request('GET', 'https://api.openweathermap.org/data/2.5/forecast', [
                'query' => [
                    'q' => $ville,
                    'appid' => $apiKey,
                    'units' => 'metric',
                    'lang' => 'fr'
                ]
            ]);

            $data = $response->toArray();

            if (!isset($data['list']) || !is_array($data['list']) || empty($data['list'])) {
                return null;
            }

            foreach ($data['list'] as $forecast) {
                if (isset($forecast['dt_txt']) && str_starts_with($forecast['dt_txt'], $date)) {
                    return $this->formatWeather($forecast);
                }
            }

            return $this->formatWeather($data['list'][0]);
        } catch (\Exception $e) {
            return null;
        }
    }

    private function formatWeather(array $forecast): ?array
    {
        if (
            !isset($forecast['main']['temp']) ||
            !isset($forecast['weather'][0]['description']) ||
            !isset($forecast['weather'][0]['icon']) ||
            !isset($forecast['main']['humidity']) ||
            !isset($forecast['wind']['speed'])
        ) {
            return null;
        }

        return [
            'temp' => round($forecast['main']['temp']),
            'description' => $forecast['weather'][0]['description'],
            'icon' => $forecast['weather'][0]['icon'],
            'humidity' => $forecast['main']['humidity'],
            'wind' => round($forecast['wind']['speed'] * 3.6),
        ];
    }

    private function generateTips(Activite $activite, array $reviews, array $weather): array
{
    $apiKey = $_ENV['GEMINI_API_KEY'] ?? null;

    if (!$apiKey) {
        return $this->generateFallbackTips($weather);
    }

    $reviewsText = !empty($reviews)
        ? "- " . implode("\n- ", $reviews)
        : "Aucun avis utile disponible.";

    $nom = trim((string) $activite->getNom());
    $description = trim((string) $activite->getDescription());
    $type = trim((string) $activite->getTypeActivite());

    $prompt = "
Tu es un assistant de voyage.

Ta mission :
Générer des conseils pratiques en français pour cette activité.

Sources autorisées :
- la météo
- les avis clients
- les informations de l'activité

Règles STRICTES :
- Conseils UNIQUEMENT liés à la météo
- Pas de conseils généraux
- Pas d’invention
- Ne suppose aucune activité (photo, plage, randonnée, baignade, promenade, etc.) sauf si mentionnée clairement
- Adapte STRICTEMENT les conseils aux données météo
- Les conseils doivent rester cohérents avec l'activité
- Utilise un ton naturel, simple et utile
- Évite un ton trop alarmant ou trop strict
- Donne entre 2 et 5 conseils
- Chaque conseil doit apporter une idée différente
- Interdiction de reformuler la même idée avec des mots différents
- Si plusieurs conseils reviennent à l’idée de prendre une couche légère, n’en garde qu’un seul
- Ne répète pas la même recommandation sous plusieurs formes
- Les conseils doivent couvrir des idées différentes quand c’est possible
- S'il n'existe que 2 ou 3 idées utiles, ne complète pas artificiellement

Format :
- entre 2 et 5 conseils
- courts, utiles et actionnables
- pas d’intro
- pas de conclusion
- pas de numérotation
- uniquement des puces commençant par '-'

Activité :
Nom : {$nom}
Description : {$description}
Type : {$type}

Météo :
{$weather['temp']}°C, {$weather['description']}
Humidité {$weather['humidity']}%
Vent {$weather['wind']} km/h

Avis :
{$reviewsText}
";

    $body = [
        'contents' => [
            [
                'parts' => [
                    ['text' => $prompt]
                ]
            ]
        ]
    ];

    $url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=' . $apiKey;

    try {
        $response = $this->client->request('POST', $url, [
            'headers' => [
                'Content-Type' => 'application/json'
            ],
            'json' => $body,
            'timeout' => 40
        ]);

        $status = $response->getStatusCode();
        $raw = $response->getContent(false);

        if ($status >= 400) {
            return $this->generateFallbackTips($weather);
        }

        $data = json_decode($raw, true);
        $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? '';

        $tips = $this->parseTips($text);

        if (empty($tips)) {
            return $this->generateFallbackTips($weather);
        }

        return $tips;
    } catch (\Exception $e) {
        return $this->generateFallbackTips($weather);
    }
}
private function generateFallbackTips(array $weather): array
{
    $tips = [];

    $temp = (int) ($weather['temp'] ?? 0);
    $description = mb_strtolower((string) ($weather['description'] ?? ''));
    $humidity = (int) ($weather['humidity'] ?? 0);
    $wind = (int) ($weather['wind'] ?? 0);

    if ($temp < 12) {
        $tips[] = "Prévoyez un vêtement chaud pour rester à l’aise pendant l’activité.";
    } elseif ($temp >= 12 && $temp <= 18) {
        $tips[] = "Une veste légère peut être utile si vous restez dehors un moment.";
    } elseif ($temp > 28) {
        $tips[] = "Pensez à prendre de l’eau pour rester à l’aise par temps chaud.";
    }

    if ($wind >= 25) {
        $tips[] = "Le vent peut être gênant, prévoyez une tenue adaptée et évitez les accessoires trop légers.";
    } elseif ($wind >= 15) {
        $tips[] = "Une légère brise est prévue, un petit vêtement de plus peut apporter plus de confort.";
    }

    if (
        str_contains($description, 'pluie') ||
        str_contains($description, 'averse') ||
        str_contains($description, 'bruine')
    ) {
        $tips[] = "Un parapluie ou une veste imperméable peut être utile en cas de pluie.";
    }

    if (
        str_contains($description, 'soleil') ||
        str_contains($description, 'dégagé') ||
        str_contains($description, 'ensoleillé')
    ) {
        $tips[] = "Des lunettes de soleil peuvent améliorer votre confort si l’exposition est forte.";
    }

    if ($humidity >= 80 && $temp >= 20) {
        $tips[] = "L’air peut paraître plus lourd que prévu, privilégiez une tenue confortable.";
    }

    $tips = array_values(array_unique($tips));
    $tips = $this->filterSimilarTips($tips);
    $tips = $this->removeIdeaRepetitions($tips);

    if (count($tips) < 2) {
        $tips[] = "Consultez la météo juste avant le départ pour adapter votre tenue si nécessaire.";
    }

    return array_slice($tips, 0, 5);
}
    private function parseTips(string $content): array
{
    $lines = preg_split('/\R/', $content);
    $tips = [];

    foreach ($lines as $line) {
        $tip = trim($line);
        $tip = preg_replace('/^[•\-\*]+\s*/u', '', $tip);
        $tip = preg_replace('/^\d+[\).\-\s]+/u', '', $tip);
        $tip = trim($tip);

        if ($tip !== '') {
            $tips[] = $tip;
        }
    }

    $tips = array_values(array_unique($tips));
    $tips = $this->filterSimilarTips($tips);
    $tips = $this->removeIdeaRepetitions($tips);

    return array_slice($tips, 0, 5);
}
private function filterSimilarTips(array $tips): array
{
    $filtered = [];

    foreach ($tips as $tip) {
        $normalizedTip = mb_strtolower($tip);
        $normalizedTip = preg_replace('/[^\p{L}\p{N}\s]/u', '', $normalizedTip);
        $normalizedTip = preg_replace('/\s+/u', ' ', $normalizedTip);
        $normalizedTip = trim($normalizedTip);

        $isSimilar = false;

        foreach ($filtered as $kept) {
            $normalizedKept = mb_strtolower($kept);
            $normalizedKept = preg_replace('/[^\p{L}\p{N}\s]/u', '', $normalizedKept);
            $normalizedKept = preg_replace('/\s+/u', ' ', $normalizedKept);
            $normalizedKept = trim($normalizedKept);

            similar_text($normalizedTip, $normalizedKept, $percent);

            if ($percent >= 70) {
                $isSimilar = true;
                break;
            }
        }

        if (!$isSimilar) {
            $filtered[] = $tip;
        }
    }

    return $filtered;
}
private function removeIdeaRepetitions(array $tips): array
{
    $result = [];
    $seenThemes = [];

    foreach ($tips as $tip) {
        $text = mb_strtolower($tip);

        $theme = 'other';

        if (
            str_contains($text, 'veste') ||
            str_contains($text, 'gilet') ||
            str_contains($text, 'cardigan') ||
            str_contains($text, 'couche') ||
            str_contains($text, 'couvrir') ||
            str_contains($text, 'se couvrir')
        ) {
            $theme = 'light_layer';
        } elseif (
            str_contains($text, 'foulard') ||
            str_contains($text, 'écharpe') ||
            str_contains($text, 'echarpe') ||
            str_contains($text, 'châle') ||
            str_contains($text, 'chale')
        ) {
            $theme = 'scarf';
        } elseif (
            str_contains($text, 'vent') ||
            str_contains($text, 'rafale') ||
            str_contains($text, 'brise')
        ) {
            $theme = 'wind';
        } elseif (
            str_contains($text, 'soleil') ||
            str_contains($text, 'lunettes') ||
            str_contains($text, 'crème solaire') ||
            str_contains($text, 'creme solaire') ||
            str_contains($text, 'uv')
        ) {
            $theme = 'sun';
        } elseif (
            str_contains($text, 'pluie') ||
            str_contains($text, 'averse') ||
            str_contains($text, 'parapluie') ||
            str_contains($text, 'imperméable') ||
            str_contains($text, 'impermeable')
        ) {
            $theme = 'rain';
        } elseif (
            str_contains($text, 'chaud') ||
            str_contains($text, 'chaleur') ||
            str_contains($text, 'hydrater') ||
            str_contains($text, 'eau')
        ) {
            $theme = 'heat';
        } elseif (
            str_contains($text, 'frais') ||
            str_contains($text, 'fraîcheur') ||
            str_contains($text, 'fraicheur') ||
            str_contains($text, 'soirée') ||
            str_contains($text, 'soiree')
        ) {
            $theme = 'cool_evening';
        }

        if (!in_array($theme, $seenThemes, true) || $theme === 'other') {
            $result[] = $tip;
            $seenThemes[] = $theme;
        }
    }

    return $result;
}
}