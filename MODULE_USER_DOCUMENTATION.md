# Module Gestion des Utilisateurs – Documentation Technique
**Projet :** Rehla – Agence de Voyage  
**Module :** Gestion des Utilisateurs & Authentification  
**Framework :** Symfony 6.4 | PHP 8.2 | MySQL

---

## Sommaire

1. [Métiers Avancés](#métiers-avancés)
2. [Bundles Externes](#bundles-externes)
3. [Fonctionnalités Avancées](#fonctionnalités-avancées)
4. [Intelligence Artificielle – Analyse des Risques](#intelligence-artificielle)
5. [API Externe – Google reCAPTCHA v2](#api-externe)
6. [APIs REST Exposées](#apis-rest-exposées)

---

## Métiers Avancés

### Métier A – Mot de Passe Oublié

**Objectif :** Permettre à un utilisateur de réinitialiser son mot de passe sans intervention admin.

**Fonctionnement :**
1. L'utilisateur clique sur « Mot de passe oublié ? » sur la page de connexion
2. Il entre son adresse email
3. Le système génère un **token unique** (64 caractères hexadécimaux) avec une **expiration de 1 heure**
4. Un email contenant un lien de réinitialisation est envoyé via UserMailerBundle
5. L'utilisateur clique sur le lien → page de saisie du nouveau mot de passe
6. Le token est vérifié (existence + non-expiré), le mot de passe est mis à jour, le token est supprimé

**Fichiers modifiés :**
- `src/Entity/Personne.php` – ajout des champs `reset_token` (varchar 100) et `reset_token_expiry` (datetime)
- `src/Controller/UserController.php` – routes `app_forgot_password` et `app_reset_password/{token}`
- `templates/user/forgot_password.html.twig` – formulaire de demande
- `templates/user/reset_password.html.twig` – formulaire de nouveau mot de passe avec validation JS (correspondance des mots de passe)
- `templates/user/login.html.twig` – lien « Mot de passe oublié ? »

**Base de données :**
```sql
ALTER TABLE personne 
  ADD COLUMN reset_token VARCHAR(100) NULL,
  ADD COLUMN reset_token_expiry DATETIME NULL;
```

---

### Métier B – Suspension Temporaire avec Date de Fin

**Objectif :** L'admin peut suspendre un compte avec une date de fin automatique. Le compte se réactive seul à la prochaine connexion.

**Fonctionnement :**
1. Dans la page de modification d'un utilisateur, l'admin choisit le statut **SUSPENDU**
2. Un champ `datetime-local` apparaît automatiquement (JS) pour saisir la date de fin
3. La date est enregistrée dans `suspension_fin`
4. Un email de suspension est envoyé automatiquement via UserMailerBundle (avec la date de fin)
5. À la prochaine tentative de connexion de l'utilisateur : si `suspension_fin <= now()` → statut passe à ACTIF automatiquement, `suspension_fin` remis à NULL
6. Le message de suspension affiché à l'utilisateur inclut la date de fin prévue

**Fichiers modifiés :**
- `src/Entity/Personne.php` – ajout du champ `suspension_fin` (datetime nullable)
- `src/Controller/AdminUserController.php` – logique de suspension dans la méthode `edit()`
- `src/Controller/UserController.php` – vérification de l'expiration à la connexion
- `templates/admin/user_edit.html.twig` – champ `suspension_fin` avec toggle JS

**Base de données :**
```sql
ALTER TABLE personne ADD COLUMN suspension_fin DATETIME NULL;
```

---

### Métier C – Détection d'Inactivité Automatique

**Objectif :** Détecter les comptes inactifs depuis plus de 30 jours, les passer à INACTIF, et notifier les utilisateurs par email.

**Fonctionnement :**
1. L'admin clique sur « Vérifier inactivité » dans le sidebar
2. Le système parcourt tous les comptes ACTIF (hors ADMIN)
3. Pour chaque compte : compare `derniere_connexion` (ou `dateInscription` si jamais connecté) avec aujourd'hui
4. Si inactif depuis **≥ 30 jours** → statut = INACTIF + email envoyé via UserMailerBundle
5. Flash message indiquant le nombre de comptes marqués inactifs
6. À la prochaine connexion d'un compte INACTIF → réactivation automatique + flash « Bon retour »

**Fichiers modifiés :**
- `src/Entity/Personne.php` – ajout du champ `derniere_connexion` (datetime nullable)
- `src/Controller/UserController.php` – mise à jour de `derniere_connexion` à chaque connexion + route `admin_check_inactivity`
- `templates/admin/Partials/sidebar.html.twig` – lien « Vérifier inactivité »

**Base de données :**
```sql
ALTER TABLE personne ADD COLUMN derniere_connexion DATETIME NULL;
```

---

## Bundles Externes

### Bundle 1 – UserMailerBundle (Bundle Custom)

**Objectif :** Bundle Symfony maison gérant tous les emails automatiques du module utilisateurs.

**Architecture (inspirée du SmsBundle) :**
```
src/UserMailerBundle/
├── UserMailerBundle.php                        ← classe Bundle
├── DependencyInjection/
│   └── UserMailerExtension.php                ← charge le services.yaml interne
└── Resources/
    └── config/
        └── services.yaml                      ← enregistre UserMailerService
    Service/
        └── UserMailerService.php              ← les 3 méthodes d'envoi
```

**3 méthodes du service :**

| Méthode | Déclencheur | Contenu |
|---------|-------------|---------|
| `sendWelcomeEmail(Personne)` | Inscription réussie | Email de bienvenue avec le prénom |
| `sendAccountSuspendedEmail(Personne, ?DateTime)` | Admin suspend un compte | Raison + date de fin si temporaire |
| `sendInactivityEmail(Personne, int $days)` | Détection inactivité | Nombre de jours d'inactivité + invitation à se reconnecter |

**Enregistrement :**
- `config/bundles.php` – `App\UserMailerBundle\UserMailerBundle::class => ['all' => true]`
- `config/services.yaml` – injection explicite + `public: true`

**Infrastructure email :** Symfony Mailer + Google Gmail Bridge (`symfony/google-mailer`) via le compte `rehla.noreply@gmail.com`

---

### Bundle 2 – dompdf/dompdf

**Objectif :** Générer et télécharger un PDF complet de la liste des utilisateurs depuis l'interface admin.

**Fonctionnement :**
1. L'admin clique sur « Export PDF » dans la page des utilisateurs
2. Le contrôleur récupère tous les utilisateurs triés par date d'inscription
3. Calcule les statistiques (total, actifs, suspendus, guides, clients)
4. Construit un HTML complet avec styles inline (compatible DomPDF)
5. DomPDF génère le PDF en format A4 paysage
6. Retourne une réponse HTTP avec `Content-Type: application/pdf`

**Fichier :** `src/Controller/AdminUserController.php` – méthode `exportPdf()`

**Contenu du PDF :** En-tête Rehla, date d'export, 5 statistiques visuelles, tableau complet avec couleurs par rôle/statut

---

### Bundle 3 – symfony/mailer + symfony/google-mailer

**Objectif :** Infrastructure d'envoi d'emails via Gmail SMTP.

**Configuration :**
```
# .env
MAILER_DSN=gmail+smtp://rehla.noreply@gmail.com:APP_PASSWORD@default
```

Utilisé par UserMailerBundle pour les 3 types d'emails. Le mot de passe est un **App Password Google** (pas le mot de passe du compte).

---

## Fonctionnalités Avancées

### Fonctionnalité 1 – Statistiques Utilisateurs (Chart.js)

**Objectif :** Tableau de bord visuel avec graphiques pour l'admin.

**3 graphiques :**
- **Camembert** – Distribution par rôle (CLIENT / GUIDE / ADMIN)
- **Camembert** – Distribution par statut (ACTIF / INACTIF / SUSPENDU)
- **Histogramme** – Inscriptions par mois sur les 12 derniers mois

**Fichiers :**
- `src/Controller/AdminUserController.php` – méthode `stats()`, calcule toutes les données PHP
- `templates/admin/stats.html.twig` – rendu Chart.js avec données JSON injectées via `|json_encode`

---

### Fonctionnalité 2 – reCAPTCHA v2 (voir section API Externe)

---

### Fonctionnalité 3 – Vérification Email en Temps Réel

Sur le formulaire d'inscription, à chaque blur sur le champ email : appel AJAX vers `/api/check-email` → affiche une erreur si l'email est déjà utilisé, sans attendre la soumission du formulaire.

---

## Intelligence Artificielle

### Analyse IA des Risques Utilisateurs

**Objectif :** Analyser automatiquement tous les comptes utilisateurs et attribuer un niveau de risque avec une explication en langage naturel.

**Pourquoi c'est de l'IA et pas un simple calcul :**
- Un calcul classique appliquerait des règles fixes (`if postsSuspects > 3 → ÉLEVÉ`)
- L'IA **pondère plusieurs facteurs ensemble** de façon contextuelle
- Elle génère une **explication en langage naturel unique** pour chaque utilisateur
- Elle peut détecter des **combinaisons de facteurs** qu'une règle fixe ne couvrirait pas

**Données analysées par utilisateur :**
```json
{
  "id": 1,
  "nom": "mezghani lina",
  "role": "CLIENT",
  "statut": "SUSPENDU",
  "postsSuspects": 2,
  "anciennete_jours": 74,
  "inactivite_jours": 60
}
```

**Niveaux de risque retournés :**
| Niveau | Couleur | Critères typiques |
|--------|---------|-------------------|
| ÉLEVÉ | Rouge | Statut SUSPENDU, posts suspects > 3, combinaisons |
| MOYEN | Orange | Inactivité > 60 jours, facteurs modérés |
| FAIBLE | Vert | Aucun comportement suspect |

**Architecture technique (`src/Service/UserRiskAnalysisService.php`) :**

1. Collecte les données de tous les utilisateurs non-ADMIN
2. Construit un prompt en français avec les données JSON
3. **Essaie OpenRouter en premier** (LLaMA 3.1 8B gratuit via `openrouter.ai`) via cURL
4. **Fallback Gemini** si OpenRouter échoue : essaie 2 clés API × 3 modèles = 6 combinaisons
5. Parse la réponse JSON retournée par l'IA
6. Normalise le niveau (gère `ELEVE` → `ÉLEVÉ` pour l'affichage)

**Stratégie de fallback :**
```
OpenRouter (LLaMA 3.1) → Gemini gemini-2.0-flash (clé 1) 
→ Gemini gemini-1.5-flash (clé 1) → Gemini gemini-2.5-flash (clé 1)
→ Gemini gemini-2.0-flash (clé 2) → ...
```

**Pourquoi cURL et pas Symfony HttpClient :**  
XAMPP Windows présente des problèmes SSL avec HttpClient. cURL avec `CURLOPT_SSL_VERIFYPEER => false` contourne ce problème en développement local.

**Fichiers :**
- `src/Service/UserRiskAnalysisService.php` – logique complète
- `src/Controller/AdminUserController.php` – route `/admin/analyse-risques`
- `templates/admin/risk_analysis.html.twig` – tableau avec badges colorés + compteurs résumés
- `config/services.yaml` – injection des 3 clés API
- `.env` – `GEMINI_API_KEY`, `GEMINI_API_KEY2`, `OPENROUTER_API_KEY`

---

## API Externe

### Google reCAPTCHA v2 – Protection Anti-Bot

**Objectif :** Protéger les formulaires de connexion et d'inscription contre les robots et attaques automatisées.

**Type utilisé :** reCAPTCHA v2 « Je ne suis pas un robot » (checkbox visible avec défi image possible)

**Différence avec reCAPTCHA v3 :**
- v3 est invisible (score en arrière-plan, pas d'interaction utilisateur)
- **v2 affiche un checkbox visible** et parfois un défi de sélection d'images (feux de signalisation, bus, etc.) quand Google considère la session suspecte

**Fonctionnement :**

**Frontend :**
```html
<!-- Chargement du script Google -->
<script src="https://www.google.com/recaptcha/api.js"></script>

<!-- Widget dans le formulaire -->
<div class="g-recaptcha" data-sitekey="{{ recaptcha_v2_site_key }}"></div>
```

À la soumission : JS vérifie que `grecaptcha.getResponse()` n'est pas vide (sinon bloque).

**Backend :**
```
POST https://www.google.com/recaptcha/api/siteverify
  secret=SECRET_KEY
  response=TOKEN_DU_FORMULAIRE
→ {"success": true/false}
```
Implémenté via cURL dans `UserController::verifyRecaptchaV2()`. Si `success = false` → erreur affichée, formulaire rejeté.

**Pages protégées :**
- `/connexion` – login
- `/inscription` – register

**Fichiers :**
- `.env` – `RECAPTCHA_V2_SITE_KEY` + `RECAPTCHA_V2_SECRET_KEY`
- `config/services.yaml` – paramètres `recaptcha_v2_site_key` et `recaptcha_v2_secret_key`
- `src/Controller/UserController.php` – méthode `verifyRecaptchaV2()` + vérification dans `login()` et `register()`
- `templates/user/login.html.twig` – widget + validation JS
- `templates/user/register.html.twig` – widget + validation JS

---

## APIs REST Exposées

### API 1 – POST `/api/login`
Authentification programmatique. Retourne les infos utilisateur en JSON ou une erreur.

```json
// Request
{"email": "user@example.com", "motDePasse": "pass"}

// Response 200
{"success": true, "user": {"id": 1, "nom": "...", "role": "CLIENT"}}

// Response 401
{"success": false, "message": "Email ou mot de passe incorrect."}
```

---

### API 2 – GET `/api/users`
Liste tous les utilisateurs non-ADMIN (lecture seule, pour intégrations externes).

```json
[
  {"id": 1, "nom": "mezghani", "prenom": "lina", "email": "...", "role": "CLIENT", "statut": "ACTIF"},
  ...
]
```

---

### API 3 – GET `/api/check-email?email=xxx`
Vérifie en temps réel si une adresse email est disponible à l'inscription.

```json
// Email disponible
{"available": true}

// Email déjà utilisé
{"available": false, "message": "Cette adresse e-mail est déjà utilisée."}
```
Utilisé en AJAX sur le formulaire d'inscription (blur sur le champ email).

---

### API 4 – POST `/api/check-password-pwned`
Vérifie si un mot de passe a été exposé dans des fuites de données via **HaveIBeenPwned**.

**Principe k-Anonymité :** Seuls les 5 premiers caractères du hash SHA1 sont envoyés à l'API externe. Le mot de passe n'est jamais transmis en clair.

```
SHA1("password123") = CBFDAC6008F9CAB4083784CBD1874F76618D2A97
→ Envoie seulement : CBFDA
→ Reçoit une liste de suffixes et leur nombre d'occurrences
→ Compare localement le reste : C6008F9CAB4083784CBD1874F76618D2A97
```

```json
// Request
{"password": "password123"}

// Response – mot de passe compromis
{"pwned": true, "count": 9545824}

// Response – mot de passe sûr
{"pwned": false, "count": 0}
```

Affiché sur les formulaires d'inscription et de changement de mot de passe (profil) via AJAX au blur.

---

## Résumé des Fichiers Créés / Modifiés

| Fichier | Type | Description |
|---------|------|-------------|
| `src/Entity/Personne.php` | Modifié | +4 champs : reset_token, reset_token_expiry, suspension_fin, derniere_connexion |
| `src/Controller/UserController.php` | Modifié | Login avancé, register+mailer, forgot password, reset password, inactivity check, 4 APIs REST, reCAPTCHA |
| `src/Controller/AdminUserController.php` | Modifié | Suspension temporaire, analyse IA, export PDF, statistiques |
| `src/Service/UserRiskAnalysisService.php` | Créé | Service IA avec fallback OpenRouter → Gemini |
| `src/UserMailerBundle/` | Créé | Bundle complet avec 3 emails automatiques |
| `templates/admin/risk_analysis.html.twig` | Créé | Page analyse IA avec tableau et compteurs |
| `templates/admin/stats.html.twig` | Créé | Dashboard Chart.js |
| `templates/user/forgot_password.html.twig` | Créé | Formulaire mot de passe oublié |
| `templates/user/reset_password.html.twig` | Créé | Formulaire réinitialisation |
| `templates/user/login.html.twig` | Modifié | +reCAPTCHA v2, +lien mot de passe oublié |
| `templates/user/register.html.twig` | Modifié | +reCAPTCHA v2, +check email AJAX, +check pwned |
| `templates/user/profile.html.twig` | Modifié | +check pwned sur changement mot de passe |
| `templates/admin/user_edit.html.twig` | Modifié | +champ suspension_fin avec toggle JS |
| `templates/admin/users_admin.html.twig` | Modifié | +boutons Export PDF, Statistiques, Analyse IA |
| `templates/admin/Partials/sidebar.html.twig` | Modifié | +liens Statistiques, Analyse IA, Vérifier inactivité |
| `config/bundles.php` | Modifié | +UserMailerBundle |
| `config/services.yaml` | Modifié | +paramètres reCAPTCHA, +services explicites |
| `.env` | Modifié | +GEMINI_API_KEY, +GEMINI_API_KEY2, +OPENROUTER_API_KEY, +RECAPTCHA_V2_SITE_KEY, +RECAPTCHA_V2_SECRET_KEY |
| `public/.htaccess` | Créé | mod_rewrite pour routing Symfony |