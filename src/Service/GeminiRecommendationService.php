<?php

namespace App\Service;

use App\Entity\Activite;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class GeminiRecommendationService
{
    private const ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=';
    private const DEBUG = true;

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $geminiApiKey
    ) {
    }

    /**
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

            if (empty($matches[1])) {
                return [];
            }

            $ids = array_filter(array_map(function ($value) {
                $value = trim($value);
                return is_numeric($value) ? (int) $value : null;
            }, explode(',', $matches[1])));

            $ids = array_values(array_unique($ids));

            return array_slice($ids, 0, 3);
        } catch (\Throwable $e) {
            if (self::DEBUG) {
                dump($e->getMessage());
            }

            return [];
        }
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