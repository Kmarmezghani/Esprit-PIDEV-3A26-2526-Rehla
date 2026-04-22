<?php

namespace App\UserMailerBundle\Service;

use App\Entity\Personne;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

/**
 * Service central du UserMailerBundle.
 * Tous les emails liés à la gestion des utilisateurs passent par ici.
 */
class UserMailerService
{
    private const FROM = 'rehla.noreply@gmail.com';
    private const BRAND_COLOR = '#223f91';

    public function __construct(
        private MailerInterface $mailer,
        private UrlGeneratorInterface $router
    ) {}

    // ── Email de bienvenue à l'inscription ──────────────────────────
    public function sendWelcomeEmail(Personne $personne): void
    {
        $role = $personne->getRole() === 'GUIDE' ? 'Guide' : 'Voyageur';

        $html = $this->wrapHtml(
            'Bienvenue sur Rehla ! 🎉',
            '<p>Bonjour <strong>' . htmlspecialchars($personne->getPrenom() . ' ' . $personne->getNom()) . '</strong>,</p>
             <p>Votre compte <strong>' . $role . '</strong> a été créé avec succès. Vous faites maintenant partie de la communauté Rehla !</p>
             <p>Commencez à explorer nos destinations et activités dès maintenant.</p>',
            'Se connecter',
            $this->router->generate('app_login', [], UrlGeneratorInterface::ABSOLUTE_URL)
        );

        $this->send($personne->getEmail(), 'Bienvenue sur Rehla – Votre compte est prêt !', $html);
    }

    // ── Email de suspension de compte ───────────────────────────────
    public function sendAccountSuspendedEmail(Personne $personne, ?\DateTimeInterface $finSuspension = null): void
    {
        $dureeMsg = $finSuspension
            ? 'La suspension prend fin le <strong>' . $finSuspension->format('d/m/Y à H:i') . '</strong>.'
            : 'La durée de la suspension est indéterminée.';

        $html = $this->wrapHtml(
            'Votre compte a été suspendu',
            '<p>Bonjour <strong>' . htmlspecialchars($personne->getPrenom() . ' ' . $personne->getNom()) . '</strong>,</p>
             <p>Votre compte Rehla a été temporairement <strong style="color:#dc3545;">suspendu</strong> par un administrateur.</p>
             <p>' . $dureeMsg . '</p>
             <p>Si vous pensez qu\'il s\'agit d\'une erreur, contactez notre support.</p>',
            'Contacter le support',
            'mailto:rehla.noreply@gmail.com'
        );

        $this->send($personne->getEmail(), 'Votre compte Rehla a été suspendu', $html);
    }

    // ── Email de relance inactivité ─────────────────────────────────
    public function sendInactivityEmail(Personne $personne, int $joursInactivite): void
    {
        $html = $this->wrapHtml(
            'Vous nous manquez ! 👋',
            '<p>Bonjour <strong>' . htmlspecialchars($personne->getPrenom() . ' ' . $personne->getNom()) . '</strong>,</p>
             <p>Nous avons remarqué que vous ne vous êtes pas connecté(e) depuis <strong>' . $joursInactivite . ' jours</strong>.</p>
             <p>Votre compte a été marqué comme <strong style="color:#fd7e14;">inactif</strong>. Reconnectez-vous pour le réactiver automatiquement !</p>
             <p>De nouvelles destinations et activités vous attendent sur Rehla.</p>',
            'Me reconnecter',
            $this->router->generate('app_login', [], UrlGeneratorInterface::ABSOLUTE_URL)
        );

        $this->send($personne->getEmail(), 'Votre compte Rehla est inactif – Revenez nous voir !', $html);
    }

    // ── Helpers privés ──────────────────────────────────────────────
    private function send(string $to, string $subject, string $html): void
    {
        try {
            $email = (new Email())
                ->from(self::FROM)
                ->to($to)
                ->subject($subject)
                ->html($html);
            $this->mailer->send($email);
        } catch (\Exception) {
            // Ne jamais bloquer l'UX si le mailer échoue
        }
    }

    private function wrapHtml(string $title, string $body, string $btnText, string $btnUrl): string
    {
        return '<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>
        <div style="font-family:Poppins,Arial,sans-serif;max-width:560px;margin:auto;padding:0;background:#f8f9fa;">
            <div style="background:' . self::BRAND_COLOR . ';padding:28px 32px;text-align:center;border-radius:10px 10px 0 0;">
                <h1 style="color:#fff;margin:0;font-size:20px;font-weight:700;">✈ Rehla</h1>
            </div>
            <div style="background:#fff;padding:32px;border-radius:0 0 10px 10px;border:1px solid #e0e0e0;border-top:none;">
                <h2 style="color:' . self::BRAND_COLOR . ';font-size:18px;margin-top:0;">' . $title . '</h2>
                ' . $body . '
                <div style="text-align:center;margin:28px 0 16px;">
                    <a href="' . $btnUrl . '" style="background:' . self::BRAND_COLOR . ';color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:600;font-size:14px;">' . $btnText . '</a>
                </div>
                <p style="color:#aaa;font-size:11px;text-align:center;margin-top:24px;">Rehla Travel Agency &mdash; Ne répondez pas à cet email.</p>
            </div>
        </div></body></html>';
    }
}
