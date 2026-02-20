package services;

import models.Avis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AvisService implements IService<Avis> {
    private Connection conn;
    private ActiviteService activiteService;

    public AvisService() {
        this.conn = DBConnection.getInstance().getConn();
        this.activiteService = new ActiviteService();
    }

    @Override
    public void add(Avis avis) {
        String SQL = "INSERT INTO avis (note, commentaire, dateAvis, activite_id, personne_id) VALUES (" +
                avis.getNote() + ", '" +
                avis.getCommentaire() + "', '" +
                avis.getDateAvis() + "', " +
                avis.getActiviteId() + ", " +
                avis.getPersonneId() + ")";
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Review added successfully!");
            activiteService.updateNoteMoyenne(avis.getActiviteId());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Avis avis) {
        String SQL = "UPDATE avis SET note = ?, commentaire = ?, dateAvis = ?, activite_id = ?, personne_id = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, avis.getNote());
            pstmt.setString(2, avis.getCommentaire());
            pstmt.setDate(3, avis.getDateAvis());
            pstmt.setInt(4, avis.getActiviteId());
            pstmt.setInt(5, avis.getPersonneId());
            pstmt.setInt(6, avis.getId());
            pstmt.executeUpdate();
            System.out.println("Review updated successfully!");

            // Update average note of the activity
            ActiviteService activiteService = new ActiviteService();
            activiteService.updateNoteMoyenne(avis.getActiviteId());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    @Override
    public void delete(Avis avis) {
        String SQL = "DELETE FROM avis WHERE id = " + avis.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Review deleted successfully!");
            activiteService.updateNoteMoyenne(avis.getActiviteId());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Avis> getAll() {
        List<Avis> avisList = new ArrayList<>();
        String SQL = "SELECT * FROM avis";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Avis a = new Avis();
                a.setId(rs.getInt("id"));
                a.setNote(rs.getInt("note"));
                a.setCommentaire(rs.getString("commentaire"));
                a.setDateAvis(rs.getDate("dateAvis"));
                a.setActiviteId(rs.getInt("activite_id"));
                a.setPersonneId(rs.getInt("personne_id"));

                avisList.add(a);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return avisList;
    }

    public List<Avis> getAvisByActivite(int activiteId) {
        List<Avis> avisList = new ArrayList<>();
        String SQL = "SELECT * FROM avis WHERE activite_id = " + activiteId;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Avis a = new Avis();
                a.setId(rs.getInt("id"));
                a.setNote(rs.getInt("note"));
                a.setCommentaire(rs.getString("commentaire"));
                a.setDateAvis(rs.getDate("dateAvis"));
                a.setActiviteId(rs.getInt("activite_id"));
                a.setPersonneId(rs.getInt("personne_id"));

                avisList.add(a);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return avisList;
    }
}

