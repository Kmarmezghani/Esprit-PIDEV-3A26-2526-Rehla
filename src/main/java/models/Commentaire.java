package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Commentaire {

    private int id;
    private String contenu;
    private LocalDateTime dateCommentaire;
    private Personne auteur;
    private Post post;

    public Commentaire() {}

    public Commentaire(int id, String contenu,
                       LocalDateTime dateCommentaire,
                       Personne auteur, Post post) {
        this.id = id;
        this.contenu = contenu;
        this.dateCommentaire = dateCommentaire;
        this.auteur = auteur;
        this.post = post;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateCommentaire() {
        return dateCommentaire;
    }

    public void setDateCommentaire(LocalDateTime dateCommentaire) {
        this.dateCommentaire = dateCommentaire;
    }

    public Personne getAuteur() {
        return auteur;
    }

    public void setAuteur(Personne auteur) {
        this.auteur = auteur;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

}

