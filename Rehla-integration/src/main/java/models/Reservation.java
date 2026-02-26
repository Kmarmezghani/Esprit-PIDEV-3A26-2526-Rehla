package models;

import java.sql.Date;

public class Reservation {

    private int id;
    private Date dateReservation;
    private Date dateDebut;
    private Date dateFin;
    private String statut;
    private double coutTotal;

    private int personneId;            // FK personne_id
    private Integer destinationId;     // FK destination_id (nullable) - optional
    private int nbTickets = 0;         // optional

    public Reservation() {}

    // For READ/UPDATE
    public Reservation(int id, Date dateReservation, Date dateDebut, Date dateFin,
                       String statut, double coutTotal, int personneId,
                       Integer destinationId, int nbTickets) {
        this.id = id;
        this.dateReservation = dateReservation;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.coutTotal = coutTotal;
        this.personneId = personneId;
        this.destinationId = destinationId;
        this.nbTickets = nbTickets;
    }

    // For CREATE
    public Reservation(Date dateReservation, Date dateDebut, Date dateFin,
                       String statut, double coutTotal, int personneId,
                       Integer destinationId, int nbTickets) {
        this.dateReservation = dateReservation;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.coutTotal = coutTotal;
        this.personneId = personneId;
        this.destinationId = destinationId;
        this.nbTickets = nbTickets;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDateReservation() { return dateReservation; }
    public void setDateReservation(Date dateReservation) { this.dateReservation = dateReservation; }

    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }

    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getCoutTotal() { return coutTotal; }
    public void setCoutTotal(double coutTotal) { this.coutTotal = coutTotal; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }

    public Integer getDestinationId() { return destinationId; }
    public void setDestinationId(Integer destinationId) { this.destinationId = destinationId; }

    public int getNbTickets() { return nbTickets; }
    public void setNbTickets(int nbTickets) { this.nbTickets = nbTickets; }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", dateReservation=" + dateReservation +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut='" + statut + '\'' +
                ", coutTotal=" + coutTotal +
                ", personneId=" + personneId +
                ", destinationId=" + destinationId +
                ", nbTickets=" + nbTickets +
                '}';
    }
}