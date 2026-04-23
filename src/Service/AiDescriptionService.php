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
        ?string $duration,
        array $attractions = []
    ): string {
        if (empty($this->openRouterApiKey)) {
            throw new \RuntimeException('OPENROUTER_API_KEY manquante.');
        }

        $cleanAttractions = $this->cleanAttractions($attractions);

        $required = 0;
        if (count($cleanAttractions) >= 3) {
            $required = 3;
        } elseif (count($cleanAttractions) >= 2) {
            $required = 2;
        }

        $prompt1 = $this->buildPrompt(
            $name,
            $type,
            $destination,
            $duration,
            $cleanAttractions,
            $required,
            false
        );

        $lastError = null;

        foreach ($this->models as $model) {
            for ($attempt = 1; $attempt <= 3; $attempt++) {
                try {
                    $out = $this->callOpenRouter($model, $prompt1);
                    $out = $this->clean($out);
                    $out = $this->enforceTwoSentences($out);

                    if ($required > 0 && !$this->usesAtLeastNAttractions($out, $cleanAttractions, $required)) {
                        $prompt2 = $this->buildPrompt(
                            $name,
                            $type,
                            $destination,
                            $duration,
                            $cleanAttractions,
                            $required,
                            true
                        );

                        $out2 = $this->callOpenRouter($model, $prompt2);
                        $out2 = $this->clean($out2);
                        $out2 = $this->enforceTwoSentences($out2);

                        if (!$this->usesAtLeastNAttractions($out2, $cleanAttractions, $required)) {
                            $prompt3 = $this->buildRewritePrompt($out2, $cleanAttractions, $required);
                            $out3 = $this->callOpenRouter($model, $prompt3);
                            $out3 = $this->clean($out3);

                            return $this->enforceTwoSentences($out3);
                        }

                        return $out2;
                    }

                    if ($this->looksTruncated($out)) {
                        $prompt2 = $this->buildPrompt(
                            $name,
                            $type,
                            $destination,
                            $duration,
                            $cleanAttractions,
                            $required,
                            true
                        );

                        $out2 = $this->callOpenRouter($model, $prompt2);
                        $out2 = $this->clean($out2);

                        return $this->enforceTwoSentences($out2);
                    }

                    return $out;
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
                    'content' => "Tu rédiges des descriptions d’activités touristiques en français dans un style type Airbnb : naturel, concret, structuré et pratique. Respecte strictement les consignes et retourne uniquement deux phrases complètes.",
                ],
                [
                    'role' => 'user',
                    'content' => $prompt,
                ],
            ],
            'provider' => [
                'allow_fallbacks' => true,
            ],
            'temperature' => 0.35,
            'max_tokens' => 520,
        ];

        try {
            $response = $this->httpClient->request('POST', $this->endpoint, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->openRouterApiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'http://localhost',
                    'X-Title' => 'TravelApp',
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

            return (string) $data['choices'][0]['message']['content'];
        } catch (TransportExceptionInterface $e) {
            throw new \RuntimeException('Timeout sur le modèle : ' . $model, 0, $e);
        }
    }

    private function cleanAttractions(array $attractions): array
    {
        $clean = [];

        foreach ($attractions as $attraction) {
            if (is_array($attraction)) {
                $line = sprintf(
                    'Nom: %s | Type: %s | Note: %s',
                    trim((string) ($attraction['nom'] ?? '')),
                    trim((string) ($attraction['type'] ?? '')),
                    trim((string) ($attraction['description'] ?? ''))
                );
            } else {
                $line = trim((string) $attraction);
            }

            $line = trim(preg_replace('/\s+/', ' ', $line) ?? '');

            if ($line !== '') {
                $clean[] = $line;
            }
        }

        $clean = array_values(array_unique($clean));

        return array_slice($clean, 0, 8);
    }

    private function buildPrompt(
        ?string $name,
        ?string $type,
        ?string $destination,
        ?string $duration,
        array $attractions,
        int $requiredAttractions,
        bool $strict
    ): string {
        $minWords = $strict ? 80 : 70;
        $maxWords = $strict ? 115 : 105;

        $allowedBlock = empty($attractions)
            ? '- Aucune attraction disponible'
            : implode("\n", array_map(fn($a) => '- ' . $a, $attractions));

        if ($requiredAttractions >= 3) {
            $must = "Mentionne EXACTEMENT TROIS noms d’attractions de la liste et ajoute UN détail concret (Type/Note) pour au moins DEUX d’entre elles.";
        } elseif ($requiredAttractions === 2) {
            $must = "Mentionne EXACTEMENT DEUX noms d’attractions de la liste et ajoute UN détail concret (Type/Note) pour CHACUNE d’elles.";
        } else {
            $must = "Si la liste est vide, rédige une description complète en deux phrases sans inventer de noms de lieux.";
        }

        return sprintf(
"Écris EXACTEMENT deux phrases complètes en français, avec un total de %d à %d mots.
Les deux phrases doivent se terminer par un point.
Retourne UNIQUEMENT les deux phrases, sans titre ni puces.

STYLE :
- annonce d’activité type Airbnb
- ton naturel, professionnel, pratique
- texte concret, pas scolaire

OBLIGATOIRE :
- %s
- utilise les détails fournis (Type/Note) seulement si cela s’intègre naturellement
- ne fais pas une liste mécanique d’informations
- ne répète pas bêtement le texte de la base

INTERDIT :
- “inoubliable”, “iconique”, “opportunité”, “vue panoramique”
- “découvrez”, “explorez”, “plongez dans”, “expérience immersive”
- formulations trop marketing ou vagues

STRUCTURE :
- Phrase 1 : à quoi ressemble l’activité, ambiance, point de départ ou premier repère
- Phrase 2 : suite du parcours, rythme, deuxième repère éventuel et lien avec la durée

ATTRACTIONS AUTORISÉES (noms + détails) :
%s

Détails de l’activité :
Nom : %s
Type : %s
Destination : %s
Durée : %s",
            $minWords,
            $maxWords,
            $must,
            $allowedBlock,
            $this->safe($name),
            $this->safe($type),
            $this->safe($destination),
            $this->safe($duration)
        );
    }

    private function buildRewritePrompt(string $badOutput, array $attractions, int $required): string
    {
        $names = [];

        foreach ($attractions as $line) {
            $name = $this->extractNameFromLine($line);
            if ($name !== '') {
                $names[] = $name;
            }
        }

        $names = array_values(array_unique($names));
        $namesLine = empty($names) ? '(aucune)' : implode(', ', $names);

        return sprintf(
"Réécris le texte ci-dessous en EXACTEMENT deux phrases complètes en français, avec 70 à 125 mots au total.
Les deux phrases doivent se terminer par un point.
Tu dois mentionner %d noms d’attractions depuis cet ensemble autorisé (copie l’orthographe exacte) : %s
Ajoute aussi au moins un détail fourni (Type/Note) pour chaque attraction mentionnée, sans rien inventer.
Retourne UNIQUEMENT les deux phrases.

TEXTE :
%s",
            $required,
            $namesLine,
            $badOutput
        );
    }

    private function extractNameFromLine(?string $line): string
    {
        $s = trim((string) $line);

        if ($s === '') {
            return '';
        }

        if (stripos($s, 'Nom:') !== false) {
            $pos = stripos($s, 'Nom:');
            $after = trim(substr($s, $pos + 4));
            $pipe = strpos($after, '|');

            return trim($pipe !== false ? substr($after, 0, $pipe) : $after);
        }

        if (stripos($s, 'Name:') !== false) {
            $pos = stripos($s, 'Name:');
            $after = trim(substr($s, $pos + 5));
            $pipe = strpos($after, '|');

            return trim($pipe !== false ? substr($after, 0, $pipe) : $after);
        }

        $pipe = strpos($s, '|');

        return trim($pipe !== false ? substr($s, 0, $pipe) : $s);
    }

    private function usesAtLeastNAttractions(string $text, array $attractionLines, int $n): bool
    {
        if ($n <= 0) {
            return true;
        }

        $low = mb_strtolower($text);
        $count = 0;

        foreach ($attractionLines as $line) {
            $name = $this->extractNameFromLine($line);

            if ($name === '') {
                continue;
            }

            if (str_contains($low, mb_strtolower($name))) {
                $count++;
                if ($count >= $n) {
                    return true;
                }
            }
        }

        return false;
    }

    private function looksTruncated(?string $s): bool
    {
        $t = trim((string) $s);

        if ($t === '') {
            return true;
        }

        if (!preg_match('/[.!?]$/', $t)) {
            return true;
        }

        $low = mb_strtolower($t);
        $badEnds = [' sur.', ' à.', ' de.', ' avec.', ' pour.', ' en.', ' dans.', ' incluant.', ' autour de.'];

        foreach ($badEnds as $ending) {
            if (str_ends_with($low, $ending)) {
                return true;
            }
        }

        return false;
    }

    private function enforceTwoSentences(?string $text): string
    {
        $s = trim(preg_replace('/\s+/', ' ', str_replace("\n", ' ', (string) $text)) ?? '');

        if ($s === '') {
            return '';
        }

        $parts = preg_split('/(?<=[\.\!\?])\s+/', $s) ?: [];

        if (count($parts) >= 2) {
            $out = trim($parts[0]) . ' ' . trim($parts[1]);
            if (!preg_match('/[.!?]$/', $out)) {
                $out .= '.';
            }
            return $out;
        }

        if (!preg_match('/[.!?]$/', $s)) {
            $s .= '.';
        }

        return $s;
    }

    private function clean(?string $s): string
    {
        return trim(preg_replace('/\s+/', ' ', str_replace("\n", ' ', (string) $s)) ?? '');
    }

    private function safe(?string $s): string
    {
        return trim((string) $s);
    }

    private function isRetryable(\RuntimeException $e): bool
    {
        $message = mb_strtolower((string) $e->getMessage());

        return str_contains($message, 'timeout')
            || str_contains($message, 'timed out')
            || str_contains($message, 'http 429')
            || str_contains($message, 'rate')
            || str_contains($message, 'http 503')
            || str_contains($message, 'overloaded')
            || str_contains($message, 'unavailable');
    }
}