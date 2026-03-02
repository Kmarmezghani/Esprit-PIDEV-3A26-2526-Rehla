package services;

import models.stats.UserReputationSummary;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Computes a simple dynamic reputation score for a user based on reservations
 * and reviews (avis).
 *
 * Example scoring:
 *   +10 per confirmed reservation (RESERVED/PAID)
 *   -15 per cancelled reservation
 *   +5  per review left
 *
 * Then mapped to:
 *   >= 40  -> "Utilisateur fiable"
 *   15–39  -> "Utilisateur standard"
 *   < 15   -> "Utilisateur à surveiller"
 */
public class ReputationService {

    private final Connection conn;

    public ReputationService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    public UserReputationSummary computeForUser(int personneId) {
        int totalReservations = 0;
        int confirmed = 0;
        int cancelled = 0;

        String sqlReservations = """
            SELECT COALESCE(UPPER(statut), '') AS st, COUNT(*) AS nb
            FROM reservation
            WHERE personne_id = ?
            GROUP BY COALESCE(UPPER(statut), '')
        """;

        try (PreparedStatement ps = conn.prepareStatement(sqlReservations)) {
            ps.setInt(1, personneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("st");
                    int nb = rs.getInt("nb");
                    totalReservations += nb;
                    if ("RESERVED".equals(st) || "PAID".equals(st)) {
                        confirmed += nb;
                    } else if ("CANCELLED".equals(st)) {
                        cancelled += nb;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int reviewsCount = countReviews(personneId);

        int score = confirmed * 10 + reviewsCount * 5 + cancelled * (-15);

        String label;
        String color;
        if (score >= 40) {
            label = "🟢 Utilisateur fiable";
            color = "#1BAA5A";
        } else if (score >= 15) {
            label = "🟡 Utilisateur standard";
            color = "#E2B93B";
        } else {
            label = "🔴 Utilisateur à surveiller";
            color = "#D64545";
        }

        return new UserReputationSummary(
                score,
                totalReservations,
                confirmed,
                cancelled,
                reviewsCount,
                label,
                color
        );
    }

    private int countReviews(int personneId) {
        String sql = "SELECT COUNT(*) FROM avis WHERE personne_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}