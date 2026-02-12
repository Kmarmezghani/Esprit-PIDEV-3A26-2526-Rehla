package rehla;

import models.Personne;
import models.Post;
import services.PostService;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        Personne user1 = new Personne();
        user1.setId(1);
        Post post = new Post();
        post.setTitre("Mon premier post");
        post.setContenu("Contenu du post...");
        post.setDatePublication(LocalDate.now());
        post.setPopularite(0);
        post.setAuteur(user1);
        PostService ps1 = new PostService();

        ps1.add(post);
        Post post2 = new Post();
        post.setTitre("Mon 2eme post");
        post.setContenu("essai du post...");
        post.setDatePublication(LocalDate.now());
        post.setPopularite(12);
        post.setAuteur(user1);

        ps1.add(post);
    }
}
