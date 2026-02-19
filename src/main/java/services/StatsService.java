package services;

import models.stats.AvgNoteRow;
import models.stats.NoteDistributionRow;
import models.stats.TopActiviteRow;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatsService {

    private final Connection cnx;

    public StatsService() {
        cnx = DBConnection.getInstance().getConn();
    }

    // Distribution des notes
    public List<NoteDistributionRow> getNoteDistribution() throws SQLException {
        String sql = """
            SELECT note, COUNT(*) AS nb
            FROM avis
            GROUP BY note
            ORDER BY note ASC
        """;

        List<NoteDistributionRow> res = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                res.add(new NoteDistributionRow(
                        rs.getInt("note"),
                        rs.getInt("nb")
                ));
            }
        }
        return res;
    }

    // Moyenne + nb avis par activité
    public List<AvgNoteRow> getAvgNotesByActivity() throws SQLException {
        String sql = """
            SELECT a.id, a.nom,
                   IFNULL(AVG(av.note), 0) AS avgNote,
                   COUNT(av.id) AS nbAvis
            FROM activite a
            LEFT JOIN avis av ON av.activite_id = a.id
            GROUP BY a.id, a.nom
            ORDER BY avgNote DESC
        """;

        List<AvgNoteRow> res = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                res.add(new AvgNoteRow(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDouble("avgNote"),
                        rs.getInt("nbAvis")
                ));
            }
        }
        return res;
    }
    public List<TopActiviteRow> getTopActivitiesByInscriptions(int limit) throws SQLException {

        String sql = """
        SELECT a.id, a.nom, COUNT(i.id) AS nbInscriptions
        FROM activite a
        LEFT JOIN inscription_activite i ON i.activite_id = a.id
        GROUP BY a.id, a.nom
        ORDER BY nbInscriptions DESC
        LIMIT ?
    """;

        List<TopActiviteRow> res = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new TopActiviteRow(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nbInscriptions")
                ));
            }
        }
        return res;
    }
}
