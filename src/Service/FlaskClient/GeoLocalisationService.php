<?php

namespace App\Service\FlaskClient;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\Mime\Part\DataPart;
use Symfony\Component\Mime\Part\Multipart\FormDataPart;

class GeoLocalisationService
{
    private HttpClientInterface $client;

    public function __construct(HttpClientInterface $client)
    {
        $this->client = $client;
    }

   public function localize(string $imagePath): array
{
    $formData = new FormDataPart([
        'image' => new DataPart(fopen($imagePath, 'r'), 'image.jpg')
    ]);

    try {
        $response = $this->client->request('POST', 'http://127.0.0.1:5001/localize', [
            'headers' => $formData->getPreparedHeaders()->toArray(),
            'body' => $formData->bodyToIterable(),
        ]);

        $content = $response->getContent(false); 

        return json_decode($content, true);

    } catch (\Exception $e) {
        throw new \Exception("Flask API error: " . $e->getMessage());
    }
}


}
