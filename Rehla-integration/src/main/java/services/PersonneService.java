package services;

import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PersonneService {

    private final Connection conn = DBConnection.getInstance().getConn();

    public String getEmailById(int id) {
        String sql = "SELECT email FROM personne WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getFullNameById(int id) {
        String sql = "SELECT nom, prenom FROM personne WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    return (prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "User";
    }
}
