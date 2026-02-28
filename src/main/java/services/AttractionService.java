package services;

import models.Attraction;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttractionService {

    private final Connection conn;

    public AttractionService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    public List<Attraction> getAttractionsByVille(int villeId, int limit) {

        String sql = """
            SELECT id, nom, type, prix, heures_ouverture, description, ville_id
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
                            nz(rs.getString("type")),
                            rs.getDouble("prix"),
                            nz(rs.getString("heures_ouverture")),
                            nz(rs.getString("description")),
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
                    .append(" | Hours: ").append(safeShort(a.getHeuresOuverture(), 40))
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