package models;

public class Pays {
    private int id;
    private String nom;
    private String continent;
    private String description;

    // Constructors
    public Pays() {}

    public Pays(String nom, String continent, String description) {
        this.nom = nom;
        this.continent = continent;
        this.description = description;
    }

    public Pays(int id, String nom, String continent, String description) {
        this.id = id;
        this.nom = nom;
        this.continent = continent;
        this.description = description;
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

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Pays{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", continent='" + continent + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
