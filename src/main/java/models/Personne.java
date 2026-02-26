package models;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity matching the database table `personne`.
 * Each user has: id, nom, prenom, email, motDePasse, dateInscription, role, statutCompte,
 * plus telephone and heureNotif for SMS notifications.
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

    // NOUVEAUX CHAMPS
    private String telephone;
    private LocalTime heureNotif;

    public Personne() {}

    public Personne(int id, String nom, String prenom, String email, String motDePasse,
                    LocalDateTime dateInscription, String role, String statutCompte,
                    String telephone, LocalTime heureNotif) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dateInscription = dateInscription;
        this.role = role;
        this.statutCompte = statutCompte;
        this.telephone = telephone;
        this.heureNotif = heureNotif;
    }

    public Personne(String nom, String prenom, String email, String password, LocalDateTime now, String role, String actif) {
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

    public Favoris getFavoris() { return favoris; }
    public void setFavoris(Favoris favoris) { this.favoris = favoris; }

    // GETTERS / SETTERS pour SMS
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalTime getHeureNotif() { return heureNotif; }
    public void setHeureNotif(LocalTime heureNotif) { this.heureNotif = heureNotif; }

    @Override
    public String toString() {
        return "Personne{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "', role='" + role + "'}";
    }
}