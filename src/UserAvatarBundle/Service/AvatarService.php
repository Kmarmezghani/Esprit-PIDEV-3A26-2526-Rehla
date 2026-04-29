<?php

namespace App\UserAvatarBundle\Service;

use Symfony\Component\HttpFoundation\Response;

class AvatarService
{
    private const PALETTE = [
        '#223f91', '#1a5276', '#117a65', '#6c3483',
        '#935116', '#1a252f', '#922b21', '#1f618d',
        '#196f3d', '#7d6608', '#0e6655', '#784212',
    ];

    public function generateSvg(string $prenom, string $nom): string
    {
        $initials = $this->initials($prenom, $nom);
        $color    = $this->color($prenom . $nom);

        return <<<SVG
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" style="width:100%;height:100%;display:block;">
            <circle cx="50" cy="50" r="50" fill="{$color}"/>
            <text x="50" y="50"
                  dominant-baseline="central"
                  text-anchor="middle"
                  font-family="Segoe UI, Arial, sans-serif"
                  font-size="36"
                  font-weight="700"
                  fill="#ffffff">{$initials}</text>
        </svg>
        SVG;
    }

    public function generateResponse(string $prenom, string $nom): Response
    {
        return new Response($this->generateSvg($prenom, $nom), 200, [
            'Content-Type'  => 'image/svg+xml',
            'Cache-Control' => 'public, max-age=604800',
        ]);
    }

    private function initials(string $prenom, string $nom): string
    {
        $p = mb_strtoupper(mb_substr(trim($prenom), 0, 1));
        $n = mb_strtoupper(mb_substr(trim($nom),    0, 1));
        return $p . $n;
    }

    private function color(string $seed): string
    {
        $index = abs(crc32($seed)) % count(self::PALETTE);
        return self::PALETTE[$index];
    }
}
