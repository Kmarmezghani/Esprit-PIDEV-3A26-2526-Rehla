package rehla;

import models.Personne;
import models.Post;
import services.PostService;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        Personne user1 = new Personne();
        user1.setId(1);
        Post post = new Post();
        post.setTitre("esaiiiiiiiiiiiiii");
        post.setContenu("esssssssss du post...");
        post.setDatePublication(LocalDateTime.now());
        post.setPopularite(0);
        post.setAuteur(user1);
        PostService ps1 = new PostService();

        ps1.add(post);
        Post post2 = new Post();
        post.setTitre("Mon eeeeeeeee post");
        post.setContenu("essai du post...");
        post.setDatePublication(LocalDateTime.now());
        post.setPopularite(12);
        post.setAuteur(user1);

        ps1.add(post);
    }
}
