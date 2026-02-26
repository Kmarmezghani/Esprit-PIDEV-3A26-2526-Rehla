package services;

import models.InscriptionActivite;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InscriptionActiviteService implements IService<InscriptionActivite> {

    private final Connection conn;

    public InscriptionActiviteService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    // ===============================
    // BOOK ACTIVITY (NEW METHOD)
    // ===============================
    public void book(int personneId, int activiteId, double total_price) throws SQLException {

        if (exists(personneId, activiteId)) {
            throw new SQLException("User already booked this activity.");
        }

        String sql = """
            INSERT INTO inscription_activite 
            (personne_id, activite_id, date_inscription, total_price, status)
            VALUES (?, ?, ?, ?, 'CONFIRMED')
        """;



        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personneId);
            ps.setInt(2, activiteId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(4, total_price);
            ps.executeUpdate();
        }
    }

    // ===============================
    // CHECK IF EXISTS
    // ===============================
    public boolean exists(int personneId, int activiteId) throws SQLException {
        String sql = "SELECT 1 FROM inscription_activite WHERE personne_id=? AND activite_id=? LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personneId);
            ps.setInt(2, activiteId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }
    public int countConfirmedByActiviteId(int activiteId) {
        String sql = "SELECT COUNT(*) FROM inscription_activite WHERE activite_id = ? AND status = 'CONFIRMED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    // ===============================
    // BASIC CRUD (unchanged)
    // ===============================

    @Override
    public void add(InscriptionActivite inscription) {
        // plus utilisé pour booking
    }

    @Override
    public void update(InscriptionActivite inscription) {}

    @Override
    public void delete(InscriptionActivite inscription) {}

    @Override
    public List<InscriptionActivite> getAll() {
        return new ArrayList<>();
    }
}
