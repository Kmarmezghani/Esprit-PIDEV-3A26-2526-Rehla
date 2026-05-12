package services;

import models.Ville;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VilleService implements IService<Ville> {
    Connection conn;

    public VilleService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Ville ville) {
        if (conn == null) return;
        String SQL = "INSERT INTO ville (nom, pays_id, visit_count, latitude, longitude, typeTourisme, saison) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, ville.getNom());
            pstmt.setInt(2, ville.getPaysId());
            pstmt.setInt(3, ville.getVisitCount());
            pstmt.setDouble(4, ville.getLatitude());
            pstmt.setDouble(5, ville.getLongitude());
            pstmt.setString(6, ville.getTypeTourisme());
            pstmt.setString(7, ville.getSaison());
            pstmt.executeUpdate();
            System.out.println("Ville added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Ville ville) {
        if (conn == null) return;
        String SQL = "UPDATE ville SET nom = ?, pays_id = ?, visit_count = ?, latitude = ?, longitude = ?, typeTourisme = ?, saison = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, ville.getNom());
            pstmt.setInt(2, ville.getPaysId());
            pstmt.setInt(3, ville.getVisitCount());
            pstmt.setDouble(4, ville.getLatitude());
            pstmt.setDouble(5, ville.getLongitude());
            pstmt.setString(6, ville.getTypeTourisme());
            pstmt.setString(7, ville.getSaison());
            pstmt.setInt(8, ville.getId());
            pstmt.executeUpdate();
            System.out.println("Ville updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Ville ville) {
        if (conn == null) return;
        String SQL = "DELETE FROM ville WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, ville.getId());
            pstmt.executeUpdate();
            System.out.println("Ville deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Ville> getAll() {
        List<Ville> villes = new ArrayList<>();
        if (conn == null) return villes;
        String SQL = "SELECT * FROM ville";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Ville ville = new Ville();
                ville.setId(rs.getInt("id"));
                ville.setNom(rs.getString("nom"));
                ville.setPaysId(rs.getInt("pays_id"));
                ville.setVisitCount(rs.getInt("visit_count"));
                ville.setLatitude(rs.getDouble("latitude"));
                ville.setLongitude(rs.getDouble("longitude"));
                ville.setTypeTourisme(rs.getString("typeTourisme"));
                ville.setSaison(rs.getString("saison"));
                // Debug: Check what database actually returns
                String imageValue = rs.getString("image");
                System.out.println("DEBUG: Database ville.id=" + rs.getInt("id") + " image='" + imageValue + "'");
                
                ville.setImage(imageValue);
                villes.add(ville);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return villes;
    }

    public List<Ville> getByPaysId(int paysId) {
        List<Ville> villes = new ArrayList<>();
        if (conn == null) return villes;
        String SQL = "SELECT * FROM ville WHERE pays_id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, paysId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Ville ville = new Ville();
                ville.setId(rs.getInt("id"));
                ville.setNom(rs.getString("nom"));
                ville.setPaysId(rs.getInt("pays_id"));
                ville.setVisitCount(rs.getInt("visit_count"));
                ville.setLatitude(rs.getDouble("latitude"));
                ville.setLongitude(rs.getDouble("longitude"));
                ville.setTypeTourisme(rs.getString("typeTourisme"));
                ville.setSaison(rs.getString("saison"));
                // Debug: Check what database actually returns
                String imageValue = rs.getString("image");
                System.out.println("DEBUG: Database ville.id=" + rs.getInt("id") + " image='" + imageValue + "'");
                
                ville.setImage(imageValue);
                villes.add(ville);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return villes;
    }
    public void incrementVisitCount(int villeId) {
        if (conn == null) return;
        String SQL = "UPDATE ville SET visit_count = visit_count + 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, villeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
