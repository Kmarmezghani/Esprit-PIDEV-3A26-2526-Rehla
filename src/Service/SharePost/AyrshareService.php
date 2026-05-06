<?php

namespace App\Service\SharePost;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class AyrshareService
{
    private HttpClientInterface $client;
    private string $apiKey;

    public function __construct(HttpClientInterface $client)
    {
        $this->client = $client;
        $this->apiKey = '91007505-3C484B68-863FA35B-1DE5B9B0';
    }

    /**
     * @param array<int, string> $platforms
     * @param array<int, string> $mediaUrls
     * @return array<string, mixed>
     */
    public function sharePost(
        string $content,
        array $platforms,
        array $mediaUrls = []
    ): array {
        $payload = [
            'post' => $content,
            'platforms' => $platforms,
        ];

        if (!empty($mediaUrls)) {
            $payload['mediaUrls'] = $mediaUrls;
        }

        $response = $this->client->request(
            'POST',
            'https://api.ayrshare.com/api/post',
            [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => $payload,
            ]
        );

        $data = $response->getContent(false);

        return json_decode($data, true);
    }
}