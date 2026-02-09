package services;

import models.Activite;
import util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService implements IService<Activite> {
    Connection conn;

    public ActiviteService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Activite activite) {
        activite.setNoteMoyenne(0);
        String SQL = "INSERT INTO activite (nom, description, prix, duree, typeActivite, noteMoyenne, guide_id) VALUES ('" +
                activite.getNom() + "','" +
                activite.getDescription() + "'," +
                activite.getPrix() + "," +
                activite.getDuree() + ",'" +
                activite.getTypeActivite() + "'," +
                activite.getNoteMoyenne() + "," +
                (activite.getGuideId() > 0 ? activite.getGuideId() : "NULL") + ")";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Activite added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Activite activite) {
        String SQL = "UPDATE activite SET " +
                "nom = '" + activite.getNom() + "', " +
                "description = '" + activite.getDescription() + "', " +
                "prix = " + activite.getPrix() + ", " +
                "duree = " + activite.getDuree() + ", " +
                "typeActivite = '" + activite.getTypeActivite() + "', " +
                "noteMoyenne = " + activite.getNoteMoyenne() + ", " +
                "guide_id = " + (activite.getGuideId() > 0 ? activite.getGuideId() : "NULL") +
                " WHERE id = " + activite.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Activite updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Activite activite) {
        String SQL = "DELETE FROM activite WHERE id = " + activite.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Activite deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Activite> getAll() {
        String req = "SELECT * FROM activite";
        List<Activite> activites = new ArrayList<>();
        try {
            Statement stm = this.conn.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Activite a = new Activite();
                a.setId(rs.getInt("id"));
                a.setNom(rs.getString("nom"));
                a.setDescription(rs.getString("description"));
                a.setPrix(rs.getDouble("prix"));
                a.setDuree(rs.getDouble("duree"));
                a.setTypeActivite(rs.getString("typeActivite"));
                a.setNoteMoyenne(rs.getDouble("noteMoyenne"));
                a.setGuideId(rs.getInt("guide_id")); // 0 if NULL

                activites.add(a);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return activites;
    }

    public void updateNoteMoyenne(int activiteId) {
        String SQL = "UPDATE activite SET noteMoyenne = " +
                "(SELECT AVG(note) FROM avis WHERE activite_id = " + activiteId + ") " +
                "WHERE id = " + activiteId;
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Average note updated for activity " + activiteId);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
