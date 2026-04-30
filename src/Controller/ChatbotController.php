<?php

namespace App\Controller;

use App\Service\ChatbotService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

class ChatbotController extends AbstractController
{
    #[Route('/chatbot/chat', name: 'app_chatbot_chat', methods: ['POST'])]
    public function chat(Request $request, ChatbotService $chatbotService): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $message = $data['message'] ?? '';

        if (empty($message)) {
            return $this->json(['response' => 'Veuillez entrer un message.'], 400);
        }

        $userId = $request->getSession()->get('user_id');
        $response = $chatbotService->getResponse($message, $userId);

        return $this->json(['response' => $response]);
    }
}
