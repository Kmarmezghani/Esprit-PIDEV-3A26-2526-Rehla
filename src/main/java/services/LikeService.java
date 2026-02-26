package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import models.Personne;
import models.Post;
import models.notification;
import util.DBConnection;

public class LikeService {
    private Connection conn;
    notificationService notificationService = new notificationService();
    public LikeService() {
        conn = DBConnection.getInstance().getConn();
    }
    public void addLike(Personne personne, Post post) {
        String sql = "INSERT INTO likes (personne_id, post_id, statut) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, personne.getId());
            ps.setInt(2, post.getId());
            ps.setInt(3, 1); // 1 = like actif
            ps.executeUpdate();
            System.out.println("Like ajouté !");
            if(post.getAuteur().getId() != personne.getId()) {

                notification notif = new notification(
                        personne.getPrenom()+ " " + personne.getNom() + " a aimé votre post"  ,
                        "LIKE",
                        post.getId(),
                        null,
                        personne.getId(),
                        post.getAuteur().getId()
                );

                notificationService.add(notif);
            }
        } catch (SQLException e) {
            System.out.println("Erreur ajout like : " + e.getMessage());
        }
    }
    public void deleteLike(Personne personne, Post post) {
        String sql = "DELETE FROM likes WHERE personne_id = ? AND post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, personne.getId());
            ps.setInt(2, post.getId());
            ps.executeUpdate();
            System.out.println("Like supprimé !");
        } catch (SQLException e) {
            System.out.println("Erreur suppression like : " + e.getMessage());
        }
    }
    //Afficher les personnes qui ont liké un post
    public List<Personne> getPersonsOfLike(Post post) {
        List<Personne> personnes = new ArrayList<>();
        String sql = "SELECT p.* FROM personne p JOIN likes l ON p.id = l.personne_id WHERE l.post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, post.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Personne p = new Personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                personnes.add(p);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération personnes : " + e.getMessage());
        }
        return personnes;
    }
    public int getNbLikes(Post post) {
        int count = 0;
        String sql = "SELECT COUNT(*) as total FROM likes WHERE post_id = ? AND statut = 1";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, post.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur récupération nbLikes : " + e.getMessage());
        }
        return count;
    }
    public boolean isLikedByUser(Personne personne, Post post) {
        String sql = "SELECT * FROM likes WHERE personne_id = ? AND post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, personne.getId());
            ps.setInt(2, post.getId());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

}
