package models;

public class Ville {

    private int id;
    private String nom;
    private String region;
    private String typeTourisme;
    private String saison;
    private int popularite;
    private Double prix;
    private int paysId;
    private int visitCount;
    private double latitude;
    private double longitude;

    // ===== Constructors =====
    public Ville() {}

    // Constructor sans id (pour insert)
    public Ville(String nom, String region, String typeTourisme, String saison,
                 int popularite, Double prix, int paysId, int visitCount,
                 double latitude, double longitude) {
        this.nom = nom;
        this.region = region;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.popularite = popularite;
        this.prix = prix;
        this.paysId = paysId;
        this.visitCount = visitCount;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Constructor avec id (pour select/update)
    public Ville(int id, String nom, String region, String typeTourisme, String saison,
                 int popularite, Double prix, int paysId, int visitCount,
                 double latitude, double longitude) {
        this.id = id;
        this.nom = nom;
        this.region = region;
        this.typeTourisme = typeTourisme;
        this.saison = saison;
        this.popularite = popularite;
        this.prix = prix;
        this.paysId = paysId;
        this.visitCount = visitCount;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ===== Getters/Setters =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getTypeTourisme() { return typeTourisme; }
    public void setTypeTourisme(String typeTourisme) { this.typeTourisme = typeTourisme; }

    public String getSaison() { return saison; }
    public void setSaison(String saison) { this.saison = saison; }

    public int getPopularite() { return popularite; }
    public void setPopularite(int popularite) { this.popularite = popularite; }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }

    public int getPaysId() { return paysId; }
    public void setPaysId(int paysId) { this.paysId = paysId; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "Ville{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", region='" + region + '\'' +
                ", typeTourisme='" + typeTourisme + '\'' +
                ", saison='" + saison + '\'' +
                ", popularite=" + popularite +
                ", prix=" + prix +
                ", paysId=" + paysId +
                ", visitCount=" + visitCount +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}