package models;

public class Activite {

    int id;
    String nom;
    String description;
    double prix;
    double duree;
    String typeActivite;
    double noteMoyenne;
    int guideId;


    public Activite() {}


    public Activite(String nom, String description, double prix, double duree, String typeActivite, int guideId) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.duree = duree;
        this.typeActivite = typeActivite;
        this.guideId = guideId;
    }

    public Activite(int id, String nom, String description, double prix, double duree, String typeActivite, int guideId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.duree = duree;
        this.typeActivite = typeActivite;
        this.guideId = guideId;
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", prix=" + prix +
                ", duree=" + duree +
                ", typeActivite='" + typeActivite + '\'' +
                ", noteMoyenne=" + noteMoyenne +
                ", guideId=" + guideId +
                '}';
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public double getDuree() { return duree; }
    public void setDuree(double duree) { this.duree = duree; }

    public String getTypeActivite() { return typeActivite; }
    public void setTypeActivite(String typeActivite) { this.typeActivite = typeActivite; }

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getGuideId() { return guideId; }
    public void setGuideId(int guideId) { this.guideId = guideId; }
}
