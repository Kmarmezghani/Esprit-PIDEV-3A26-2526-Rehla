<?php
namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class AyrshareService
{
    private $client;
    private $apiKey;

    public function __construct(HttpClientInterface $client)
    {
        $this->client = $client;
        $this->apiKey = '91007505-3C484B68-863FA35B-1DE5B9B0';
    }

public function sharePost($content, $platforms, $mediaUrls = [])
{
    $payload = [
        'post' => $content,
        'platforms' => $platforms,
    ];

    if (!empty($mediaUrls)) {
        $payload['mediaUrls'] = $mediaUrls;
    }

    $response = $this->client->request('POST', 'https://api.ayrshare.com/api/post', [
        'headers' => [
            'Authorization' => 'Bearer ' . $this->apiKey,
            'Content-Type'  => 'application/json'
        ],
        'json' => $payload
    ]);

    $data = $response->getContent(false);

    return json_decode($data, true);
}
}