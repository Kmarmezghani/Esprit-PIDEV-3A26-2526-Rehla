<?php

namespace App\Controller;

use App\Entity\Reservation;
use App\Entity\Preference;
use App\Entity\Personne;
use App\Entity\Ticket;
use App\Entity\Ville;
use App\Entity\Activite;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

class RecommandationController extends AbstractController
{
    #[Route('/api/recommandation-ia', name: 'api_recommandation_ia', methods: ['POST'])]
    public function recommander(
        Request $request,
        EntityManagerInterface $em
    ): JsonResponse {
        $session = $request->getSession();
        $session->start();

        $lastCall = $session->get('ai_last_call', 0);
        if (time() - $lastCall < 15) {
            return new JsonResponse([
                'error' => 'Trop de demandes',
                'message' => "Attendez quelques secondes avant de réessayer."
            ], 429);
        }
        $session->set('ai_last_call', time());

        $userId = $session->get('user_id');
        $user   = $em->getRepository(Personne::class)->find($userId);

        if (!$userId || !$user) {
            return new JsonResponse(['error' => 'Non connecté'], 401);
        }

        $data      = json_decode($request->getContent(), true);
        $budgetMin = (float)($data['budgetMin'] ?? 0);
        $budgetMax = (float)($data['budgetMax'] ?? 9999);
        $dateDebut = $data['dateDebut'] ?? '';
        $dateFin   = $data['dateFin']   ?? '';

        // ── 1. Historique réservations ──────────────────────────────
        $reservations = $em->getRepository(Reservation::class)
                           ->findBy(['personne_id' => $user]);

        $historiqueTexte      = '';
        $destinationsDejaVues = [];
        $depenseTotale        = 0;

        foreach ($reservations as $res) {
            $dest = $res->getDestination()?->getNom() ?? 'Inconnue';
            if (!in_array($dest, $destinationsDejaVues)) {
                $destinationsDejaVues[] = $dest;
            }
            $depenseTotale += $res->getCoutTotal();
            $historiqueTexte .= sprintf(
                "- %s → %s | Destination: %s | Coût: %.2f TND\n",
                $res->getDateDebut()->format('d/m/Y'),
                $res->getDateFin()->format('d/m/Y'),
                $dest,
                $res->getCoutTotal()
            );
        }

        $nbRes  = count($reservations);
        $depMoy = $nbRes > 0 ? round($depenseTotale / $nbRes, 2) : 0;
        $destsStr = implode(', ', $destinationsDejaVues) ?: 'aucune';

        // ── 2. Préférences ──────────────────────────────────────────
        $pref = $em->getRepository(Preference::class)
                   ->findOneBy(['personne_id' => $user]);

        $typesVoyage    = $pref?->getTypesVoyage()    ?? 'non défini';
        $centresInteret = $pref?->getCentresInteret() ?? 'non défini';

        // ── 3. Tickets disponibles depuis la DB ─────────────────────
        $ticketsDisponibles = $em->getRepository(Ticket::class)
                                 ->findBy(['statut' => 'Disponible']);

        $ticketsData = [];
        foreach ($ticketsDisponibles as $ticket) {
            $ticketsData[] = [
                'id'          => $ticket->getId(),
                'type'        => $ticket->getType(),
                'prix'        => $ticket->getPrix(),
                'destination' => $ticket->getDestination()?->getNom() ?? 'Inconnue',
                'activite'    => $ticket->getActivite()?->getNom() ?? null,
            ];
        }

        // ── 4. Destinations disponibles depuis la DB ────────────────
        $villes = $em->getRepository(Ville::class)->findAll();
        $villesData = [];
        foreach ($villes as $ville) {
            $villesData[] = [
                'id'  => $ville->getId(),
                'nom' => $ville->getNom(),
            ];
        }

        // ── 5. Prompt ───────────────────────────────────────────────
        $ticketsJson = json_encode($ticketsData, JSON_UNESCAPED_UNICODE);
        $villesJson  = json_encode($villesData,  JSON_UNESCAPED_UNICODE);

        $prompt = <<<PROMPT
Tu es un conseiller de voyage expert. Voici le profil du client :

**Historique réservations ({$nbRes} voyages) :**
{$historiqueTexte}

**Destinations déjà visitées :** {$destsStr}
**Dépense moyenne par voyage :** {$depMoy} TND
**Budget souhaité :** entre {$budgetMin} et {$budgetMax} TND
**Période souhaitée :** du {$dateDebut} au {$dateFin}
**Types de voyage préférés :** {$typesVoyage}
**Centres d'intérêt :** {$centresInteret}

**Tickets disponibles dans notre système (utilise UNIQUEMENT ces tickets) :**
{$ticketsJson}

**Destinations disponibles dans notre système (utilise UNIQUEMENT ces destinations) :**
{$villesJson}

Propose UNE recommandation de voyage personnalisée en utilisant UNIQUEMENT les tickets et destinations disponibles ci-dessus.
Choisis une destination parmi la liste, et sélectionne des tickets dont le total est entre {$budgetMin} et {$budgetMax} TND.
Réponds UNIQUEMENT en JSON valide, sans markdown, sans backticks :
{
  "destination_id": 0,
  "destination": "Nom de la ville",
  "dateDebut": "JJ Mois AAAA",
  "dateFin": "JJ Mois AAAA",
  "meteo": "Description météo courte",
  "raison": "Explication 2-3 phrases",
  "tickets": [
    {"id": 0, "nom": "Type - Activité ou description", "prix": 0}
  ],
  "total": 0
}
Le total doit être dans la fourchette {$budgetMin}-{$budgetMax} TND.
Utilise uniquement des IDs de tickets et destinations qui existent dans les listes fournies.
PROMPT;

        // ── 6. Appel Gemini ─────────────────────────────────────────
        $apiKey = $_ENV['GEMINI_API_KEY2'] ?? '';

        $ch = curl_init(
            'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=' . $apiKey
        );
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => false,
            CURLOPT_TIMEOUT        => 90,
            CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
            CURLOPT_POSTFIELDS     => json_encode([
                "contents" => [[
                    "parts" => [["text" => $prompt]]
                ]],
                "generationConfig" => [
                    "temperature"      => 0.2,
                    "maxOutputTokens"  => 8192,
                    
                ]
            ]),
        ]);

       $raw      = curl_exec($ch);
$curlErr  = curl_error($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);



        // ── 7. Error handling ───────────────────────────────────────
        if ($curlErr) {
            return new JsonResponse(['error' => 'cURL: ' . $curlErr], 500);
        }

        if ($httpCode === 429) {
            return new JsonResponse([
                'error'   => 'IA temporairement indisponible',
                'message' => "Trop de demandes envoyées à l'IA. Réessayez dans 1 minute."
            ], 429);
        }

        if ($httpCode >= 500) {
    return new JsonResponse([
        'debug_gemini_error' => true,
        'httpCode' => $httpCode,
        'raw'      => json_decode($raw, true) ?? $raw,
    ], 500);
}

        if ($httpCode !== 200) {
            return new JsonResponse([
                'error'    => 'Erreur API Gemini',
                'httpCode' => $httpCode,
                'response' => $raw
            ], 400);
        }

        // ── 8. Parse Gemini response ────────────────────────────────
        $apiResp = json_decode($raw, true);
        $text    = $apiResp['candidates'][0]['content']['parts'][0]['text'] ?? null;

        if (!$text) {
            return new JsonResponse(['error' => 'Pas de texte dans la réponse', 'apiResp' => $apiResp], 500);
        }

        $text = trim($text);
        $text = preg_replace('/```(json)?/i', '', $text);
        if (preg_match('/\{.*\}/s', $text, $matches)) {
            $text = $matches[0];
        }

        $recommendation = json_decode($text, true);

if (!$recommendation || !isset($recommendation['destination'])) {
    return new JsonResponse(['error' => 'JSON invalide', 'raw' => $text], 500); // ← 'raw' is already there
}

        $recommendation['contexte'] = [
            'nbReservations'       => $nbRes,
            'destinationsDejaVues' => $destinationsDejaVues,
            'depenseMoyenne'       => $depMoy,
        ];

        return new JsonResponse($recommendation);
    }

    // ── Route to create reservation from AI recommendation ──────────
    #[Route('/api/recommandation-ia/creer', name: 'api_recommandation_creer', methods: ['POST'])]
    public function creerDepuisIA(
        Request $request,
        EntityManagerInterface $em
    ): JsonResponse {
        $session = $request->getSession();
        $session->start();

        $userId = $session->get('user_id');
        $user   = $em->getRepository(Personne::class)->find($userId);

        if (!$userId || !$user) {
            return new JsonResponse(['error' => 'Non connecté'], 401);
        }

        $data = json_decode($request->getContent(), true);

        $destinationId = $data['destination_id'] ?? null;
        $ticketIds     = $data['ticket_ids']     ?? [];
        $dateDebut     = $data['dateDebut']      ?? null;
        $dateFin       = $data['dateFin']        ?? null;

        if (!$destinationId || empty($ticketIds) || !$dateDebut || !$dateFin) {
            return new JsonResponse(['error' => 'Données manquantes'], 400);
        }

        $destination = $em->getRepository(Ville::class)->find($destinationId);
        if (!$destination) {
            return new JsonResponse(['error' => 'Destination introuvable'], 404);
        }

        // ── Create reservation ──────────────────────────────────────
        $reservation = new Reservation();
        $reservation->setPersonne_id($user);
        $reservation->setDestination($destination);
        $reservation->setDateDebut(new \DateTime($dateDebut));
        $reservation->setDateFin(new \DateTime($dateFin));
        $reservation->setStatut('réservée');

        $total     = 0;
        $nbTickets = 0;

        foreach ($ticketIds as $ticketId) {
            $ticket = $em->getRepository(Ticket::class)->find($ticketId);
            if ($ticket && $ticket->getStatut() === 'Disponible') {
                $ticket->setStatut('Reservé');
                $ticket->setReservation_id($reservation);
                $reservation->addTicket($ticket);
                $total += $ticket->getPrix();
                $nbTickets++;
                $em->persist($ticket);
            }
        }

        $reservation->setCoutTotal($total);
        $reservation->setNb_tickets($nbTickets);

        $em->persist($reservation);
        $em->flush();

        return new JsonResponse([
            'success'        => true,
            'reservation_id' => $reservation->getId(),
            'message'        => 'Réservation créée avec succès !'
        ]);
    }
}