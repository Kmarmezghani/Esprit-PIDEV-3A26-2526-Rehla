package models;

import java.sql.Date;

public class Ticket {

    private int id;
    private Integer reservationId;
    private String type;
    private double prix;
    private String statut;
    private Date dateDebut;
    private Date dateFin;
    private int destinationId;
    private String destinationNom;
    private boolean selected;
    private Integer activiteId;


    public Ticket() {}

    // CREATE
    public Ticket(Integer reservationId, Integer activiteId, int destinationId,
                  String type, double prix, String statut, Date dateDebut, Date dateFin) {
        this.reservationId = reservationId;
        this.activiteId = activiteId;
        this.destinationId = destinationId;
        this.type = type;
        this.prix = prix;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // READ / UPDATE
    public Ticket(int id, Integer reservationId, Integer activiteId, String type,
                  double prix, String statut, Date dateDebut, Date dateFin, int destinationId) {
        this.id = id;
        this.reservationId = reservationId;
        this.activiteId = activiteId;
        this.type = type;
        this.prix = prix;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.destinationId = destinationId;
    }

    public int getId() {return id;}

    public Integer getReservationId() {return reservationId;}

    public String getType() {return type;}

    public double getPrix() {return prix;}

    public String getStatut() {return statut;}

    public Date getDateDebut() {return dateDebut;}

    public Date getDateFin() {return dateFin;}

    public int getDestinationId() { return destinationId; }
    public Integer getActiviteId() { return activiteId; }
    public String getDestinationNom() { return destinationNom; }

    public boolean isSelected() { return selected;}

    public void setActiviteId(Integer activiteId) { this.activiteId = activiteId; }
    public void setId(int id) {this.id = id;}

    public void setReservationId(Integer reservationId) {this.reservationId = reservationId;}

    public void setType(String type) {this.type = type;}

    public void setPrix(double prix) {this.prix = prix;}

    public void setStatut(String statut) {this.statut = statut;}

    public void setDateDebut(Date dateDebut) {this.dateDebut = dateDebut;}

    public void setDateFin(Date dateFin) {this.dateFin = dateFin;}

    public void setDestinationId(int destinationId) { this.destinationId = destinationId; }

    public void setDestinationNom(String destinationNom) { this.destinationNom = destinationNom; }

    public void setSelected(boolean selected) { this.selected = selected;}
    /*@Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", type='" + type + '\'' +
                ", prix=" + prix +
                ", statut='" + statut + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }*/

    @Override
    public String toString() {
        return destinationNom + " - " + type + " - " + prix + "€";
    }
}