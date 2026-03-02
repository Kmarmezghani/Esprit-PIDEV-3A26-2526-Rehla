package models;

import java.sql.Date;

public class InscriptionActivite {

    private int id;
    private int activiteId;
    private int personneId;
    private Date dateInscription;

    public InscriptionActivite() {}

    public InscriptionActivite(int activiteId, int personneId, Date dateInscription) {
        this.activiteId = activiteId;
        this.personneId = personneId;
        this.dateInscription = dateInscription;
    }

    public InscriptionActivite(int id, int activiteId, int personneId, Date dateInscription) {
        this.id = id;
        this.activiteId = activiteId;
        this.personneId = personneId;
        this.dateInscription = dateInscription;
    }

    @Override
    public String toString() {
        return "InscriptionActivite{" +
                "id=" + id +
                ", activiteId=" + activiteId +
                ", personneId=" + personneId +
                ", dateInscription=" + dateInscription +
                '}';
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActiviteId() { return activiteId; }
    public void setActiviteId(int activiteId) { this.activiteId = activiteId; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }

    public Date getDateInscription() { return dateInscription; }
    public void setDateInscription(Date dateInscription) { this.dateInscription = dateInscription; }
}
