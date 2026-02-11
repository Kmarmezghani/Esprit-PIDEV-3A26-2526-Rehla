package services;

import util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InscriptionActiviteService implements IService<InscriptionActivite> {

    private Connection conn;

    public InscriptionActiviteService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(InscriptionActivite inscription) {
        String SQL = "INSERT INTO inscription_activite (activite_id, personne_id, dateInscription) VALUES (" +
                inscription.getActiviteId() + "," +
                inscription.getPersonneId() + ",'" +
                inscription.getDateInscription() + "')";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Inscription added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(InscriptionActivite inscription) {
        String SQL = "UPDATE inscription_activite SET " +
                "activite_id = " + inscription.getActiviteId() + ", " +
                "personne_id = " + inscription.getPersonneId() + ", " +
                "dateInscription = '" + inscription.getDateInscription() + "' " +
                "WHERE id = " + inscription.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Inscription updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(InscriptionActivite inscription) {
        String SQL = "DELETE FROM inscription_activite WHERE id = " + inscription.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Inscription deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<InscriptionActivite> getAll() {
        List<InscriptionActivite> inscriptions = new ArrayList<>();
        String SQL = "SELECT * FROM inscription_activite";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                InscriptionActivite i = new InscriptionActivite(
                        rs.getInt("id"),
                        rs.getInt("activite_id"),
                        rs.getInt("personne_id"),
                        rs.getDate("dateInscription")
                );
                inscriptions.add(i);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return inscriptions;
    }

    public List<InscriptionActivite> getByActivite(int activiteId) {
        List<InscriptionActivite> inscriptions = new ArrayList<>();
        String SQL = "SELECT * FROM inscription_activite WHERE activite_id = " + activiteId;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                InscriptionActivite i = new InscriptionActivite(
                        rs.getInt("id"),
                        rs.getInt("activite_id"),
                        rs.getInt("personne_id"),
                        rs.getDate("dateInscription")
                );
                inscriptions.add(i);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return inscriptions;
    }
}
