<?php

namespace App\Service;

use App\Repository\VilleRepository;
use App\Repository\PaysRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Throwable;

class ChatbotService
{
    private const ENDPOINT = 'https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent?key=';

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $geminiApiKey4,
        private KnapsackOptimizerService $optimizer,
        private VilleRepository $villeRepository,
        private PaysRepository $paysRepository
    ) {}

    public function getResponse(string $userMessage): string
    {
        try {
            // 1. Fetch current database state for the context (Simplified)
            $allPays = $this->paysRepository->findAll();
            $dbContent = "Destinations Rehla :\n";
            foreach ($allPays as $p) {
                $villes = $p->getVilles();
                $vNames = [];
                foreach ($villes as $v) { $vNames[] = $v->getNom(); }
                $dbContent .= "- " . $p->getNom() . " (" . implode(', ', $vNames) . ")\n";
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

            // 3. Prepare the System Prompt
            $systemPrompt = "Tu es l'assistant Rehla. RÉPONDS TRÈS COURT.
            RÈGLES :
            1. Utilise UNIQUEMENT ces destinations : " . $dbContent . "
            2. Pas de longs discours.
            3. Si budget mentionné, utilise : " . ($optimizationResult ? json_encode($optimizationResult['selected']) : 'aucun');

            return $this->callGemini($systemPrompt, $userMessage);
        } catch (\Exception $e) {
            return "Erreur Service : " . $e->getMessage();
        }
    }

    private function callGemini(string $system, string $user): string
    {
        $prompt = $system . "\n\nUtilisateur : " . $user;
        $url = self::ENDPOINT . $this->geminiApiKey4;

        try {
            $response = $this->httpClient->request('POST', $url, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [['parts' => [['text' => $prompt]]]]
                ],
                'timeout' => 30
            ]);

            $content = $response->getContent(false);
            $statusCode = $response->getStatusCode();

            if ($statusCode !== 200) {
                return "Erreur API ({$statusCode}) : " . substr($content, 0, 100);
            }

            $data = json_decode($content, true);
            return $data['candidates'][0]['content']['parts'][0]['text'] ?? "Désolé, je ne peux pas répondre pour le moment.";
        } catch (Throwable $e) {
            return "Erreur Technique : " . $e->getMessage();
        }
    }
}
