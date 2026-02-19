package models;

import java.time.LocalDate;

import Enumeration.Role;
import Enumeration.StatutCompte;

public class Personne {

    protected int id;
    protected String nom;
    protected String prenom;
    protected String email;
    protected String motDePasse;
    protected LocalDate dateNaissance;
    protected Role role;
    protected StatutCompte statutCompte;

    protected Favoris favoris;

    public Personne() {}



    public Personne(int id, String nom, String prenom, String email, String motDePasse, LocalDate dateNaissance,
                    Role role, StatutCompte statutCompte, Favoris favoris) {
        super();
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dateNaissance = dateNaissance;
        this.role = role;
        this.statutCompte = statutCompte;
        this.favoris = favoris;
    }

    @Override
    public String toString() {
        return prenom + " " + nom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public LocalDate getdateNaissance() {
        return dateNaissance;
    }

    public void setdateNaissance(LocalDate dateInscription) {
        this.dateNaissance = dateInscription;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
    }

    public Favoris getFavoris() {
        return favoris;
    }

    public void setFavoris(Favoris favoris) {
        this.favoris = favoris;
    }


}
