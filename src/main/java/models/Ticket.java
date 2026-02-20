package models;

import java.sql.Date;

public class Ticket {

    private int id;
    private int reservationId;
    private String type;
    private double prix;
    private String statut;
    private Date dateDebut;
    private Date dateFin;

    public Ticket() {}

    // CREATE
    public Ticket(int reservationId, String type, double prix, String statut, Date dateDebut, Date dateFin) {
        this.reservationId = reservationId;
        this.type = type;
        this.prix = prix;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // READ / UPDATE
    public Ticket(int id, int reservationId, String type, double prix, String statut, Date dateDebut, Date dateFin) {
        this.id = id;
        this.reservationId = reservationId;
        this.type = type;
        this.prix = prix;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public int getId() {return id;}

    public int getReservationId() {return reservationId;}

    public String getType() {return type;}

    public double getPrix() {return prix;}

    public String getStatut() {return statut;}

    public Date getDateDebut() {return dateDebut;}

    public Date getDateFin() {return dateFin;}


    public void setId(int id) {this.id = id;}

    public void setReservationId(int reservationId) {this.reservationId = reservationId;}

    public void setType(String type) {this.type = type;}

    public void setPrix(double prix) {this.prix = prix;}

    public void setStatut(String statut) {this.statut = statut;}

    public void setDateDebut(Date dateDebut) {this.dateDebut = dateDebut;}

    public void setDateFin(Date dateFin) {this.dateFin = dateFin;}

    @Override
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
    }
}
