<?php

namespace App\Controller;

use App\Entity\Guide;
use App\Entity\Personne;
use App\Entity\Preference;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

class UserController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  Helper : vérifie qu'un utilisateur est bien connecté
    // ─────────────────────────────────────────────────────────────
    private function requireLogin(Request $request): ?Response
    {
        if (!$request->getSession()->get('user_id')) {
            return $this->redirectToRoute('app_login');
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  CONNEXION
    // ─────────────────────────────────────────────────────────────
    #[Route('/connexion', name: 'app_login')]
    public function login(Request $request, EntityManagerInterface $em): Response
    {
        $session = $request->getSession();

        // Déjà connecté → redirection
        if ($session->get('user_id')) {
            return $this->redirectToRoute(
                $session->get('user_role') === 'ADMIN' ? 'admin_users' : 'home'
            );
        }

        $error = null;

        if ($request->isMethod('POST')) {
            $email    = trim($request->request->get('email', ''));
            $password = $request->request->get('motDePasse', '');

            /** @var Personne|null $personne */
            $personne = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);

            if ($personne && $personne->getMotDePasse() === $password) {
                if ($personne->getStatutCompte() === 'SUSPENDU') {
                    $error = 'Votre compte a été suspendu. Contactez l\'administrateur.';
                } else {
                    // Ouverture de session
                    $session->set('user_id',     $personne->getId());
                    $session->set('user_role',   $personne->getRole());
                    $session->set('user_nom',    $personne->getNom());
                    $session->set('user_prenom', $personne->getPrenom());
                    $session->set('user_photo',  $personne->getProfile_photo());

                    if ($personne->getRole() === 'ADMIN') {
                        return $this->redirectToRoute('admin_users');
                    }
                    return $this->redirectToRoute('home');
                }
            } else {
                $error = 'Email ou mot de passe incorrect.';
            }
        }

        return $this->render('user/login.html.twig', ['error' => $error]);
    }

    // ─────────────────────────────────────────────────────────────
    //  DÉCONNEXION
    // ─────────────────────────────────────────────────────────────
    #[Route('/deconnexion', name: 'app_logout')]
    public function logout(Request $request): Response
    {
        $request->getSession()->clear();
        return $this->redirectToRoute('app_login');
    }

    // ─────────────────────────────────────────────────────────────
    //  INSCRIPTION
    // ─────────────────────────────────────────────────────────────
    #[Route('/inscription', name: 'app_register')]
    public function register(Request $request, EntityManagerInterface $em): Response
    {
        $session = $request->getSession();
        if ($session->get('user_id')) {
            return $this->redirectToRoute('home');
        }

        $errors = [];
        $old    = [];   // données du formulaire (pour repopuler en cas d'erreur)

        if ($request->isMethod('POST')) {
            $nom        = trim($request->request->get('nom', ''));
            $prenom     = trim($request->request->get('prenom', ''));
            $email      = trim($request->request->get('email', ''));
            $tel        = trim($request->request->get('telephone', ''));
            $password   = $request->request->get('motDePasse', '');
            $confirm    = $request->request->get('confirmPassword', '');
            $role       = in_array($request->request->get('role'), ['CLIENT', 'GUIDE']) ? $request->request->get('role') : 'CLIENT';
            $specialite = trim($request->request->get('specialite', ''));
            $langues    = trim($request->request->get('langues', ''));
            $experience = trim($request->request->get('experience', ''));

            $old = compact('nom', 'prenom', 'email', 'tel', 'role', 'specialite', 'langues', 'experience');

            if (empty($nom))           $errors[] = 'Le nom est requis.';
            if (empty($prenom))        $errors[] = 'Le prénom est requis.';
            if (empty($email))         $errors[] = "L'email est requis.";
            if (strlen($password) < 4) $errors[] = 'Le mot de passe doit contenir au moins 4 caractères.';
            if ($password !== $confirm) $errors[] = 'Les mots de passe ne correspondent pas.';
            if ($role === 'GUIDE') {
                if (empty($specialite)) $errors[] = 'La spécialité est requise pour un guide.';
                if (empty($langues))    $errors[] = 'Les langues parlées sont requises pour un guide.';
                if (empty($experience)) $errors[] = "L'expérience est requise pour un guide.";
            }

            if (empty($errors)) {
                $existing = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);
                if ($existing) {
                    $errors[] = 'Cette adresse e-mail est déjà utilisée.';
                } else {
                    $personne = new Personne();
                    $personne->setNom($nom);
                    $personne->setPrenom($prenom);
                    $personne->setEmail($email);
                    $personne->setMotDePasse($password);
                    $personne->setTelephone($tel !== '' ? $tel : null);
                    $personne->setRole($role);
                    $personne->setStatutCompte('ACTIF');
                    $personne->setDateInscription(new \DateTime());
                    $personne->setNotifSmsActive(true);
                    $personne->setHeureNotif(null);
                    $personne->setProfile_photo(null);

                    $em->persist($personne);
                    $em->flush();

                    if ($role === 'GUIDE') {
                        $guide = new Guide();
                        $guide->setPersonne($personne);
                        $guide->setSpecialite($specialite);
                        $guide->setLangues($langues);
                        $guide->setExperience($experience);
                        $em->persist($guide);
                        $em->flush();
                    }

                    $this->addFlash('login_info', 'Inscription réussie ! Vous pouvez maintenant vous connecter.');
                    return $this->redirectToRoute('app_login');
                }
            }
        }

        return $this->render('user/register.html.twig', [
            'errors' => $errors,
            'old'    => $old,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  PROFIL (voir & modifier)
    // ─────────────────────────────────────────────────────────────
    #[Route('/profil', name: 'app_profile')]
    public function profile(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireLogin($request);
        if ($redirect) return $redirect;

        $session  = $request->getSession();
        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->find($session->get('user_id'));

        if (!$personne) {
            $session->clear();
            return $this->redirectToRoute('app_login');
        }

        $errors = [];

        if ($request->isMethod('POST') && $request->request->get('_action') === 'update_profile') {
            $nom    = trim($request->request->get('nom', ''));
            $prenom = trim($request->request->get('prenom', ''));
            $email  = trim($request->request->get('email', ''));
            $tel    = trim($request->request->get('telephone', ''));

            if (empty($nom))    $errors[] = 'Le nom est requis.';
            if (empty($prenom)) $errors[] = 'Le prénom est requis.';
            if (empty($email))  $errors[] = "L'email est requis.";

            if (empty($errors)) {
                $existing = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);
                if ($existing && $existing->getId() !== $personne->getId()) {
                    $errors[] = 'Cet email est déjà utilisé par un autre compte.';
                } else {
                    $personne->setNom($nom);
                    $personne->setPrenom($prenom);
                    $personne->setEmail($email);
                    $personne->setTelephone($tel !== '' ? $tel : null);

                    // Upload photo de profil
                    $photoFile = $request->files->get('profilePhotoFile');
                    if ($photoFile) {
                        $uploadDir = $this->getParameter('kernel.project_dir') . '/public/uploads/profiles';
                        if (!is_dir($uploadDir)) {
                            mkdir($uploadDir, 0777, true);
                        }
                        $filename = 'profile_' . $personne->getId() . '_' . uniqid() . '.' . $photoFile->guessExtension();
                        $photoFile->move($uploadDir, $filename);
                        $personne->setProfile_photo('/uploads/profiles/' . $filename);
                    }

                    $em->flush();

                    // Mise à jour de la session
                    $session->set('user_nom',    $nom);
                    $session->set('user_prenom', $prenom);
                    $session->set('user_photo',  $personne->getProfile_photo());

                    $this->addFlash('profile_success', 'Profil mis à jour avec succès !');
                    return $this->redirectToRoute('app_profile');
                }
            }
        }

        return $this->render('user/profile.html.twig', [
            'personne' => $personne,
            'errors'   => $errors,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  CHANGEMENT DE MOT DE PASSE
    // ─────────────────────────────────────────────────────────────
    #[Route('/profil/mot-de-passe', name: 'app_change_password', methods: ['POST'])]
    public function changePassword(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireLogin($request);
        if ($redirect) return $redirect;

        $session  = $request->getSession();
        /** @var Personne $personne */
        $personne = $em->getRepository(Personne::class)->find($session->get('user_id'));

        $ancien  = $request->request->get('ancien_mdp', '');
        $nouveau = $request->request->get('nouveau_mdp', '');
        $confirm = $request->request->get('confirmer_mdp', '');

        if ($personne->getMotDePasse() !== $ancien) {
            $this->addFlash('danger', 'Ancien mot de passe incorrect.');
        } elseif ($nouveau !== $confirm) {
            $this->addFlash('danger', 'Les nouveaux mots de passe ne correspondent pas.');
        } elseif (strlen($nouveau) < 4) {
            $this->addFlash('danger', 'Le nouveau mot de passe doit contenir au moins 4 caractères.');
        } else {
            $personne->setMotDePasse($nouveau);
            $em->flush();
            $this->addFlash('profile_success', 'Mot de passe modifié avec succès !');
        }

        return $this->redirectToRoute('app_profile');
    }

    // ─────────────────────────────────────────────────────────────
    //  PRÉFÉRENCES DE VOYAGE
    // ─────────────────────────────────────────────────────────────
    #[Route('/profil/preferences', name: 'app_preferences')]
    public function preferences(Request $request, EntityManagerInterface $em): Response
    {
        $redirect = $this->requireLogin($request);
        if ($redirect) return $redirect;

        $session  = $request->getSession();
        /** @var Personne $personne */
        $personne = $em->getRepository(Personne::class)->find($session->get('user_id'));

        /** @var Preference|null $preference */
        $preference = $em->getRepository(Preference::class)->findOneBy(['personne_id' => $personne]);

        if ($request->isMethod('POST')) {
            $budgetMin      = $request->request->get('budgetMin', '');
            $budgetMax      = $request->request->get('budgetMax', '');
            $typesVoyage    = $request->request->all('typesVoyage');
            $centresInteret = $request->request->all('centresInteret');

            // ── Validation budget côté serveur ──
            $budgetErrors = [];

            if ($budgetMin !== '' && (float) $budgetMin < 0) {
                $budgetErrors[] = 'Le budget minimum ne peut pas être négatif.';
            }
            if ($budgetMax !== '' && (float) $budgetMax < 0) {
                $budgetErrors[] = 'Le budget maximum ne peut pas être négatif.';
            }
            if ($budgetMin !== '' && $budgetMax !== '' && (float) $budgetMax <= (float) $budgetMin) {
                $budgetErrors[] = 'Le budget maximum doit être strictement supérieur au budget minimum.';
            }

            if (!empty($budgetErrors)) {
                foreach ($budgetErrors as $err) {
                    $this->addFlash('danger', $err);
                }

                // Recalculer les sélections pour réafficher le formulaire avec les valeurs saisies
                $selectedTypes    = $typesVoyage;
                $selectedInterets = $centresInteret;

                return $this->render('user/preferences.html.twig', [
                    'personne'         => $personne,
                    'preference'       => $preference,
                    'selectedTypes'    => $selectedTypes,
                    'selectedInterets' => $selectedInterets,
                    'budgetMinInput'   => $budgetMin,
                    'budgetMaxInput'   => $budgetMax,
                ]);
            }

            if (!$preference) {
                $preference = new Preference();
                $preference->setPersonne_id($personne);
            }

            $preference->setBudgetMin($budgetMin !== '' ? (float) $budgetMin : null);
            $preference->setBudgetMax($budgetMax !== '' ? (float) $budgetMax : null);
            $preference->setTypesVoyage(implode(', ', $typesVoyage));
            $preference->setCentresInteret(implode(', ', $centresInteret));

            $em->persist($preference);
            $em->flush();

            $this->addFlash('profile_success', 'Préférences enregistrées avec succès !');
            return $this->redirectToRoute('app_preferences');
        }

        // Convertir les chaînes stockées en tableaux pour l'affichage des cases cochées
        $selectedTypes    = [];
        $selectedInterets = [];

        if ($preference) {
            if ($preference->getTypesVoyage()) {
                $selectedTypes = array_map('trim', explode(',', $preference->getTypesVoyage()));
            }
            if ($preference->getCentresInteret()) {
                $selectedInterets = array_map('trim', explode(',', $preference->getCentresInteret()));
            }
        }

        return $this->render('user/preferences.html.twig', [
            'personne'        => $personne,
            'preference'      => $preference,
            'selectedTypes'   => $selectedTypes,
            'selectedInterets'=> $selectedInterets,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTIER 1 — MOT DE PASSE OUBLIÉ (demande de reset)
    // ─────────────────────────────────────────────────────────────
    #[Route('/mot-de-passe-oublie', name: 'app_forgot_password')]
    public function forgotPassword(Request $request, EntityManagerInterface $em, MailerInterface $mailer): Response
    {
        if ($request->getSession()->get('user_id')) {
            return $this->redirectToRoute('home');
        }

        $success = false;
        $error   = null;

        if ($request->isMethod('POST')) {
            $email    = trim($request->request->get('email', ''));
            $personne = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);

            // Toujours afficher un message de succès (sécurité : on ne révèle pas si l'email existe)
            if ($personne) {
                $token   = bin2hex(random_bytes(32));
                $expiry  = new \DateTime('+1 hour');

                $personne->setResetToken($token);
                $personne->setResetTokenExpiry($expiry);
                $em->flush();

                $resetUrl = $this->generateUrl('app_reset_password', ['token' => $token], UrlGeneratorInterface::ABSOLUTE_URL);

                $emailMsg = (new Email())
                    ->from('rehla.noreply@gmail.com')
                    ->to($email)
                    ->subject('Réinitialisation de votre mot de passe – Rehla')
                    ->html(
                        '<div style="font-family:Poppins,sans-serif;max-width:500px;margin:auto;padding:30px;background:#f8f9fa;border-radius:12px;">'
                        . '<img src="https://i.ibb.co/placeholder/logo.png" alt="Rehla" style="height:50px;margin-bottom:20px;">'
                        . '<h2 style="color:#223f91;">Réinitialisation de mot de passe</h2>'
                        . '<p>Bonjour <strong>' . htmlspecialchars($personne->getPrenom() . ' ' . $personne->getNom()) . '</strong>,</p>'
                        . '<p>Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le bouton ci-dessous :</p>'
                        . '<a href="' . $resetUrl . '" style="display:inline-block;background:#223f91;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:600;margin:16px 0;">Réinitialiser mon mot de passe</a>'
                        . '<p style="color:#888;font-size:12px;">Ce lien expire dans 1 heure. Si vous n\'avez pas fait cette demande, ignorez cet email.</p>'
                        . '</div>'
                    );

                try {
                    $mailer->send($emailMsg);
                } catch (\Exception $e) {
                    // En cas d'échec d'envoi, on continue quand même (ne pas bloquer l'UX)
                }
            }

            $success = true;
        }

        return $this->render('user/forgot_password.html.twig', [
            'success' => $success,
            'error'   => $error,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  MÉTIER 1 — RÉINITIALISATION DU MOT DE PASSE (via token)
    // ─────────────────────────────────────────────────────────────
    #[Route('/reinitialiser-mot-de-passe/{token}', name: 'app_reset_password')]
    public function resetPassword(string $token, Request $request, EntityManagerInterface $em): Response
    {
        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->findOneBy(['resetToken' => $token]);

        if (!$personne || !$personne->getResetTokenExpiry() || $personne->getResetTokenExpiry() < new \DateTime()) {
            return $this->render('user/reset_password.html.twig', [
                'invalid' => true,
                'token'   => $token,
            ]);
        }

        $errors = [];

        if ($request->isMethod('POST')) {
            $nouveau  = $request->request->get('nouveau_mdp', '');
            $confirm  = $request->request->get('confirmer_mdp', '');

            if (strlen($nouveau) < 4) {
                $errors[] = 'Le mot de passe doit contenir au moins 4 caractères.';
            } elseif ($nouveau !== $confirm) {
                $errors[] = 'Les mots de passe ne correspondent pas.';
            } else {
                $personne->setMotDePasse($nouveau);
                $personne->setResetToken(null);
                $personne->setResetTokenExpiry(null);
                $em->flush();

                $this->addFlash('login_info', 'Mot de passe réinitialisé avec succès ! Vous pouvez vous connecter.');
                return $this->redirectToRoute('app_login');
            }
        }

        return $this->render('user/reset_password.html.twig', [
            'invalid' => false,
            'token'   => $token,
            'errors'  => $errors,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  API 1 — POST /api/login  (retourne JSON)
    // ─────────────────────────────────────────────────────────────
    #[Route('/api/login', name: 'api_login', methods: ['POST'])]
    public function apiLogin(Request $request, EntityManagerInterface $em): JsonResponse
    {
        $data     = json_decode($request->getContent(), true) ?? [];
        $email    = trim($data['email'] ?? $request->request->get('email', ''));
        $password = $data['motDePasse'] ?? $request->request->get('motDePasse', '');

        if (!$email || !$password) {
            return $this->json(['success' => false, 'message' => 'Email et mot de passe requis.'], 400);
        }

        /** @var Personne|null $personne */
        $personne = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);

        if (!$personne || $personne->getMotDePasse() !== $password) {
            return $this->json(['success' => false, 'message' => 'Identifiants incorrects.'], 401);
        }

        if ($personne->getStatutCompte() === 'SUSPENDU') {
            return $this->json(['success' => false, 'message' => 'Compte suspendu.'], 403);
        }

        return $this->json([
            'success' => true,
            'user'    => [
                'id'           => $personne->getId(),
                'nom'          => $personne->getNom(),
                'prenom'       => $personne->getPrenom(),
                'email'        => $personne->getEmail(),
                'role'         => $personne->getRole(),
                'statutCompte' => $personne->getStatutCompte(),
                'photo'        => $personne->getProfile_photo(),
            ],
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  API 2 — GET /api/users  (liste des utilisateurs, admin uniquement)
    // ─────────────────────────────────────────────────────────────
    #[Route('/api/users', name: 'api_users', methods: ['GET'])]
    public function apiUsers(Request $request, EntityManagerInterface $em): JsonResponse
    {
        // Authentification par clé API dans le header X-API-KEY
        $apiKey = $request->headers->get('X-API-KEY');
        if ($apiKey !== 'rehla-admin-2026') {
            return $this->json(['success' => false, 'message' => 'Clé API invalide ou manquante.'], 401);
        }

        $role   = $request->query->get('role');
        $statut = $request->query->get('statut');

        $criteria = [];
        if ($role)   $criteria['role']         = strtoupper($role);
        if ($statut) $criteria['statutCompte']  = strtoupper($statut);

        $users = $em->getRepository(Personne::class)->findBy($criteria);

        $data = array_map(fn(Personne $p) => [
            'id'              => $p->getId(),
            'nom'             => $p->getNom(),
            'prenom'          => $p->getPrenom(),
            'email'           => $p->getEmail(),
            'role'            => $p->getRole(),
            'statutCompte'    => $p->getStatutCompte(),
            'dateInscription' => $p->getDateInscription()?->format('Y-m-d'),
            'telephone'       => $p->getTelephone(),
        ], $users);

        return $this->json([
            'success' => true,
            'total'   => count($data),
            'users'   => $data,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  API 3 — GET /api/check-email  (vérification email disponible)
    // ─────────────────────────────────────────────────────────────
    #[Route('/api/check-email', name: 'api_check_email', methods: ['GET'])]
    public function apiCheckEmail(Request $request, EntityManagerInterface $em): JsonResponse
    {
        $email     = trim($request->query->get('email', ''));
        $excludeId = (int) $request->query->get('exclude', 0);

        if (!$email || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return $this->json(['available' => false, 'message' => 'Email invalide.'], 400);
        }

        $existing = $em->getRepository(Personne::class)->findOneBy(['email' => $email]);

        if (!$existing || ($excludeId > 0 && $existing->getId() === $excludeId)) {
            return $this->json(['available' => true, 'message' => 'Email disponible.']);
        }

        return $this->json(['available' => false, 'message' => 'Email déjà utilisé.']);
    }
}
