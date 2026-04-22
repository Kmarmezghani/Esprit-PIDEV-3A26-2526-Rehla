<?php

namespace App\Service;

use App\Entity\Personne;

class UserRiskAnalysisService
{
    private const OPENROUTER_MODELS = [
        'meta-llama/llama-3.1-8b-instruct:free',
        'mistralai/mistral-7b-instruct:free',
        'openrouter/auto',
    ];

    private const GEMINI_MODELS = [
        'gemini-2.0-flash',
        'gemini-1.5-flash',
        'gemini-2.5-flash',
    ];

    private string $lastDebug = '';

    public function __construct(
        private string $geminiApiKey,
        private string $geminiApiKey2 = '',
        private string $openRouterApiKey = ''
    ) {}

    public function getLastDebug(): string { return $this->lastDebug; }

    /** @param Personne[] $users */
    public function analyzeUsers(array $users): array
    {
        if (empty($users)) return [];

        $usersData = [];
        $now = new \DateTime();

        foreach ($users as $u) {
            if ($u->getRole() === 'ADMIN') continue;

            $anciennete = $u->getDateInscription()
                ? (int) $u->getDateInscription()->diff($now)->days : 0;

            $inactivite = $u->getDerniereConnexion()
                ? (int) $u->getDerniereConnexion()->diff($now)->days
                : ($anciennete > 0 ? $anciennete : 0);

            $usersData[] = [
                'id'               => $u->getId(),
                'nom'              => $u->getNom() . ' ' . $u->getPrenom(),
                'role'             => $u->getRole(),
                'statut'           => $u->getStatutCompte(),
                'postsSuspects'    => $u->getNbPostsSuspects(),
                'anciennete_jours' => $anciennete,
                'inactivite_jours' => $inactivite,
            ];
        }

        if (empty($usersData)) return [];

        $prompt = 'Tu es un système de sécurité pour une agence de voyage appelée Rehla.
Analyse les données suivantes des utilisateurs et attribue à chacun un niveau de risque.

Niveaux possibles : FAIBLE, MOYEN, ELEVE
Critères :
- postsSuspects > 3 : risque ELEVE
- inactivite_jours > 60 : risque MOYEN
- statut SUSPENDU : risque ELEVE
- combinaison de plusieurs facteurs : risque plus élevé

Données utilisateurs :
' . json_encode($usersData, JSON_UNESCAPED_UNICODE) . '

Réponds UNIQUEMENT avec un tableau JSON valide, sans markdown, sans explication :
[{"id": 1, "niveau": "FAIBLE", "raison": "Aucun comportement suspect."}, ...]';

        // 1. Try OpenRouter first
        if (!empty($this->openRouterApiKey)) {
            $result = $this->tryOpenRouter($prompt);
            if (!empty($result)) return $result;
        }

        // 2. Fallback to Gemini
        $keys = array_filter([$this->geminiApiKey, $this->geminiApiKey2]);
        foreach ($keys as $apiKey) {
            foreach (self::GEMINI_MODELS as $model) {
                $result = $this->tryGemini($apiKey, $model, $prompt);
                if (!empty($result)) return $result;
            }
        }

        return [];
    }

    private function tryOpenRouter(string $prompt): array
    {
        $payload = json_encode([
            'model'    => self::OPENROUTER_MODELS[0],
            'messages' => [
                ['role' => 'user', 'content' => $prompt],
            ],
            'temperature' => 0.1,
            'max_tokens'  => 4096,
            'provider'    => ['allow_fallbacks' => true],
        ]);

        foreach (self::OPENROUTER_MODELS as $model) {
            $payloadArr = json_decode($payload, true);
            $payloadArr['model'] = $model;
            $body = json_encode($payloadArr);

            $ch = curl_init('https://openrouter.ai/api/v1/chat/completions');
            curl_setopt_array($ch, [
                CURLOPT_POST           => true,
                CURLOPT_POSTFIELDS     => $body,
                CURLOPT_HTTPHEADER     => [
                    'Content-Type: application/json',
                    'Authorization: Bearer ' . $this->openRouterApiKey,
                    'HTTP-Referer: http://localhost',
                    'X-Title: Rehla',
                ],
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_TIMEOUT        => 30,
                CURLOPT_SSL_VERIFYPEER => false,
                CURLOPT_SSL_VERIFYHOST => false,
            ]);
            $raw      = curl_exec($ch);
            $httpCode = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $curlErr  = curl_error($ch);
            curl_close($ch);

            $this->lastDebug = "OPENROUTER MODEL=$model | HTTP=$httpCode | ERR=$curlErr";

            if ($httpCode === 200 && $raw) {
                $data   = json_decode($raw, true);
                $text   = $data['choices'][0]['message']['content'] ?? '';
                $result = $this->parseJson($text);
                if (!empty($result)) {
                    $this->lastDebug .= ' | OK=' . count($result) . ' users';
                    return $result;
                }
                $this->lastDebug .= ' | PARSE_FAIL | TEXT=' . substr($text, 0, 200);
                break; // got 200 but parse failed, don't retry other models
            }
        }

        return [];
    }

    private function tryGemini(string $apiKey, string $model, string $prompt): array
    {
        $payload = json_encode([
            'contents'         => [['parts' => [['text' => $prompt]]]],
            'generationConfig' => ['temperature' => 0.1, 'maxOutputTokens' => 4096],
        ]);

        $url = 'https://generativelanguage.googleapis.com/v1beta/models/'
             . $model . ':generateContent?key=' . $apiKey;

        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => $payload,
            CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => 30,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => false,
        ]);
        $raw      = curl_exec($ch);
        $httpCode = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlErr  = curl_error($ch);
        curl_close($ch);

        $this->lastDebug = "GEMINI KEY=" . substr($apiKey, 0, 8) . "... MODEL=$model | HTTP=$httpCode | ERR=$curlErr";

        if ($httpCode !== 200 || !$raw) return [];

        $data   = json_decode($raw, true);
        $text   = $data['candidates'][0]['content']['parts'][0]['text'] ?? '';
        $result = $this->parseJson($text);

        if (!empty($result)) {
            $this->lastDebug .= ' | OK=' . count($result) . ' users';
        } else {
            $this->lastDebug .= ' | PARSE_FAIL | TEXT=' . substr($text, 0, 200);
        }

        return $result;
    }

    private function parseJson(string $text): array
    {
        $text = preg_replace('/```json\s*|```/', '', $text);
        $text = trim($text);

        $parsed = json_decode($text, true);

        if (!is_array($parsed)) {
            if (preg_match('/(\[[\s\S]*\])/u', $text, $m)) {
                $parsed = json_decode($m[1], true);
            }
        }

        if (!is_array($parsed)) return [];

        $result = [];
        foreach ($parsed as $item) {
            if (isset($item['id'], $item['niveau'], $item['raison'])) {
                // normalize: ELEVE → ÉLEVÉ for display
                $niveau = strtoupper(trim($item['niveau']));
                $niveau = str_replace(['ELEVE', 'ELEVÉ', 'ÉLEVÉ'], 'ÉLEVÉ', $niveau);
                $niveau = str_replace(['MOYEN'], 'MOYEN', $niveau);
                $niveau = str_replace(['FAIBLE'], 'FAIBLE', $niveau);
                if (!in_array($niveau, ['ÉLEVÉ', 'MOYEN', 'FAIBLE'])) {
                    $niveau = 'FAIBLE';
                }
                $result[(int)$item['id']] = [
                    'niveau' => $niveau,
                    'raison' => $item['raison'],
                ];
            }
        }
        return $result;
    }
}