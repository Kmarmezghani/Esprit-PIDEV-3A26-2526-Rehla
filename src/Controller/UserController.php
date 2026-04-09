<?php

namespace App\Controller;

use App\Entity\Personne;
use App\Entity\Preference;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

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
            $nom      = trim($request->request->get('nom', ''));
            $prenom   = trim($request->request->get('prenom', ''));
            $email    = trim($request->request->get('email', ''));
            $tel      = trim($request->request->get('telephone', ''));
            $password = $request->request->get('motDePasse', '');
            $confirm  = $request->request->get('confirmPassword', '');

            $old = compact('nom', 'prenom', 'email', 'tel');

            if (empty($nom))                        $errors[] = 'Le nom est requis.';
            if (empty($prenom))                     $errors[] = 'Le prénom est requis.';
            if (empty($email))                      $errors[] = "L'email est requis.";
            if (strlen($password) < 4)              $errors[] = 'Le mot de passe doit contenir au moins 4 caractères.';
            if ($password !== $confirm)             $errors[] = 'Les mots de passe ne correspondent pas.';

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
                    $personne->setRole('CLIENT');
                    $personne->setStatutCompte('ACTIF');
                    $personne->setDateInscription(new \DateTime());
                    $personne->setNotifSmsActive(true);
                    $personne->setHeureNotif(null);
                    $personne->setProfile_photo(null);

                    $em->persist($personne);
                    $em->flush();

                    $this->addFlash('success', 'Inscription réussie ! Vous pouvez maintenant vous connecter.');
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

                    $this->addFlash('success', 'Profil mis à jour avec succès !');
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
            $this->addFlash('success', 'Mot de passe modifié avec succès !');
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

            $this->addFlash('success', 'Préférences enregistrées avec succès !');
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
}
