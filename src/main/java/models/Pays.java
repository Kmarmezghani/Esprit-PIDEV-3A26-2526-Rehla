package models;

public class Pays {
    private int id;
    private String nom;
    private String description;
    private int visitCount;

    // Constructors
    public Pays() {}

    public Pays(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    public Pays(int id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }

    public Pays(String nom, String description, int visitCount) {
        this.nom = nom;
        this.description = description;
        this.visitCount = visitCount;
    }

    public Pays(int id, String nom, String description, int visitCount) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.visitCount = visitCount;
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

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    @Override
    public String toString() {
        return "Pays{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", visitCount=" + visitCount +
                '}';
    }
}
