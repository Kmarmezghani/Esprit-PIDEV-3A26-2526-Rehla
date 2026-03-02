package models;


import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class Favoris {
    private int id;
    private LocalDateTime dateCreation;

    private Personne personne; // chaque favoris appartient à une seule personne

    private List<Post> posts = new ArrayList<>(); // posts ajoutés dans ce favoris



    public Favoris(int id, LocalDateTime dateCreation, Personne personne, List<Post> posts) {
        super();
        this.id = id;
        this.dateCreation = dateCreation;
        this.personne = personne;
        this.posts = posts;
    }

    public Favoris() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }



}


