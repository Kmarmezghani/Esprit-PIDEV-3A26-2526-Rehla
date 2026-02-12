package services;

import models.Commentaire;
import models.Personne;
import models.Post;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements IService<Commentaire> {

    private Connection conn;

    public CommentaireService() {
        conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Commentaire c) {
        String sql = "INSERT INTO commentaire (contenu, dateCommentaire, personne_id, post_id) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getContenu());
            ps.setDate(2, Date.valueOf(c.getDateCommentaire()));
            ps.setInt(3, c.getAuteur().getId());
            ps.setInt(4, c.getPost().getId());
            ps.executeUpdate();
            System.out.println("Commentaire ajouté !");
        } catch (SQLException e) {
            System.out.println("Erreur ajout commentaire : " + e.getMessage());
        }
    }

    @Override
    public void update(Commentaire c) {
        String sql = "UPDATE commentaire SET contenu=?, dateCommentaire=?, personne_id=?, post_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getContenu());
            ps.setDate(2, Date.valueOf(c.getDateCommentaire()));
            ps.setInt(3, c.getAuteur().getId());
            ps.setInt(4, c.getPost().getId());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
            System.out.println("Commentaire mis à jour !");
        } catch (SQLException e) {
            System.out.println("Erreur update commentaire : " + e.getMessage());
        }
    }

    @Override
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

    @Override
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
                c.setDateCommentaire(rs.getDate("dateCommentaire").toLocalDate());

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
        String sql = "SELECT * FROM commentaire WHERE post_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, post.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Commentaire c = new Commentaire();
                c.setId(rs.getInt("id"));
                c.setContenu(rs.getString("contenu"));
                c.setDateCommentaire(rs.getDate("dateCommentaire").toLocalDate());

                // Auteur
                Personne auteur = new Personne();
                auteur.setId(rs.getInt("personne_id"));
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

}
