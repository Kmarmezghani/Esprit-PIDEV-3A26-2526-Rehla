<?php

namespace App\UserAvatarBundle\Twig;

use App\UserAvatarBundle\Service\AvatarService;
use Twig\Extension\AbstractExtension;
use Twig\TwigFunction;

class AvatarExtension extends AbstractExtension
{
    public function __construct(private AvatarService $avatarService) {}

    public function getFunctions(): array
    {
        return [
            new TwigFunction('avatar_svg', [$this, 'avatarSvg']),
        ];
    }

    /**
     * Returns an inline SVG avatar string for use in templates:
     *   {{ avatar_svg(user.prenom, user.nom) | raw }}
     */
    public function avatarSvg(string $prenom, string $nom): string
    {
        return $this->avatarService->generateSvg($prenom, $nom);
    }
}
