<?php

namespace App\Service;

use App\Entity\Activite;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class GeminiRecommendationService
{
    private const ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=';
    private const DEBUG = true;

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $geminiApiKey,
        private CacheInterface $cache
    ) {
    }

    /**
     * Cached version:
     * Gemini is called again only if user preferences or activities changed.
     *
     * @param Activite[] $candidates
     * @return int[]
     */
    public function rankActivityIdsMax3Cached(
        int $userId,
        array $candidates,
        string $userProfileText
    ): array {
        if (empty($candidates) || empty($userProfileText) || empty($this->geminiApiKey)) {
            return [];
        }

        $cacheKey = $this->buildCacheKey($userId, $userProfileText, $candidates);

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($candidates, $userProfileText) {
            // Long lifetime, but real refresh happens when cache key changes
            $item->expiresAfter(60 * 60 * 24 * 30); // 30 days

            return $this->rankActivityIdsMax3($candidates, $userProfileText);
        });
    }

    /**
     * Real Gemini API call
     *
     * @param Activite[] $candidates
     * @return int[]
     */
    public function rankActivityIdsMax3(array $candidates, string $userProfileText): array
    {
        if (empty($this->geminiApiKey)) {
            return [];
        }

        if (empty($candidates)) {
            return [];
        }

        $list = array_slice($candidates, 0, 18);

        $activities = array_map(function (Activite $a) {
            return [
                'id' => $a->getId(),
                'name' => $this->safe($a->getNom()),
                'type' => $this->safe($a->getTypeActivite()),
                'price' => (float) $a->getPrix(),
                'rating' => (float) $a->getNoteMoyenne(),
                'desc' => $this->shortText($this->safe($a->getDescription()), 80),
            ];
        }, $list);

        $prompt = sprintf(
            "Tu es un moteur de recommandation d’activités de voyage.
Choisis et classe entre 0 et 3 activités maximum adaptées au profil utilisateur.
Réponds UNIQUEMENT avec du JSON minifié, sans markdown, sans backticks.

Format EXACT attendu :
{\"ranked_ids\":[...]}

Profil utilisateur:
%s

Activités candidates (JSON):
%s

Règles:
- Respecte budgetMin/budgetMax si possible
- Match centresInteret/typesVoyage avec name/type/desc
- Si aucune activité ne correspond, retourne {\"ranked_ids\":[]}
- Ne renvoie jamais plus de 3 IDs
- N’utilise que des IDs existants dans la liste",
            $userProfileText,
            json_encode($activities, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)
        );

        $body = [
            'contents' => [
                [
                    'parts' => [
                        ['text' => $prompt]
                    ]
                ]
            ]
        ];

        try {
            $response = $this->httpClient->request('POST', self::ENDPOINT . $this->geminiApiKey, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => $body,
                'timeout' => 20,
            ]);

            $statusCode = $response->getStatusCode();
            $raw = $response->getContent(false);

            if (self::DEBUG) {
                dump([
                    'gemini_status' => $statusCode,
                    'gemini_raw' => $raw,
                ]);
            }

            if ($statusCode !== 200) {
                return [];
            }

            $data = json_decode($raw, true);

            $modelText = $data['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if (!$modelText) {
                return [];
            }

            $modelText = trim(str_replace(['```json', '```'], '', $modelText));

            preg_match('/"ranked_ids"\s*:\s*\[(.*?)\]/s', $modelText, $matches);

            if (!isset($matches[1])) {
                return [];
            }

            $inside = trim($matches[1]);

            if ($inside === '') {
                return [];
            }

            $ids = array_filter(array_map(function ($value) {
                $value = trim($value);
                return is_numeric($value) ? (int) $value : null;
            }, explode(',', $inside)));

            $ids = array_values(array_unique($ids));

            return array_slice($ids, 0, 3);
        } catch (\Throwable $e) {
            if (self::DEBUG) {
                dump($e->getMessage());
            }

            return [];
        }
    }

    private function buildPreferencesFingerprint(string $userProfileText): string
    {
        return md5($userProfileText);
    }

    /**
     * @param Activite[] $candidates
     */
    private function buildActivitiesFingerprint(array $candidates): string
    {
        $data = array_map(function (Activite $a) {
            return [
                'id' => $a->getId(),
                'nom' => (string) $a->getNom(),
                'type' => (string) $a->getTypeActivite(),
                'prix' => (float) $a->getPrix(),
                'rating' => (float) $a->getNoteMoyenne(),
                'description' => (string) $a->getDescription(),
                'status' => method_exists($a, 'getStatus') ? (string) $a->getStatus() : null,
                'maxPlaces' => method_exists($a, 'getMaxPlaces') ? (int) $a->getMaxPlaces() : null,
            ];
        }, $candidates);

        return md5(json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES));
    }

    /**
     * @param Activite[] $candidates
     */
    private function buildCacheKey(
        int $userId,
        string $userProfileText,
        array $candidates
    ): string {
        $preferencesFingerprint = $this->buildPreferencesFingerprint($userProfileText);
        $activitiesFingerprint = $this->buildActivitiesFingerprint($candidates);

        return 'gemini_reco_' . $userId . '_' . $preferencesFingerprint . '_' . $activitiesFingerprint;
    }

    private function safe(?string $value): string
    {
        return $value ?? '';
    }

    private function shortText(string $text, int $max): string
    {
        $text = trim(str_replace(["\n", "\r"], ' ', $text));

        if (mb_strlen($text) <= $max) {
            return $text;
        }

        return mb_substr($text, 0, $max);
    }
}