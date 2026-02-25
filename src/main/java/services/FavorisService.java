package services;

import models.Personne;
import models.Post;
import util.DBConnection;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FavorisService {

    Connection cnx = DBConnection.getInstance().getConn();
    private int getOrCreateFavoris(Personne p) {

        try {

            String select = "SELECT id FROM favoris WHERE personne_id=?";
            PreparedStatement ps = cnx.prepareStatement(select);
            ps.setInt(1, p.getId());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            // 🔹 sinon créer un favoris
            String insert = "INSERT INTO favoris (personne_id, dateCreation) VALUES (?, NOW())";
            PreparedStatement ps2 = cnx.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS);
            ps2.setInt(1, p.getId());
            ps2.executeUpdate();

            ResultSet generated = ps2.getGeneratedKeys();

            if (generated.next()) {
                return generated.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
    public boolean isFavori(Personne p, Post post) {

        try {
            String sql = "SELECT * FROM favoris_post fp "
                    + "JOIN favoris f ON fp.favoris_id = f.id "
                    + "WHERE f.personne_id=? AND fp.post_id=?";

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, p.getId());
            ps.setInt(2, post.getId());

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void addFavori(Personne p, Post post) {

        try {

            int favorisId = getOrCreateFavoris(p);

            String sql = "INSERT INTO favoris_post (favoris_id, post_id, dateAjout) VALUES (?, ?, NOW())";

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, favorisId);
            ps.setInt(2, post.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeFavori(Personne p, Post post) {

        try {

            String sql = "DELETE fp FROM favoris_post fp "
                    + "JOIN favoris f ON fp.favoris_id=f.id "
                    + "WHERE f.personne_id=? AND fp.post_id=?";

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, p.getId());
            ps.setInt(2, post.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Post> getFavorisPosts(int personneId) {

        List<Post> posts = new ArrayList<>();

        String req = "SELECT p.*, per.id as pid, per.nom, per.prenom " +
                "FROM post p " +
                "JOIN favoris_post fp ON p.id = fp.post_id " +
                "JOIN favoris f ON f.id = fp.favoris_id " +
                "JOIN personne per ON p.personne_id = per.id " +
                "WHERE f.personne_id = ?";

        try {
            PreparedStatement ps = this.cnx.prepareStatement(req);
            ps.setInt(1, personneId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Personne auteur = new Personne();
                auteur.setId(rs.getInt("pid"));
                auteur.setNom(rs.getString("nom"));
                auteur.setPrenom(rs.getString("prenom"));

                Post post = new Post();
                post.setAuteur(auteur);
                post.setId(rs.getInt("id"));
                post.setTitre(rs.getString("titre"));
                post.setContenu(rs.getString("contenu"));
                post.setPopularite(rs.getInt("popularite"));
                post.setImage(rs.getString("image"));

                post.setDatePublication(
                        rs.getTimestamp("datePublication").toLocalDateTime()
                );

                posts.add(post);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return posts;
    }
}