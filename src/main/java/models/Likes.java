package models;

public class Likes {

    private int id;
    private boolean statut;
    private Personne personne;
    private Post post;

    public Likes() {}

    public Likes(int id, boolean statut,
                 Personne personne, Post post) {
        this.id = id;
        this.statut = statut;
        this.personne = personne;
        this.post = post;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

}
