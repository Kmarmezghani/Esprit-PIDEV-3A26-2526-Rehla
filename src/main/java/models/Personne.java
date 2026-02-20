package models;

import java.time.LocalDateTime;

/**
 * Entity matching the database table `personne`.
 * Each user has: id, nom, prenom, email, motDePasse, dateInscription, role, statutCompte.
 */
public class Personne {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private LocalDateTime dateInscription;
    private String role;
    private String statutCompte;
    private Favoris favoris;

    public Personne() {}

    public Personne(int id, String nom, String prenom, String email, String motDePasse,
                    LocalDateTime dateInscription, String role, String statutCompte) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dateInscription = dateInscription;
        this.role = role;
        this.statutCompte = statutCompte;
    }

    public Personne(String nom, String prenom, String email, String motDePasse,
                    LocalDateTime dateInscription, String role, String statutCompte) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dateInscription = dateInscription;
        this.role = role;
        this.statutCompte = statutCompte;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatutCompte() { return statutCompte; }
    public void setStatutCompte(String statutCompte) { this.statutCompte = statutCompte; }

    public Personne(int id, Favoris favoris, String statutCompte, String role, LocalDateTime dateInscription, String motDePasse, String email, String prenom, String nom) {
        this.id = id;
        this.favoris = favoris;
        this.statutCompte = statutCompte;
        this.role = role;
        this.dateInscription = dateInscription;
        this.motDePasse = motDePasse;
        this.email = email;
        this.prenom = prenom;
        this.nom = nom;
    }

    public Favoris getFavoris() {
        return favoris;
    }

    public void setFavoris(Favoris favoris) {
        this.favoris = favoris;
    }

    @Override
    public String toString() {
        return "Personne{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "', role='" + role + "'}";
    }
}