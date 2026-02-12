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
        String SQL = "INSERT INTO ville (nom, pays_id, region, typeTourisme, saison, popularite) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, ville.getNom());
            pstmt.setInt(2, ville.getPaysId());
            pstmt.setString(3, ville.getRegion());
            pstmt.setString(4, ville.getTypeTourisme());
            pstmt.setString(5, ville.getSaison());
            pstmt.setInt(6, ville.getPopularite());
            pstmt.executeUpdate();
            System.out.println("Ville added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Ville ville) {
        String SQL = "UPDATE ville SET nom = ?, pays_id = ?, region = ?, typeTourisme = ?, saison = ?, popularite = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, ville.getNom());
            pstmt.setInt(2, ville.getPaysId());
            pstmt.setString(3, ville.getRegion());
            pstmt.setString(4, ville.getTypeTourisme());
            pstmt.setString(5, ville.getSaison());
            pstmt.setInt(6, ville.getPopularite());
            pstmt.setInt(7, ville.getId());
            pstmt.executeUpdate();
            System.out.println("Ville updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Ville ville) {
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
        String SQL = "SELECT * FROM ville";
        List<Ville> villes = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Ville ville = new Ville();
                ville.setId(rs.getInt("id"));
                ville.setNom(rs.getString("nom"));
                ville.setPaysId(rs.getInt("pays_id"));
                ville.setRegion(rs.getString("region"));
                ville.setTypeTourisme(rs.getString("typeTourisme"));
                ville.setSaison(rs.getString("saison"));
                ville.setPopularite(rs.getInt("popularite"));
                villes.add(ville);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return villes;
    }

    public List<Ville> getByPaysId(int paysId) {
        String SQL = "SELECT * FROM ville WHERE pays_id = ?";
        List<Ville> villes = new ArrayList<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setInt(1, paysId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Ville ville = new Ville();
                ville.setId(rs.getInt("id"));
                ville.setNom(rs.getString("nom"));
                ville.setPaysId(rs.getInt("pays_id"));
                ville.setRegion(rs.getString("region"));
                ville.setTypeTourisme(rs.getString("typeTourisme"));
                ville.setSaison(rs.getString("saison"));
                ville.setPopularite(rs.getInt("popularite"));
                villes.add(ville);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return villes;
    }
}
