<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;
use Symfony\Component\Filesystem\Filesystem;

class UnsplashService
{
    private $client;
    private $accessKey;
    private $projectDir;

    public function __construct(HttpClientInterface $client, ParameterBagInterface $params)
    {
        $this->client = $client;
        $this->accessKey = $params->get('unsplash_access_key');
        $this->projectDir = $params->get('kernel.project_dir');
    }

    /**
     * Fetches a random photo from Unsplash based on a query and saves it locally.
     * Returns the filename of the saved image.
     */
    public function fetchAndSaveImage(string $query, string $filenamePrefix): ?string
    {
        if (!$this->accessKey || $this->accessKey === 'your_key_here' || empty($this->accessKey)) {
            return null;
        }

        try {
            // 1. Request a random photo from Unsplash
            $response = $this->client->request('GET', 'https://api.unsplash.com/photos/random', [
                'query' => [
                    'query' => $query,
                    'client_id' => $this->accessKey,
                    'orientation' => 'landscape'
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                return null;
            }

            $data = $response->toArray();
            $imageUrl = $data['urls']['regular'];

            // 2. Download the actual image content
            $imageResponse = $this->client->request('GET', $imageUrl);
            $imageContent = $imageResponse->getContent();

            // 3. Ensure the upload directory exists
            $uploadDir = $this->projectDir . '/public/uploads/destinations/';
            $filesystem = new Filesystem();
            if (!$filesystem->exists($uploadDir)) {
                $filesystem->mkdir($uploadDir);
            }

            // 4. Create a unique, safe filename
            $safePrefix = preg_replace('/[^a-z0-9]/i', '_', $filenamePrefix);
            $filename = strtolower($safePrefix) . '_' . uniqid() . '.jpg';

            // 5. Save the file to disk
            $filesystem->dumpFile($uploadDir . $filename, $imageContent);

            return $filename;

        } catch (\Exception $e) {
            // Error handling
            return null;
        }
    }
}
