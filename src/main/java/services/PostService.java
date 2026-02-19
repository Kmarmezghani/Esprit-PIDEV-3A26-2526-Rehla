package services;


import models.Post;
import util.DBConnection;
import models.Personne;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;

public class PostService implements IService<Post> {

    private Connection conn;

    public PostService() {
        conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Post post) {
        String sql = "INSERT INTO post (titre, contenu, datePublication, popularite, personne_id, image) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setDate(3, Date.valueOf(post.getDatePublication()));
            ps.setInt(4, post.getPopularite());
            ps.setInt(5, post.getAuteur().getId());
            ps.setString(6, post.getImage());

            ps.executeUpdate();
            System.out.println("Post ajouté !");
        } catch (SQLException e) {
            System.out.println("Erreur ajout post : " + e.getMessage());
        }
    }

    @Override
    public void update(Post post) {
        String sql = "UPDATE post SET titre=?, contenu=?, datePublication=?, popularite=?, personne_id=?, image=? "
                + "WHERE id=?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setDate(3, Date.valueOf(post.getDatePublication()));
            ps.setInt(4, post.getPopularite());
            ps.setInt(5, post.getAuteur().getId());
            ps.setString(6, post.getImage());
            ps.setInt(7, post.getId());

            ps.executeUpdate();
            System.out.println("Post mis à jour !");
        } catch (SQLException e) {
            System.out.println("Erreur update post : " + e.getMessage());
        }
    }


    @Override
    public void delete(Post post) {
        String sql = "DELETE FROM post WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, post.getId());
            ps.executeUpdate();
            System.out.println("Post supprimé !");
        } catch (SQLException e) {
            System.out.println("Erreur suppression post : " + e.getMessage());
        }
    }

    @Override
    public List<Post> getAll() {

        List<Post> posts = new ArrayList<>();

        String sql = "SELECT p.*, pe.nom, pe.prenom " +
                "FROM post p " +
                "JOIN personne pe ON p.personne_id = pe.id";

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                Personne auteur = new Personne();
                auteur.setId(rs.getInt("personne_id"));
                auteur.setNom(rs.getString("nom"));
                auteur.setPrenom(rs.getString("prenom"));

                Post post = new Post(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getDate("datePublication").toLocalDate(),
                        rs.getInt("popularite"),
                        auteur,
                        rs.getString("image")
                );

                posts.add(post);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération posts : " + e.getMessage());
        }

        return posts;
    }

    // Récupérer tous les posts d'une personne
    public List<Post> getPostsByPersonne(Personne p) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post WHERE personne_id = ?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, p.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Post post = new Post();

                post.setId(rs.getInt("id"));
                post.setTitre(rs.getString("titre"));
                post.setContenu(rs.getString("contenu"));
                post.setDatePublication(
                        rs.getDate("datePublication").toLocalDate()
                );
                post.setPopularite(rs.getInt("popularite"));
                post.setAuteur(p);


                post.setImage(rs.getString("image"));

                posts.add(post);
            }

        } catch (SQLException e) {
            System.out.println("Erreur getPostsByPersonne : " + e.getMessage());
        }

        return posts;
    }

}
