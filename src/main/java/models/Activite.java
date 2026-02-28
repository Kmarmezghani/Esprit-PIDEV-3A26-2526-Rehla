package models;

import java.time.LocalDateTime;

public class Activite {

    private int id;
    private String nom;
    private String description;
    private double prix;
    private String typeActivite;
    private double noteMoyenne;
    private int guideId;
    private int destinationId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String status;
    private Integer maxPlaces;
    private String image;

    public Activite() {}

    public Activite(String nom, String description, double prix, String typeActivite,
                    int guideId, int destinationId, LocalDateTime dateDebut, LocalDateTime dateFin, String status,
                    String image) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.typeActivite = typeActivite;
        this.guideId = guideId;
        this.destinationId = destinationId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.status = status;
        this.image = image;
    }

    public Activite(int id, String nom, String description, double prix, String typeActivite, double noteMoyenne,
                    int guideId, int destinationId, LocalDateTime dateDebut, LocalDateTime dateFin, String status,
                    String image) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.typeActivite = typeActivite;
        this.noteMoyenne = noteMoyenne;
        this.guideId = guideId;
        this.destinationId = destinationId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.status = status;
        this.image = image;
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", prix=" + prix +
                ", typeActivite='" + typeActivite + '\'' +
                ", noteMoyenne=" + noteMoyenne +
                ", guideId=" + guideId +
                ", destinationId=" + destinationId +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", status='" + status + '\'' +
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

    public String getTypeActivite() { return typeActivite; }
    public void setTypeActivite(String typeActivite) { this.typeActivite = typeActivite; }

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getGuideId() { return guideId; }
    public void setGuideId(int guideId) { this.guideId = guideId; }

    public int getDestinationId() { return destinationId; }
    public void setDestinationId(int destinationId) { this.destinationId = destinationId; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMaxPlaces() { return maxPlaces; }
    public void setMaxPlaces(Integer maxPlaces) { this.maxPlaces = maxPlaces; }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    private boolean flashSale;
    private Double flashPrice;
    private LocalDateTime flashExpiresAt;

    public boolean isFlashSale() { return flashSale; }
    public void setFlashSale(boolean flashSale) { this.flashSale = flashSale; }

    public Double getFlashPrice() { return flashPrice; }
    public void setFlashPrice(Double flashPrice) { this.flashPrice = flashPrice; }

    public LocalDateTime getFlashExpiresAt() { return flashExpiresAt; }
    public void setFlashExpiresAt(LocalDateTime flashExpiresAt) { this.flashExpiresAt = flashExpiresAt; }
}
