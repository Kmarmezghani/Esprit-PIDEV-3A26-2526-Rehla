package services;

import models.Reservation;
import models.stats.CarbonFootprintSummary;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Estimates CO2 footprint for trips based on reservations, destination continent
 * and activity tickets. This is an approximation meant for awareness, not for
 * official reporting.
 */
public class CarbonFootprintService {

    private final Connection conn;
    private final ReservationService reservationService;

    public CarbonFootprintService() {
        this.conn = DBConnection.getInstance().getConn();
        this.reservationService = new ReservationService();
    }

    /**
     * Estimate CO2 (in kg CO2e) for a single reservation.
     */
    public double estimateReservationKg(Reservation reservation) {
        if (reservation == null) return 0;
        int reservationId = reservation.getId();
        int travellers = Math.max(reservation.getNbTickets(), 1);

        String continent = getContinentForReservation(reservationId);
        double basePerTraveller = baseFlightKgForContinent(continent);
        double baseFlightKg = basePerTraveller * travellers;

        int activityCount = countReservedActivities(reservationId);
        double activitiesKg = activityCount * 5.0; // 5 kg per activity ticket

        double priceKg = Math.max(0, reservation.getCoutTotal()) * 0.2; // 0.2 kg per currency unit

        return baseFlightKg + activitiesKg + priceKg;
    }

    /**
     * Aggregate footprint for all confirmed (reserved/paid) reservations of a user.
     */
    public CarbonFootprintSummary getFootprintForUser(int personneId) {
        List<Reservation> reservations = reservationService.getConfirmedReservationsForUser(personneId);
        if (reservations.isEmpty()) {
            return new CarbonFootprintSummary(0, 0, 0);
        }
        double total = 0;
        for (Reservation r : reservations) {
            total += estimateReservationKg(r);
        }
        double avg = total / reservations.size();
        return new CarbonFootprintSummary(total, avg, reservations.size());
    }

    // =========================
    // Helpers
    // =========================

    private String getContinentForReservation(int reservationId) {
        String sql = """
            SELECT p.continent
            FROM reservation r
            LEFT JOIN pays p ON r.destination_id = p.id
            WHERE r.id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String c = rs.getString(1);
                    return c != null ? c : "";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int countReservedActivities(int reservationId) {
        String sql = """
            SELECT COUNT(*)
            FROM ticket t
            WHERE t.reservation_id = ?
              AND t.activite_id IS NOT NULL
              AND UPPER(IFNULL(t.statut,'')) IN ('RESERVED','PAID')
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double baseFlightKgForContinent(String continent) {
        if (continent == null) return 220;
        String c = continent.trim().toLowerCase();
        if (c.contains("europe") && !c.contains("asie")) return 250;
        if (c.contains("afrique")) return 180;
        if (c.contains("asie") && c.contains("europe")) return 400;
        if (c.contains("asie")) return 600;
        if (c.contains("amérique du nord") || c.contains("amerique du nord")) return 700;
        if (c.contains("amérique du sud") || c.contains("amerique du sud")) return 800;
        return 220;
    }
}

