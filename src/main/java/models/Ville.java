package models;

public class Ville {
    private int id;
    private String nom;
    private int paysId;
    private int visitCount;
    private double latitude;
    private double longitude;
    private String typeTourisme;
    private String saison;
    private String image;

    // Constructors
    public Ville() {}

    public Ville(String nom, int paysId, String typeTourisme, String saison, String image) {
        this.nom = nom;
        this.paysId = paysId;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.image = image;
    }

    public Ville(int id, String nom, int paysId, String typeTourisme, String saison, String image) {
        this.id = id;
        this.nom = nom;
        this.paysId = paysId;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.image = image;
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

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "Ville{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", paysId=" + paysId +
                ", visitCount=" + visitCount +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", typeTourisme='" + typeTourisme + '\'' +
                ", saison='" + saison + '\'' +
                ", image='" + image + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
