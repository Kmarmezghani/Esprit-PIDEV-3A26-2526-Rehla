<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;

class AiDescriptionService
{
    private string $endpoint = 'https://openrouter.ai/api/v1/chat/completions';

    private array $models = [
        'openrouter/auto',
        'meta-llama/llama-3-8b-instruct',
        'mistralai/mistral-7b-instruct',
        'google/gemma-7b-it',
    ];

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $openRouterApiKey
    ) {
    }

    public function generate(
        ?string $name,
        ?string $type,
        ?string $destination,
        ?string $duration
    ): string {
        if (empty($this->openRouterApiKey)) {
            throw new \RuntimeException('OPENROUTER_API_KEY manquante.');
        }

        $prompt = $this->buildPrompt($name, $type, $destination, $duration);

        $lastError = null;

        foreach ($this->models as $model) {
            for ($attempt = 1; $attempt <= 3; $attempt++) {
                try {
                    $text = $this->callOpenRouter($model, $prompt);
                    $text = $this->clean($text);
                    return $this->enforceTwoSentences($text);
                } catch (\RuntimeException $e) {
                    $lastError = $e;

                    if (!$this->isRetryable($e) || $attempt === 3) {
                        break;
                    }

                    sleep($attempt);
                }
            }
        }

        throw $lastError ?? new \RuntimeException('Échec de génération IA.');
    }

    private function callOpenRouter(string $model, string $prompt): string
    {
        $body = [
            'model' => $model,
            'messages' => [
                [
                    'role' => 'system',
                    'content' => "Tu rédiges des descriptions d’activités touristiques en français, dans un style naturel, fluide, vendeur mais concret. Réponds uniquement avec deux phrases complètes.",
                ],
                [
                    'role' => 'user',
                    'content' => $prompt,
                ],
            ],
            'provider' => [
                'allow_fallbacks' => true,
            ],
            'temperature' => 0.5,
            'max_tokens' => 300,
        ];

        try {
            $response = $this->httpClient->request('POST', $this->endpoint, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->openRouterApiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'http://localhost',
                    'X-Title' => 'Rehla',
                ],
                'json' => $body,
                'timeout' => 90,
            ]);

            $statusCode = $response->getStatusCode();
            $content = $response->getContent(false);

            if ($statusCode !== 200) {
                throw new \RuntimeException("OpenRouter HTTP {$statusCode} -> {$content}");
            }

            $data = json_decode($content, true);

            if (!isset($data['choices'][0]['message']['content'])) {
                throw new \RuntimeException('Réponse IA invalide.');
            }

            return $data['choices'][0]['message']['content'];
        } catch (TransportExceptionInterface $e) {
            throw new \RuntimeException('Erreur réseau ou timeout.', 0, $e);
        }
    }

    private function buildPrompt(?string $name, ?string $type, ?string $destination, ?string $duration): string
    {
        return sprintf(
            "Rédige une description en français pour une activité touristique.\n\nNom : %s\nType : %s\nDestination : %s\nDurée : %s\n\nContraintes :\n- exactement deux phrases\n- ton professionnel, naturel et engageant\n- pas de liste à puces\n- pas de titre\n- texte clair pour aider un voyageur à comprendre l’expérience\n- éviter les phrases trop génériques\n- ne pas inventer d’informations précises non fournies",
            $this->safe($name),
            $this->safe($type),
            $this->safe($destination),
            $this->safe($duration)
        );
    }

    private function enforceTwoSentences(string $text): string
    {
        $text = trim(preg_replace('/\s+/', ' ', str_replace("\n", ' ', $text)) ?? '');

        if ($text === '') {
            return '';
        }

        $parts = preg_split('/(?<=[\.\!\?])\s+/', $text) ?: [];

        if (count($parts) >= 2) {
            $result = trim($parts[0]) . ' ' . trim($parts[1]);
            if (!preg_match('/[.!?]$/', $result)) {
                $result .= '.';
            }
            return $result;
        }

        if (!preg_match('/[.!?]$/', $text)) {
            $text .= '.';
        }

        return $text;
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

    private function isRetryable(\RuntimeException $e): bool
    {
        $message = strtolower($e->getMessage());

        return str_contains($message, 'timeout')
            || str_contains($message, '429')
            || str_contains($message, '503')
            || str_contains($message, 'rate')
            || str_contains($message, 'unavailable')
            || str_contains($message, 'overloaded');
    }
}