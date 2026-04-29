<?php
namespace App\Service\SharePost;

use Cloudinary\Cloudinary;

class CloudinaryService
{
    private $cloudinary;

    public function __construct()
    {
        $this->cloudinary = new Cloudinary([
            'cloud' => [
                'cloud_name' => 'duz53i6eh',
                'api_key'    => '478786937755444',
                'api_secret' => 'OFnQpuuPdIb4a0y4iFxXs9ZUo8c',
            ]
        ]);
    }

    public function uploadImage($imagePath)
    {
        $result = $this->cloudinary->uploadApi()->upload($imagePath);

        return $result['secure_url']; // URL publique
    }
}