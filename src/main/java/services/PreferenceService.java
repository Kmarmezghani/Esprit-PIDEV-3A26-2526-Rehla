package services;

import models.Preference;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PreferenceService implements IService<Preference> {

    private final Connection conn;

    public PreferenceService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Preference pref) {
        if (conn == null) throw new IllegalStateException("DB not connected");
        String sql = "INSERT INTO `preference` (`budgetMin`, `budgetMax`, `typesVoyage`, `centresInteret`, `personne_id`) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, pref.getBudgetMin());
            ps.setObject(2, pref.getBudgetMax());
            ps.setString(3, pref.getTypesVoyage());
            ps.setString(4, pref.getCentresInteret());
            ps.setInt(5, pref.getPersonneId());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) pref.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService add failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not save preferences: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Preference pref) {
        if (conn == null) throw new IllegalStateException("DB not connected");
        String sql = "UPDATE `preference` SET `budgetMin`=?, `budgetMax`=?, `typesVoyage`=?, `centresInteret`=? WHERE `id`=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, pref.getBudgetMin());
            ps.setObject(2, pref.getBudgetMax());
            ps.setString(3, pref.getTypesVoyage());
            ps.setString(4, pref.getCentresInteret());
            ps.setInt(5, pref.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService update failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not update preferences: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Preference pref) {
        String sql = "DELETE FROM `preference` WHERE `id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pref.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService delete: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Delete all preferences for a user (e.g. before deleting the user). */
    public void deleteByPersonneId(int personneId) {
        String sql = "DELETE FROM `preference` WHERE `personne_id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService deleteByPersonneId: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Preference> getAll() {
        List<Preference> list = new ArrayList<>();
        String sql = "SELECT * FROM `preference`";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService getAll: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /** Get preferences for one user (personne_id). Returns first match or null if none. */
    public Preference getByPersonneId(int personneId) {
        String sql = "SELECT * FROM `preference` WHERE `personne_id` = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personneId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService getByPersonneId: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Preference getById(int id) {
        String sql = "SELECT * FROM `preference` WHERE `id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DB] PreferenceService getById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Preference mapRow(ResultSet rs) throws SQLException {
        Preference p = new Preference();
        p.setId(rs.getInt("id"));
        p.setBudgetMin(rs.getObject("budgetMin") != null ? rs.getDouble("budgetMin") : null);
        p.setBudgetMax(rs.getObject("budgetMax") != null ? rs.getDouble("budgetMax") : null);
        p.setTypesVoyage(rs.getString("typesVoyage"));
        p.setCentresInteret(rs.getString("centresInteret"));
        p.setPersonneId(rs.getInt("personne_id"));
        return p;
    }
}
