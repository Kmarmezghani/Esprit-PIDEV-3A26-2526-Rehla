package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import models.Favoris;

import models.Post;
import util.DBConnection;

public class FavorisService {

    private Connection conn;

    public FavorisService() {
        conn = DBConnection.getInstance().getConn();
    }

    // Récupérer tous les posts d’un favoris
    public List<Post> getPosts(Favoris favoris) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.id, p.titre, p.contenu, p.datePublication, p.popularite, p.personne_id " +
                "FROM post p " +
                "JOIN favoris_post fp ON p.id = fp.post_id " +
                "WHERE fp.favoris_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, favoris.getPersonne().getFavoris().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post post = new Post();
                post.setId(rs.getInt("id"));
                post.setTitre(rs.getString("titre"));
                post.setContenu(rs.getString("contenu"));
                post.setDatePublication(
                        rs.getTimestamp("datePublication").toLocalDateTime()
                );

                post.setPopularite(rs.getInt("popularite"));
                // TODO: récupérer l'auteur si nécessaire
                posts.add(post);
            }
        } catch (SQLException e) {
            System.out.println("Erreur récupération posts favoris: " + e.getMessage());
        }
        return posts;
    }

    // Ajouter un post au favoris
    public void addPost(Favoris favoris, Post post) {
        String sql = "INSERT INTO favoris_post(favoris_id, post_id) VALUES(?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, favoris.getPersonne().getFavoris().getId());
            ps.setInt(2, post.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout post favoris: " + e.getMessage());
        }
    }

    // Supprimer un post du favoris
    public void removePost(Favoris favoris, Post post) {
        String sql = "DELETE FROM favoris_post WHERE favoris_id = ? AND post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, favoris.getPersonne().getFavoris().getId());
            ps.setInt(2, post.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur suppression post favoris: " + e.getMessage());
        }
    }
}
