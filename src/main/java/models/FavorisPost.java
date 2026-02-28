package models;

import java.util.Date;

public class FavorisPost {
    private Favoris favoris;
    private Post post;
    private Date dateAjout;

    public FavorisPost(Favoris favoris, Post post) {
        this.favoris = favoris;
        this.post = post;
        this.dateAjout = new Date();
    }

    public FavorisPost() {

    }

    public Favoris getFavoris() {
        return favoris;
    }

    public void setFavoris(Favoris favoris) {
        this.favoris = favoris;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Date getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(Date dateAjout) {
        this.dateAjout = dateAjout;
    }


}
