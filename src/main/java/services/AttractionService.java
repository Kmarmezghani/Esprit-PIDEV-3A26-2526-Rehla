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
        if (conn == null) return;
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
        if (conn == null) return;
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
        if (conn == null) return;
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
        List<Attraction> attractions = new ArrayList<>();
        if (conn == null) return attractions;
        String SQL = "SELECT * FROM attraction";
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
        List<Attraction> attractions = new ArrayList<>();
        if (conn == null) return attractions;
        String SQL = "SELECT * FROM attraction WHERE ville_id = ?";
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

    public List<Attraction> getAttractionsByVille(int villeId, int limit) {

        String sql = """
SELECT id, nom, description, type, prix,
       heure_ouverture, heure_fermeture,
       est_ferme, ville_id
FROM attraction
WHERE ville_id = ?
ORDER BY nom ASC
LIMIT ?
""";

        List<Attraction> res = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, villeId);
            ps.setInt(2, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attraction a = new Attraction(
                            rs.getInt("id"),
                            nz(rs.getString("nom")),
                            nz(rs.getString("description")),
                            nz(rs.getString("type")),
                            rs.getDouble("prix"),
                            rs.getTime("heure_ouverture"),
                            rs.getTime("heure_fermeture"),
                            rs.getBoolean("est_ferme"),
                            rs.getInt("ville_id")
                    );
                    if (!a.getNom().isBlank()) res.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }

    // ✅ CE QUE TON CONTROLLER APPELLE
    public List<String> getAttractionNamesByVille(int villeId, int limit) {
        List<Attraction> list = getAttractionsByVille(villeId, limit);
        List<String> names = new ArrayList<>();
        for (Attraction a : list) {
            if (a != null && a.getNom() != null && !a.getNom().isBlank()) {
                names.add(a.getNom().trim());
            }
        }
        return names;
    }

    /** (Optionnel) contexte riche si un jour tu veux l’envoyer à l’IA */
    public String buildContext(List<Attraction> list) {
        if (list == null || list.isEmpty()) return "No attraction records available for this destination.";

        StringBuilder sb = new StringBuilder();
        for (Attraction a : list) {
            sb.append("- ")
                    .append(a.getNom())
                    .append(" | Type: ").append(safeShort(a.getType(), 40))
                    .append(" | Hours: ")
                    .append(safeShort(
                            a.getHeureOuverture() != null
                                    ? a.getHeureOuverture().toString()
                                    : "",
                            40))
                    .append(" | Note: ").append(safeShort(a.getDescription(), 140))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String nz(String s) { return s == null ? "" : s.trim(); }

    private String safeShort(String s, int max) {
        String t = nz(s);
        if (t.length() <= max) return t;
        return t.substring(0, max - 1) + "…";
    }
}
