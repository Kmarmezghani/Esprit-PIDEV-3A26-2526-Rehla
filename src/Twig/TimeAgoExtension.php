<?php
namespace App\Twig;

use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;

class TimeAgoExtension extends AbstractExtension
{
    public function getFilters()
    {
        return [
            new TwigFilter('timeAgo', [$this, 'timeAgo']),
        ];
    }

    public function timeAgo(\DateTimeInterface $date): string
    {
        $now = new \DateTime();
        $diff = $now->getTimestamp() - $date->getTimestamp();

        if ($diff < 60) {
            return "à l'instant";
        }

        if ($diff < 3600) {
            $minutes = floor($diff / 60);
            return $minutes . " min";
        }

        if ($diff < 86400) {
            $hours = floor($diff / 3600);
            return $hours . " h";
        }

        if ($diff < 172800) {
            return "Hier";
        }

        $days = floor($diff / 86400);
        return $days . " j";
    }
}
