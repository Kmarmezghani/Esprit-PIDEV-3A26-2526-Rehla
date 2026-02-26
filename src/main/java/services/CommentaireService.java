package services;

import models.Commentaire;
import models.Personne;
import models.Post;
import models.notification;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommentaireService  {

    private Connection conn;
    notificationService notificationService = new notificationService();
    public CommentaireService() {
        conn = DBConnection.getInstance().getConn();
    }

//    @Override
//    public void add(Commentaire c) {
//        String sql = "INSERT INTO commentaire (contenu, dateCommentaire, personne_id, post_id) VALUES (?, ?, ?, ?)";
//
//        try {
//            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//
//            ps.setString(1, c.getContenu());
//            ps.setTimestamp(2, Timestamp.valueOf(c.getDateCommentaire()));
//            ps.setInt(3, c.getAuteur().getId());
//            ps.setInt(4, c.getPost().getId());
//
//            ps.executeUpdate();
//
//            ResultSet rs = ps.getGeneratedKeys();
//            int commentId = -1;
//            if (rs.next()) {
//                commentId = rs.getInt(1);
//            }
//
//            System.out.println("Commentaire ajouté !");
//
//            // 🔥 Création notification
//            if (c.getPost().getAuteur().getId() != c.getAuteur().getId()) {
//
//                notification notif = new notification(
//                        c.getAuteur().getPrenom() + " a commenté votre post",
//                        "COMMENT",
//                        c.getPost().getId(),
//                        commentId,
//                        c.getAuteur().getId(),
//                        c.getPost().getAuteur().getId()
//                );
//
//                notificationService.add(notif);
//            }
//
//        } catch (SQLException e) {
//            System.out.println("Erreur ajout commentaire : " + e.getMessage());
//        }
//    }



    public void update(Commentaire c) {
        String sql = "UPDATE commentaire SET contenu=?, dateCommentaire=?, personne_id=?, post_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getContenu());
            ps.setTimestamp(2, Timestamp.valueOf(c.getDateCommentaire()));
            ps.setInt(3, c.getAuteur().getId());
            ps.setInt(4, c.getPost().getId());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
            System.out.println("Commentaire mis à jour !");
        } catch (SQLException e) {
            System.out.println("Erreur update commentaire : " + e.getMessage());
        }
    }

    public void delete(Commentaire c) {
        String sql = "DELETE FROM commentaire WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, c.getId());
            ps.executeUpdate();
            System.out.println("Commentaire supprimé !");
        } catch (SQLException e) {
            System.out.println("Erreur suppression commentaire : " + e.getMessage());
        }
    }

    public List<Commentaire> getAll() {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT * FROM commentaire";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Commentaire c = new Commentaire();
                c.setId(rs.getInt("id"));
                c.setContenu(rs.getString("contenu"));
                c.setDateCommentaire(
                        rs.getTimestamp("dateCommentaire").toLocalDateTime()
                );


                // Auteur
                Personne auteur = new Personne();
                auteur.setId(rs.getInt("personne_id"));
                c.setAuteur(auteur);

                // Post
                Post post = new Post();
                post.setId(rs.getInt("post_id"));
                c.setPost(post);

                commentaires.add(c);
            }
        } catch (SQLException e) {
            System.out.println("Erreur récupération commentaires : " + e.getMessage());
        }
        return commentaires;
    }
    public List<Commentaire> getCommentairesByPost(Post post) {
        List<Commentaire> commentaires = new ArrayList<>();
        String sql = "SELECT c.id, c.contenu, c.dateCommentaire, p.id AS auteur_id, p.prenom, p.nom " +
                "FROM commentaire c " +
                "JOIN personne p ON c.personne_id = p.id " +
                "WHERE c.post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, post.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Commentaire c = new Commentaire();
                c.setId(rs.getInt("id"));
                c.setContenu(rs.getString("contenu"));
                c.setDateCommentaire(
                        rs.getTimestamp("dateCommentaire").toLocalDateTime()
                );


                // Auteur complet
                Personne auteur = new Personne();
                auteur.setId(rs.getInt("auteur_id"));
                auteur.setPrenom(rs.getString("prenom"));
                auteur.setNom(rs.getString("nom"));
                c.setAuteur(auteur);

                // Post
                c.setPost(post);

                commentaires.add(c);
            }

        } catch (SQLException e) {
            System.out.println("Erreur récupération commentaires par post : " + e.getMessage());
        }
        return commentaires;
    }
    public int countByPost(int postId) {
        int count = 0;
        String query = "SELECT COUNT(*) FROM commentaire WHERE post_id = ?";

        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, postId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }
    public Commentaire addAndReturn(Commentaire c){

        String sql =
                "INSERT INTO commentaire(contenu,dateCommentaire,post_id,personne_id) VALUES(?,?,?,?)";

        try{

            PreparedStatement ps =
                    conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, c.getContenu());
            ps.setTimestamp(2,
                    Timestamp.valueOf(c.getDateCommentaire())
            );

            ps.setInt(3, c.getPost().getId());
            ps.setInt(4, c.getAuteur().getId());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            int commentId = -1;

            if(rs.next()){
                commentId = rs.getInt(1);
                c.setId(commentId);
            }

            // 🔥 Notification après récupération ID
            if (c.getPost().getAuteur().getId() != c.getAuteur().getId()) {

                notification notif = new notification(
                        c.getAuteur().getPrenom() + " "+ c.getAuteur().getNom()  +" a commenté votre post ",
                        "COMMENT",
                        c.getPost().getId(),
                        commentId,
                        c.getAuteur().getId(),
                        c.getPost().getAuteur().getId()
                );

                notificationService.add(notif);
            }

            return c;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public Map<Integer, Integer> countByPosts(List<Post> posts) {

        Map<Integer, Integer> counts = new HashMap<>();

        if (posts == null || posts.isEmpty()) {
            return counts;
        }

        String ids = posts.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(Collectors.joining(","));

        String query = "SELECT post_id, COUNT(*) as total " +
                "FROM commentaire " +
                "WHERE post_id IN (" + ids + ") " +
                "GROUP BY post_id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                counts.put(
                        rs.getInt("post_id"),
                        rs.getInt("total")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return counts;
    }

}
