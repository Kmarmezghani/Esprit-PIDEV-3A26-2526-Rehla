<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class WeatherController extends AbstractController
{
   #[Route('/meteo/{ville}/{date}', name: 'meteo')]
public function meteo($ville, $date, HttpClientInterface $client): JsonResponse
{
    $apiKey = $_ENV['OPENWEATHER_API_KEY'];

    try {
        $response = $client->request('GET',
            "https://api.openweathermap.org/data/2.5/forecast", [
                'query' => [
                    'q'     => $ville,
                    'appid' => $apiKey,
                    'units' => 'metric',
                    'lang'  => 'fr'
                ]
            ]
        );

        $data = $response->toArray();

        foreach ($data['list'] as $forecast) {
            if (str_starts_with($forecast['dt_txt'], $date)) {
                return $this->json([
    'temp'        => round($forecast['main']['temp']),
    'description' => $forecast['weather'][0]['description'],
    'icon'        => $forecast['weather'][0]['icon'],
    'humidity'    => $forecast['main']['humidity'],          // ← add this
    'wind'        => round($forecast['wind']['speed'] * 3.6) // ← m/s → km/h
]);
            }
        }

        // ✅ Fallback: use the first available forecast entry
        $first = $data['list'][0];
       return $this->json([
    'temp'        => round($forecast['main']['temp']),
    'description' => $forecast['weather'][0]['description'],
    'icon'        => $forecast['weather'][0]['icon'],
    'humidity'    => $forecast['main']['humidity'],          // ← add this
    'wind'        => round($forecast['wind']['speed'] * 3.6) // ← m/s → km/h
]);

    } catch (\Exception $e) {
       return $this->json([
    'temp'        => round($forecast['main']['temp']),
    'description' => $forecast['weather'][0]['description'],
    'icon'        => $forecast['weather'][0]['icon'],
    'humidity'    => $forecast['main']['humidity'],          // ← add this
    'wind'        => round($forecast['wind']['speed'] * 3.6) // ← m/s → km/h
]);
    }
}
}