package models;

import java.sql.Time;

public class Attraction {
    private int id;
    private String nom;
    private String description;
    private String type;
    private double prix;
    private Time heureOuverture;
    private Time heureFermeture;
    private boolean estFerme;
    private int villeId;

    // Constructors
    public Attraction() {}

    public Attraction(String nom, String description, String type, double prix, Time heureOuverture, Time heureFermeture, boolean estFerme, int villeId) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.prix = prix;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
        this.estFerme = estFerme;
        this.villeId = villeId;
    }

    public Attraction(int id, String nom, String description, String type, double prix, Time heureOuverture, Time heureFermeture, boolean estFerme, int villeId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.prix = prix;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
        this.estFerme = estFerme;
        this.villeId = villeId;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public Time getHeureOuverture() {
        return heureOuverture;
    }

    public void setHeureOuverture(Time heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public Time getHeureFermeture() {
        return heureFermeture;
    }

    public void setHeureFermeture(Time heureFermeture) {
        this.heureFermeture = heureFermeture;
    }

    public boolean isEstFerme() {
        return estFerme;
    }

    public void setEstFerme(boolean estFerme) {
        this.estFerme = estFerme;
    }

    public int getVilleId() {
        return villeId;
    }

    public void setVilleId(int villeId) {
        this.villeId = villeId;
    }

    @Override
    public String toString() {
        return "Attraction{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", prix=" + prix +
                ", heureOuverture=" + heureOuverture +
                ", heureFermeture=" + heureFermeture +
                ", estFerme=" + estFerme +
                ", villeId=" + villeId +
                '}';
    }
}
