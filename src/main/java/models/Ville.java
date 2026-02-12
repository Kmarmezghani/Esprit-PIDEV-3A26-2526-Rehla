package models;

public class Ville {
    private int id;
    private String nom;
    private int paysId;
    private String region;
    private String typeTourisme;
    private String saison;
    private int popularite;

    // Constructors
    public Ville() {}

    public Ville(String nom, int paysId, String region, String typeTourisme, String saison, int popularite) {
        this.nom = nom;
        this.paysId = paysId;
        this.region = region;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.popularite = popularite;
    }

    public Ville(int id, String nom, int paysId, String region, String typeTourisme, String saison, int popularite) {
        this.id = id;
        this.nom = nom;
        this.paysId = paysId;
        this.region = region;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.popularite = popularite;
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

    public int getPaysId() {
        return paysId;
    }

    public void setPaysId(int paysId) {
        this.paysId = paysId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTypeTourisme() {
        return typeTourisme;
    }

    public void setTypeTourisme(String typeTourisme) {
        this.typeTourisme = typeTourisme;
    }

    public String getSaison() {
        return saison;
    }

    public void setSaison(String saison) {
        this.saison = saison;
    }

    public int getPopularite() {
        return popularite;
    }

    public void setPopularite(int popularite) {
        this.popularite = popularite;
    }

    @Override
    public String toString() {
        return "Ville{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", paysId=" + paysId +
                ", region='" + region + '\'' +
                ", typeTourisme='" + typeTourisme + '\'' +
                ", saison='" + saison + '\'' +
                ", popularite=" + popularite +
                '}';
    }
}
