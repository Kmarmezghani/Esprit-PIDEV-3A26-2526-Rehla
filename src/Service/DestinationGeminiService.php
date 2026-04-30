<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;

class DestinationGeminiService
{
    private HttpClientInterface $httpClient;

    public function __construct(HttpClientInterface $httpClient)
    {
        $this->httpClient = $httpClient;
    }

    public function generateForCountry(string $countryName): string
    {
        $apiKey = $_ENV['GEMINI_API_KEY3'] ?? $_SERVER['GEMINI_API_KEY3'] ?? null;
        if (empty($apiKey)) {
            throw new \RuntimeException('Clé API Gemini3 manquante dans le fichier .env');
        }

        $prompt = sprintf(
            "Tu es un expert en tourisme international. Rédige une description touristique en français pour le pays suivant : %s.\n\nContraintes :\n- environ 3 phrases\n- ton professionnel, naturel et engageant\n- pas de liste à puces\n- texte clair pour donner envie de visiter ce pays.",
            $this->safe($countryName)
        );

        $url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=' . $apiKey;

        try {
            $response = $this->httpClient->request('POST', $url, [
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
                ],
                'timeout' => 30,
            ]);

            $statusCode = $response->getStatusCode();
            $content = $response->getContent(false);

            if ($statusCode !== 200) {
                throw new \RuntimeException("Gemini HTTP {$statusCode} -> {$content}");
            }

            $data = json_decode($content, true);

            if (!isset($data['candidates'][0]['content']['parts'][0]['text'])) {
                throw new \RuntimeException('Réponse IA invalide depuis Gemini.');
            }

            return $this->clean($data['candidates'][0]['content']['parts'][0]['text']);
        } catch (TransportExceptionInterface $e) {
            throw new \RuntimeException('Erreur réseau ou timeout lors de l\'appel à Gemini.', 0, $e);
        }
    }

    private function clean(?string $text): string
    {
        if ($text === null) {
            return '';
        }

        return trim(preg_replace('/\s+/', ' ', str_replace("\n", ' ', $text)) ?? '');
    }

    private function safe(?string $value): string
    {
        return trim($value ?? '');
    }

    public function suggestCityProfile(string $cityName): array
    {
        $apiKey = $_ENV['GEMINI_API_KEY3'] ?? $_SERVER['GEMINI_API_KEY3'] ?? null;
        if (empty($apiKey)) {
            throw new \RuntimeException('Clé API Gemini3 manquante dans le fichier .env');
        }

        $prompt = sprintf(
            "Tu es un expert en tourisme. Pour la ville de %s, choisis le meilleur typeTourisme. Tu DOIS choisir EXACTEMENT l'un de ces 5 mots anglais : Seaside, Desert, Mountain, Urban, Cultural. Choisis ensuite la meilleure saison. Tu DOIS choisir EXACTEMENT l'un de ces 5 mots anglais : Winter, Spring, Summer, Autumn, All Year. Ne traduis pas ces mots en français. Réponds UNIQUEMENT avec un objet JSON valide au format exact suivant: {\"typeTourisme\": \"...\", \"saison\": \"...\"}",
            $this->safe($cityName)
        );

        $url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=' . $apiKey;

        try {
            $response = $this->httpClient->request('POST', $url, [
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
                    ],
                    'generationConfig' => [
                        'responseMimeType' => 'application/json',
                    ],
                ],
                'timeout' => 15,
            ]);

            $statusCode = $response->getStatusCode();
            $content = $response->getContent(false);

            if ($statusCode !== 200) {
                throw new \RuntimeException("Gemini HTTP {$statusCode}");
            }

            $data = json_decode($content, true);
            $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if ($text === null) {
                throw new \RuntimeException('Réponse IA invalide depuis Gemini.');
            }

            // Remove markdown code blocks if any
            $text = preg_replace('/```json\s*/', '', $text);
            $text = preg_replace('/```\s*/', '', $text);

            $result = json_decode(trim($text), true);
            
            if (!$result || !isset($result['typeTourisme']) || !isset($result['saison'])) {
                 return ['typeTourisme' => 'Non défini', 'saison' => 'Non défini'];
            }

            return [
                'typeTourisme' => $result['typeTourisme'],
                'saison' => $result['saison'],
            ];

        } catch (\Exception $e) {
            return ['error' => $e->getMessage()];
        }
    }
}
