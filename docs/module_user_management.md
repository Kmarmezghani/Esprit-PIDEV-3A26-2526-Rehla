# Documentation — Module Gestion des Utilisateurs
## Projet Rehla — Symfony 6.4 — Université

---

## Table des matières
1. Vue d'ensemble du module
2. Architecture & fichiers créés
3. Authentification et gestion de session
4. Routes et fonctionnalités
5. Entités modifiées
6. Formulaires
7. Templates (vues)
8. Administration backend
9. Sécurité
10. Flux complet (diagrammes)
11. Questions fréquentes jury

---

## 1. Vue d'ensemble du module

Le module **Gestion des Utilisateurs** couvre :
- **Inscription** d'un nouveau client
- **Connexion** (login) et **Déconnexion** (logout avec confirmation)
- **Profil** : modification des informations personnelles + photo de profil
- **Préférences de voyage** : budget, types de voyage, centres d'intérêt
- **Administration** : liste des comptes, modification, suppression, consultation des préférences

### Technologie utilisée
- Framework : **Symfony 6.4**
- Base de données : **MySQL** via **Doctrine ORM**
- Auth : **Sessions PHP** (pas Symfony Security, pour compatibilité avec l'architecture d'équipe)
- Frontend : **Bootstrap 4.5** + **Poppins** (Google Fonts) + **SweetAlert2**
- Admin : **Sneat Admin Template** (Bootstrap 5)

---

## 2. Architecture & fichiers créés

### Nouveaux fichiers (créés par ce module)

```
src/
├── Controller/
│   ├── UserController.php          ← Routes frontales (login, register, profile, prefs)
│   └── AdminUserController.php     ← Routes admin (CRUD utilisateurs)
├── Form/
│   ├── LoginType.php               ← Formulaire de connexion
│   ├── RegisterType.php            ← Formulaire d'inscription
│   ├── ProfileType.php             ← Formulaire de modification de profil
│   └── PreferenceType.php          ← Formulaire de préférences

templates/
├── user/
│   ├── login.html.twig             ← Page de connexion (standalone, sans nav)
│   ├── register.html.twig          ← Page d'inscription (standalone)
│   ├── profile.html.twig           ← Profil utilisateur (étend base.html.twig)
│   └── preferences.html.twig      ← Préférences voyage (étend base.html.twig)
└── admin/
    ├── users_admin.html.twig       ← Liste des utilisateurs (admin)
    ├── user_edit.html.twig         ← Formulaire de modification admin
    └── user_preferences_show.html.twig ← Vue préférences en lecture seule

docs/
└── module_user_management.md       ← Ce fichier de documentation
```

### Fichiers existants modifiés

| Fichier | Modification |
|---------|-------------|
| `src/Entity/Personne.php` | Ajout `#[ORM\GeneratedValue]`, champs nullable (telephone, heureNotif, profile_photo) |
| `src/Entity/Preference.php` | Ajout `#[ORM\GeneratedValue]`, tous les champs en nullable |
| `templates/admin/Partials/sidebar.html.twig` | Ajout section "Utilisateurs" dans le menu |
| `templates/home/index.html.twig` | Navbar session-aware, suppression lien admin public, boutons login/register |

---

## 3. Authentification et gestion de session

### Choix technique : Sessions PHP (pas Symfony Security)

Le module utilise les **sessions PHP natives** via `$request->getSession()`. Ce choix a été fait car :
1. L'entité `Personne` n'implémente pas `UserInterface` (requis pour Symfony Security)
2. Les mots de passe en base sont en **texte clair** (format de l'équipe)
3. Compatibilité avec les autres modules de l'équipe

### Variables de session enregistrées lors de la connexion

| Clé session | Contenu | Exemple |
|-------------|---------|---------|
| `user_id` | ID de la personne | `1` |
| `user_role` | Rôle (`CLIENT`, `GUIDE`, `ADMIN`) | `CLIENT` |
| `user_nom` | Nom de famille | `mezghani` |
| `user_prenom` | Prénom | `lina` |
| `user_photo` | Chemin vers la photo de profil | `/uploads/profiles/profile_1_xxx.jpg` |

### Accès aux données de session dans Twig

```twig
{{ app.session.get('user_id') }}
{{ app.session.get('user_prenom') }}
{{ app.session.get('user_role') }}
```

---

## 4. Routes et fonctionnalités

### Routes frontales (UserController)

| Route | Méthode | Nom | Description |
|-------|---------|-----|-------------|
| `/connexion` | GET/POST | `app_login` | Page de connexion |
| `/deconnexion` | GET | `app_logout` | Déconnexion + clear session |
| `/inscription` | GET/POST | `app_register` | Inscription nouveau compte |
| `/profil` | GET/POST | `app_profile` | Voir & modifier son profil |
| `/profil/mot-de-passe` | POST | `app_change_password` | Changer le mot de passe |
| `/profil/preferences` | GET/POST | `app_preferences` | Voir & modifier ses préférences |

### Routes admin (AdminUserController)

| Route | Méthode | Nom | Accès requis |
|-------|---------|-----|-------------|
| `/admin/utilisateurs` | GET | `admin_users` | ADMIN |
| `/admin/utilisateurs/{id}/modifier` | GET/POST | `admin_user_edit` | ADMIN |
| `/admin/utilisateurs/{id}/supprimer` | POST | `admin_user_delete` | ADMIN |
| `/admin/utilisateurs/{id}/preferences` | GET | `admin_user_preferences` | ADMIN |

---

## 5. Entités modifiées

### Personne.php — Modifications

**Avant :**
```php
#[ORM\Id]
#[ORM\Column(type: "integer")]
private int $id;
```

**Après :**
```php
#[ORM\Id]
#[ORM\GeneratedValue]   // ← Permet l'auto-incrément MySQL
#[ORM\Column(type: "integer")]
private ?int $id = null;  // ← nullable pour les nouveaux objets
```

**Raison :** Sans `#[ORM\GeneratedValue]`, Doctrine utilise la stratégie `NONE` et attend que vous fournissiez l'ID manuellement. Avec AUTO_INCREMENT en base, il faut cette annotation pour que Doctrine récupère l'ID généré après INSERT.

**Champs rendus nullable :**
- `telephone` → `?string` (certains utilisateurs n'ont pas de téléphone)
- `heureNotif` → `?\DateTimeInterface` (optionnel)
- `profile_photo` → `?string` (pas obligatoire à l'inscription)

### Preference.php — Modifications

Mêmes corrections : `#[ORM\GeneratedValue]` sur l'id, tous les champs en nullable car la table DB les accepte toutes en NULL.

---

## 6. Formulaires

### LoginType.php
Champs : `email` (EmailType), `motDePasse` (PasswordType)
**Note :** En pratique, le login est géré directement dans le controller via `$request->request->get()` pour plus de simplicité.

### RegisterType.php
Champs : `nom`, `prenom`, `email`, `telephone` (optionnel), `motDePasse` (RepeatedType avec confirmation)
Validations : NotBlank sur les champs requis, Email format, longueur minimale 4 caractères

### ProfileType.php
Champs : `nom`, `prenom`, `email`, `telephone`, `profilePhotoFile` (FileType, non mappé)
Le `FileType` avec `mapped: false` permet l'upload de fichier sans qu'il soit lié à l'entité directement.

### PreferenceType.php
Champs :
- `budgetMin`, `budgetMax` (NumberType)
- `typesVoyage` (ChoiceType, multiple, expanded = checkboxes, **mapped: false**)
- `centresInteret` (ChoiceType, multiple, expanded = checkboxes, **mapped: false**)

**Pourquoi `mapped: false` sur les choix ?**
La base de données stocke les types comme une chaîne de texte : `"Culture, Aventure, Randonnée"`. Les `ChoiceType` avec `multiple: true` renvoient un tableau PHP. La conversion tableau ↔ chaîne se fait dans le controller :

```php
// Tableau → chaîne (pour sauvegarder)
$preference->setTypesVoyage(implode(', ', $typesVoyage));

// Chaîne → tableau (pour pré-cocher les cases)
$selectedTypes = array_map('trim', explode(',', $preference->getTypesVoyage()));
```

---

## 7. Templates (vues)

### login.html.twig & register.html.twig
- Pages **standalone** (n'étendent pas base.html.twig)
- Design : carte blanche centrée sur fond dégradé bleu `#0f1c3f → #223f91`
- Toggle affichage mot de passe (JavaScript natif)
- Messages d'erreur stylisés en rouge

### profile.html.twig & preferences.html.twig
- Étendent `base.html.twig`
- Navbar identique à la page d'accueil avec infos utilisateur connecté
- Layout deux colonnes : sidebar profil gauche + formulaire droite
- Upload photo avec aperçu immédiat (FileReader API)
- Bouton déconnexion avec confirmation SweetAlert2

### Déconnexion avec confirmation (SweetAlert2)
```javascript
function confirmLogout(e) {
    e.preventDefault();
    Swal.fire({
        title: 'Déconnexion',
        text: 'Êtes-vous sûr de vouloir vous déconnecter ?',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#223f91',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Oui, me déconnecter',
        cancelButtonText: 'Annuler'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('logoutForm').submit();
        }
    });
}
```

---

## 8. Administration backend

### Accès admin
- **Identifiant :** email `admin@rehla.tn`
- **Mot de passe :** doit correspondre au champ `motDePasse` en DB
- **Protection :** chaque action admin vérifie `$session->get('user_role') === 'ADMIN'`
- **Si non admin → redirection** vers la page de connexion

### Helper de protection (dans AdminUserController)
```php
private function requireAdmin(Request $request): ?Response
{
    $session = $request->getSession();
    if (!$session->get('user_id') || $session->get('user_role') !== 'ADMIN') {
        return $this->redirectToRoute('app_login');
    }
    return null;
}
```

### Fonctionnalités admin — `users_admin.html.twig`
- **Statistiques rapides** : total comptes, actifs, suspendus, guides
- **Tableau** avec : photo, nom, email, téléphone, rôle (badge coloré), statut, date inscription
- **Filtres** : par rôle (dropdown) + recherche texte (JavaScript côté client)
- **Actions** : Modifier, Voir préférences, Supprimer (avec confirmation modale)
- **Protection** : les comptes ADMIN ne peuvent pas être supprimés

### Consultation des préférences (lecture seule)
La route `admin_user_preferences` affiche les préférences en **lecture seule** uniquement (l'admin ne modifie pas les préférences de l'utilisateur). Les données texte sont splittées par virgule et affichées en badges.

---

## 9. Sécurité

### Protections implémentées
1. **Anti-auto-suppression** : un admin ne peut pas supprimer son propre compte
2. **Vérification session** : toutes les routes protégées vérifient la session avant d'agir
3. **Compte suspendu** : si `statutCompte = SUSPENDU`, le login est refusé avec message
4. **Email unique** : validation de l'unicité à l'inscription et à la modification du profil
5. **CSRF token** sur les formulaires de suppression (via `{{ csrf_token(...) }}`)
6. **Chemin admin caché** : le lien `/admin` n'est plus dans la navbar publique ; seul l'utilisateur admin connecté le voit dans son dropdown

### Ce qui n'est PAS implémenté (hors scope)
- Hachage des mots de passe (l'équipe utilise du texte clair)
- Rate limiting (protection brute force)
- Réinitialisation mot de passe par email

---

## 10. Flux complet

### Flux de connexion
```
Visiteur → GET /connexion → affiche formulaire
  POST /connexion (email + motDePasse)
    → Recherche Personne par email
    → Compare motDePasse
    → Si match :
        ├── SUSPENDU → erreur "compte suspendu"
        ├── ADMIN → session + redirect /admin/utilisateurs
        └── CLIENT/GUIDE → session + redirect /home
    → Si pas match : erreur "email ou mot de passe incorrect"
```

### Flux d'inscription
```
Visiteur → GET /inscription → formulaire
  POST /inscription
    → Validation (champs requis, longueur mdp, confirmation)
    → Vérification email unique en DB
    → Création Personne (role=CLIENT, statut=ACTIF)
    → Redirect /connexion avec message flash "succès"
```

### Flux de déconnexion
```
Click "Déconnexion" → SweetAlert2 (confirmation)
  → Si confirmé : GET /deconnexion
      → $session->clear()
      → Redirect /connexion
  → Si annulé : reste sur la page
```

### Flux modification profil
```
Connecté → GET /profil → affiche formulaire pré-rempli
  POST /profil (_action=update_profile)
    → Validation
    → Si photo uploadée : déplace vers /public/uploads/profiles/
    → Mise à jour Personne en DB
    → Mise à jour session (nom, prenom, photo)
    → Flash "succès" + redirect /profil
```

---

## 11. Questions fréquentes jury

**Q : Pourquoi vous n'utilisez pas le composant Security de Symfony ?**
R : L'entité `Personne` n'implémente pas `UserInterface` (requis par le composant Security). Notre équipe a décidé de garder l'entité simple pour la compatibilité entre tous les modules. On gère l'authentification manuellement via les sessions PHP, ce qui est tout à fait valide pour une application académique.

**Q : Comment fonctionne la protection des routes admin ?**
R : Chaque action de `AdminUserController` appelle la méthode privée `requireAdmin()` en premier. Cette méthode vérifie si `user_id` et `user_role === 'ADMIN'` existent en session. Si non, elle retourne une redirection vers `/connexion`. Si oui, elle retourne `null` et l'action continue.

**Q : Comment est géré l'upload de la photo de profil ?**
R : On utilise `$request->files->get('profilePhotoFile')` pour récupérer le fichier uploadé. On génère un nom unique avec `uniqid()`, on déplace le fichier vers `/public/uploads/profiles/`, et on sauvegarde le chemin relatif (`/uploads/profiles/nom_du_fichier.jpg`) dans le champ `profile_photo` de l'entité `Personne`.

**Q : Pourquoi `#[ORM\GeneratedValue]` était-il nécessaire ?**
R : Sans cette annotation, Doctrine utilise la stratégie `NONE` pour l'ID, ce qui veut dire que vous devez fournir l'ID manuellement avant de faire un `persist()`. Puisque la colonne `id` en base est `AUTO_INCREMENT`, Doctrine a besoin de `GeneratedValue` pour savoir qu'il doit laisser MySQL générer l'ID et récupérer la valeur générée après l'INSERT.

**Q : Comment les préférences sont-elles stockées en base ?**
R : La table `preference` a des colonnes texte (`typesVoyage`, `centresInteret`) qui stockent les valeurs sous forme de chaîne séparée par des virgules. Ex : `"Culture, Aventure, Randonnée"`. Dans le controller, on convertit le tableau PHP (retourné par les checkboxes) en chaîne avec `implode(', ', $array)` pour la sauvegarde, et on fait l'inverse avec `explode(',', $string)` pour l'affichage.

**Q : Comment la déconnexion avec confirmation est-elle implémentée ?**
R : On utilise la bibliothèque **SweetAlert2** (déjà incluse dans le projet). Le lien "Déconnexion" appelle une fonction JavaScript `confirmLogout()` qui affiche une boîte de dialogue. Si l'utilisateur clique "Oui", un formulaire GET caché (`<form id="logoutForm">`) soumet vers `/deconnexion` qui vide la session et redirige vers `/connexion`.

**Q : Qu'est-ce qui se passe si on accède directement à /admin/utilisateurs sans être connecté ?**
R : Le controller vérifie la session. Si `user_id` n'est pas en session (utilisateur non connecté) ou si `user_role` n'est pas `ADMIN`, on est immédiatement redirigé vers `/connexion`. La route admin n'est plus accessible depuis la navbar publique non plus.

**Q : Comment fonctionne le filtre dans le tableau des utilisateurs ?**
R : C'est un filtre **côté client** (JavaScript pur, pas d'appel serveur). Chaque ligne du tableau a des attributs `data-role` et `data-name`. La fonction `filterTable()` est appelée à chaque saisie dans le champ de recherche ou changement du select de rôle. Elle parcourt toutes les lignes et met `display: none` sur celles qui ne correspondent pas.

---

*Documentation générée pour le module Gestion des Utilisateurs — Projet Rehla — 2026*
