<?php
namespace App\Service\FlaskClient;
use Symfony\Contracts\HttpClient\HttpClientInterface;


class ToxicityChecker
{
    private $client;

    public function __construct(HttpClientInterface $client)
    {
        $this->client = $client;
    }

    public function check(string $text): array
    {
        $response = $this->client->request('POST', 'http://127.0.0.1:5000/predict', [
            'json' => ['text' => $text]
        ]);

        return $response->toArray();
    }
}
