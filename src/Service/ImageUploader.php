<?php
namespace App\Service;

use Symfony\Component\HttpFoundation\File\UploadedFile;

class ImageUploader
{
    public function upload(?UploadedFile $file, string $dir): ?string
    {
        if (!$file) return null;

        $newFilename = uniqid().'.'.$file->guessExtension();

        $file->move($dir, $newFilename);

        return $dir . DIRECTORY_SEPARATOR . $newFilename;
    }
}
