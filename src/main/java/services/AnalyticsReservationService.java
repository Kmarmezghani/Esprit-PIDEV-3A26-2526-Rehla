package services;

import util.DBConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsReservationService {

    private Connection conn = DBConnection.getInstance().getConn();

    // 1️⃣ TOTAL REVENUE
    public double getTotalRevenue() {
        double total = 0;
        String sql = "SELECT SUM(coutTotal) FROM reservation WHERE statut = 'PAID'";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                total = rs.getDouble(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return total;
    }

    // 2️⃣ REVENUE PER MONTH
    public Map<Integer, Double> getRevenuePerMonth() {

        Map<Integer, Double> map = new HashMap<>();

        String sql = """
                SELECT MONTH(dateReservation) as month,
                       SUM(coutTotal) as revenue
                FROM reservation
                WHERE statut = 'PAID'
                GROUP BY MONTH(dateReservation)
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int month = rs.getInt("month");
                double revenue = rs.getDouble("revenue");
                map.put(month, revenue);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }

    // 3️⃣ MOST RESERVED DESTINATION
    public String getMostReservedDestination() {

        String sql = """
                SELECT d.nom, COUNT(r.id) as total
                FROM reservation r
                JOIN ville d ON r.destination_id = d.id
                GROUP BY r.destination_id
                ORDER BY total DESC
                LIMIT 1
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString("nom");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "No Data";
    }

    // 4️⃣ CONVERSION RATE
    public double getConversionRate() {

        String sql = """
                SELECT 
                (SELECT COUNT(*) FROM reservation WHERE statut='PAID') /
                (SELECT COUNT(*) FROM reservation) * 100
                AS conversionRate
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("conversionRate");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int getTotalUsers() {
        return count("SELECT COUNT(*) FROM personne");
    }

    public int getTotalPosts() {
        return count("SELECT COUNT(*) FROM post");
    }

    public int getTotalComments() {
        return count("SELECT COUNT(*) FROM commentaire");
    }

    public int getTotalReservations() {
        return count("SELECT COUNT(*) FROM reservation");
    }

    private int count(String sql) {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}