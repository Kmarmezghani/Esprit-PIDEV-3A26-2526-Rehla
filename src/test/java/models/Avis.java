package models;

import java.sql.Date;

public class Avis {
    private int id;
    private int note;
    private String commentaire;
    private Date dateAvis;
    private int activiteId;
    private int personneId;

    public Avis() {}

    public Avis(int id, int note, String commentaire, Date dateAvis, int activiteId, int personneId) {
        this.id = id;
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
        this.activiteId = activiteId;
        this.personneId = personneId;
    }

    public Avis(int note, String commentaire, Date dateAvis, int activiteId, int personneId) {
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
        this.activiteId = activiteId;
        this.personneId = personneId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Date getDateAvis() { return dateAvis; }
    public void setDateAvis(Date dateAvis) { this.dateAvis = dateAvis; }

    public int getActiviteId() { return activiteId; }
    public void setActiviteId(int activiteId) { this.activiteId = activiteId; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }

    @Override
    public String toString() {
        return "Avis{" +
                "id=" + id +
                ", note=" + note +
                ", commentaire='" + commentaire + '\'' +
                ", dateAvis=" + dateAvis +
                ", activiteId=" + activiteId +
                ", personneId=" + personneId +
                '}';
    }
}
