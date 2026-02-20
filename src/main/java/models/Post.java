package models;

import java.time.LocalDate;

public class Post {

    private int id;
    private String titre;
    private String contenu;
    private LocalDate datePublication;
    private int popularite;
    private Personne auteur;
    private int nbLikes;
    private String image;
    public Post(String image) {
        this.image = image;
    }

    public Post(int id, String titre, String contenu,
                LocalDate datePublication, int popularite,
                Personne auteur, String image) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.popularite = popularite;
        this.auteur = auteur;
        this.image = image;
    }

    public Post() {

    }

    public Post(String yosrAmamou, String s, String s1) {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public int getPopularite() {
        return popularite;
    }

    public void setPopularite(int popularite) {
        this.popularite = popularite;
    }

    public Personne getAuteur() {
        return auteur;
    }

    public void setAuteur(Personne auteur) {
        this.auteur = auteur;
    }

    public int getNbLikes() {
        return nbLikes;
    }

    public void setNbLikes(int nbLikes) {
        this.nbLikes = nbLikes;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

