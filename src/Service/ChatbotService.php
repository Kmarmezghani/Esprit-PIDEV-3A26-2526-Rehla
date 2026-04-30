<?php

namespace App\Service;

use App\Repository\VilleRepository;
use App\Repository\PaysRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Throwable;

class ChatbotService
{
    /** gemini-pro retiré par Google (404). Ordre identique à UserRiskAnalysisService::GEMINI_MODELS. */
    private const GEMINI_MODELS_TRY_ORDER = [
        'gemini-2.0-flash',
        'gemini-1.5-flash',
        'gemini-2.5-flash',
    ];

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $geminiApiKey4,
        private KnapsackOptimizerService $optimizer,
        private VilleRepository $villeRepository,
        private PaysRepository $paysRepository,
        private \App\Repository\PersonneRepository $personneRepository
    ) {}

    public function getResponse(string $userMessage, ?int $userId = null): string
    {
        try {
            // 1. Fetch current database state for the context (Simplified)
            $allPays = $this->paysRepository->findAll();
            $dbContent = "Catalogue Rehla (Destinations, Attractions, Activités Guidées) :\n";
            foreach ($allPays as $p) {
                $dbContent .= "- " . $p->getNom() . " :\n";
                foreach ($p->getVilles() as $v) {
                    $attractions = [];
                    foreach ($v->getAttractions() as $a) {
                        $attractions[] = $a->getNom();
                    }
                    
                    $activites = [];
                    foreach ($v->getActivites() as $act) {
                        $guideName = $act->getGuide() ? " (Guide: " . $act->getGuide()->getPrenom() . " " . $act->getGuide()->getNom() . ")" : "";
                        $activites[] = $act->getNom() . $guideName;
                    }

                    $attrText = !empty($attractions) ? " | Attractions: " . implode(', ', $attractions) : "";
                    $actText = !empty($activites) ? " | Séjours/Activités Guidées: " . implode(', ', $activites) : "";
                    
                    $dbContent .= "   * " . $v->getNom() . $attrText . $actText . "\n";
                }
            }

            // 2. Detect if the user is asking for a budget plan
            $optimizationResult = null;
            $villes = $this->villeRepository->findAll();
            foreach ($villes as $ville) {
                if (stripos($userMessage, $ville->getNom()) !== false) {
                    if (preg_match('/(\d+)/', $userMessage, $matches)) {
                        $budget = (float) $matches[1];
                        $optimizationResult = $this->optimizer->optimize($ville, $budget);
                        break;
                    }
                }
            }

            // 3. Personalize Context with User Preferences
            $prefText = "";
            if ($userId) {
                $personne = $this->personneRepository->find($userId);
                if ($personne && count($personne->getPreferences()) > 0) {
                    $pref = $personne->getPreferences()->first();
                    $prefText = "\n\nINFORMATIONS SUR L'UTILISATEUR ACTUEL :\n"
                        . "- Budget Max : " . ($pref->getBudgetMax() ? $pref->getBudgetMax() . "€" : "Non précisé") . "\n"
                        . "- Types de voyages préférés : " . ($pref->getTypesVoyage() ?: "Non précisé") . "\n"
                        . "- Centres d'intérêt : " . ($pref->getCentresInteret() ?: "Non précisé") . "\n"
                        . "-> RÈGLE : Utilise ces informations pour personnaliser tes recommandations (propose des destinations ou activités qui correspondent à ses goûts et son budget).";
                }
            }

            // 4. Prepare the System Prompt
            $systemPrompt = "Tu es l'assistant Rehla.
            RÈGLES :
            1. Utilise UNIQUEMENT le catalogue suivant : \n" . $dbContent . "
            2. Sois direct et concis (pas de longs paragraphes).
            3. Si on te demande des recommandations de séjours, des visites ou des activités, propose les options disponibles dans la ville (en mentionnant le nom du guide si c'est une activité guidée).
            4. Si budget mentionné, utilise : " . ($optimizationResult ? json_encode($optimizationResult['selected']) : 'aucun')
            . $prefText;

            return $this->callGemini($systemPrompt, $userMessage);
        } catch (\Exception $e) {
            return "Erreur Service : " . $e->getMessage();
        }
    }

    private function callGemini(string $system, string $user): string
    {
        $prompt = $system . "\n\nUtilisateur : " . $user;
        $lastError = '';

        foreach (self::GEMINI_MODELS_TRY_ORDER as $model) {
            $url = 'https://generativelanguage.googleapis.com/v1beta/models/'
                . $model . ':generateContent?key=' . $this->geminiApiKey4;

            try {
                $response = $this->httpClient->request('POST', $url, [
                    'headers' => [
                        'Content-Type' => 'application/json',
                    ],
                    'json' => [
                        'contents' => [['parts' => [['text' => $prompt]]]],
                        'generationConfig' => [
                            'temperature' => 0.4,
                            'maxOutputTokens' => 1024,
                        ],
                    ],
                    'timeout' => 30,
                ]);

                $content = $response->getContent(false);
                $statusCode = $response->getStatusCode();

                if ($statusCode === 200) {
                    $data = json_decode($content, true);
                    $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? null;
                    if ($text !== null && $text !== '') {
                        return $text;
                    }

                    return "Désolé, je ne peux pas répondre pour le moment.";
                }

                $lastError = "Erreur API ({$statusCode}) modèle {$model} : " . substr($content, 0, 200);
                if (in_array($statusCode, [404, 429, 503, 500])) {
                    continue;
                }

                return $lastError;
            } catch (Throwable $e) {
                $lastError = 'Erreur Technique : ' . $e->getMessage();
                continue;
            }
        }

        return "DEBUG Error: " . $lastError;
    }
}
