package services;

import models.Attraction;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttractionService implements IService<Attraction> {
    Connection conn;

    public AttractionService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Attraction attraction) {
        String SQL = "INSERT INTO attraction (nom, description, type, prix, heure_ouverture, heure_fermeture, est_ferme, ville_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, attraction.getNom());
            pstmt.setString(2, attraction.getDescription());
            pstmt.setString(3, attraction.getType());
            pstmt.setDouble(4, attraction.getPrix());
            pstmt.setTime(5, attraction.getHeureOuverture());
            pstmt.setTime(6, attraction.getHeureFermeture());
            pstmt.setBoolean(7, attraction.isEstFerme());
            pstmt.setInt(8, attraction.getVilleId());
            pstmt.executeUpdate();
            System.out.println("Attraction added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Attraction attraction) {
        String SQL = "UPDATE attraction SET nom = ?, description = ?, type = ?, prix = ?, heure_ouverture = ?, heure_fermeture = ?, est_ferme = ?, ville_id = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, attraction.getNom());
            pstmt.setString(2, attraction.getDescription());
            pstmt.setString(3, attraction.getType());
            pstmt.setDouble(4, attraction.getPrix());
            pstmt.setTime(5, attraction.getHeureOuverture());
            pstmt.setTime(6, attraction.getHeureFermeture());
            pstmt.setBoolean(7, attraction.isEstFerme());
            pstmt.setInt(8, attraction.getVilleId());
            pstmt.setInt(9, attraction.getId());
            pstmt.executeUpdate();
            System.out.println("Attraction updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Attraction attraction) {
        String SQL = "DELETE FROM attraction WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, attraction.getId());
            pstmt.executeUpdate();
            System.out.println("Attraction deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Attraction> getAll() {
        String SQL = "SELECT * FROM attraction";
        List<Attraction> attractions = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Attraction attraction = new Attraction();
                attraction.setId(rs.getInt("id"));
                attraction.setNom(rs.getString("nom"));
                attraction.setDescription(rs.getString("description"));
                attraction.setType(rs.getString("type"));
                attraction.setPrix(rs.getDouble("prix"));
                attraction.setHeureOuverture(rs.getTime("heure_ouverture"));
                attraction.setHeureFermeture(rs.getTime("heure_fermeture"));
                attraction.setEstFerme(rs.getBoolean("est_ferme"));
                attraction.setVilleId(rs.getInt("ville_id"));
                attractions.add(attraction);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return attractions;
    }

    public List<Attraction> getByVilleId(int villeId) {
        String SQL = "SELECT * FROM attraction WHERE ville_id = ?";
        List<Attraction> attractions = new ArrayList<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, villeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Attraction attraction = new Attraction();
                attraction.setId(rs.getInt("id"));
                attraction.setNom(rs.getString("nom"));
                attraction.setDescription(rs.getString("description"));
                attraction.setType(rs.getString("type"));
                attraction.setPrix(rs.getDouble("prix"));
                attraction.setHeureOuverture(rs.getTime("heure_ouverture"));
                attraction.setHeureFermeture(rs.getTime("heure_fermeture"));
                attraction.setEstFerme(rs.getBoolean("est_ferme"));
                attraction.setVilleId(rs.getInt("ville_id"));
                attractions.add(attraction);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return attractions;
    }
}
