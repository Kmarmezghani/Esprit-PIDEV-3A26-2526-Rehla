package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Review {
    private int id;
    private int activiteId;
    private int personneId;
    private String userName;
    private String commentaire;
    private int note;
    private LocalDateTime dateAvis;

    // getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActiviteId() { return activiteId; }
    public void setActiviteId(int activiteId) { this.activiteId = activiteId; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public LocalDateTime getDateAvis() { return dateAvis; }
    public void setDateAvis(LocalDateTime dateAvis) { this.dateAvis = dateAvis; }
}
