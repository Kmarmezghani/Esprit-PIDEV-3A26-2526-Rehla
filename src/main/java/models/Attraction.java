package models;

public class Attraction {

    private int id;
    private String nom;
    private String type;
    private double prix;
    private String heuresOuverture;
    private String description;
    private int villeId;

    public Attraction() {}

    public Attraction(int id, String nom, String type, double prix, String heuresOuverture, String description, int villeId) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.prix = prix;
        this.heuresOuverture = heuresOuverture;
        this.description = description;
        this.villeId = villeId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public String getHeuresOuverture() { return heuresOuverture; }
    public void setHeuresOuverture(String heuresOuverture) { this.heuresOuverture = heuresOuverture; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getVilleId() { return villeId; }
    public void setVilleId(int villeId) { this.villeId = villeId; }
}