package services;

import models.Activite;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiviteService implements IService<Activite> {

    private final Connection conn;

    public ActiviteService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Activite activite) {
        activite.setNoteMoyenne(0);

        String sql = "INSERT INTO activite (nom, description, prix, typeActivite, noteMoyenne, guide_id, destination_id, date_debut, date_fin, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, activite.getNom());
            ps.setString(2, activite.getDescription());
            ps.setDouble(3, activite.getPrix());
            ps.setString(4, activite.getTypeActivite());
            ps.setDouble(5, activite.getNoteMoyenne());

            if (activite.getGuideId() > 0) ps.setInt(6, activite.getGuideId());
            else ps.setNull(6, Types.INTEGER);

            if (activite.getDestinationId() > 0) ps.setInt(7, activite.getDestinationId());
            else ps.setNull(7, Types.INTEGER);

            if (activite.getDateDebut() != null) ps.setTimestamp(8, Timestamp.valueOf(activite.getDateDebut()));
            else ps.setNull(8, Types.TIMESTAMP);

            if (activite.getDateFin() != null) ps.setTimestamp(9, Timestamp.valueOf(activite.getDateFin()));
            else ps.setNull(9, Types.TIMESTAMP);

            if (activite.getStatus() != null) ps.setString(10, activite.getStatus());
            else ps.setNull(10, Types.VARCHAR);

            ps.executeUpdate();
            System.out.println("Activite added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Activite activite) {
        String sql = "UPDATE activite SET nom=?, description=?, prix=?, typeActivite=?, noteMoyenne=?, guide_id=?, destination_id=?, date_debut=?, date_fin=?, status=? " +
                "WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, activite.getNom());
            ps.setString(2, activite.getDescription());
            ps.setDouble(3, activite.getPrix());
            ps.setString(4, activite.getTypeActivite());
            ps.setDouble(5, activite.getNoteMoyenne());

            if (activite.getGuideId() > 0) ps.setInt(6, activite.getGuideId());
            else ps.setNull(6, Types.INTEGER);

            if (activite.getDestinationId() > 0) ps.setInt(7, activite.getDestinationId());
            else ps.setNull(7, Types.INTEGER);

            if (activite.getDateDebut() != null) ps.setTimestamp(8, Timestamp.valueOf(activite.getDateDebut()));
            else ps.setNull(8, Types.TIMESTAMP);

            if (activite.getDateFin() != null) ps.setTimestamp(9, Timestamp.valueOf(activite.getDateFin()));
            else ps.setNull(9, Types.TIMESTAMP);

            if (activite.getStatus() != null) ps.setString(10, activite.getStatus());
            else ps.setNull(10, Types.VARCHAR);

            ps.setInt(11, activite.getId());

            ps.executeUpdate();
            System.out.println("Activite updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Activite activite) {
        String sql = "DELETE FROM activite WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activite.getId());
            ps.executeUpdate();
            System.out.println("Activite deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Activite> getAll() {
        String sql = "SELECT * FROM activite";
        List<Activite> activites = new ArrayList<>();

        try (Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {

            while (rs.next()) {
                Activite a = new Activite();
                a.setId(rs.getInt("id"));
                a.setNom(rs.getString("nom"));
                a.setDescription(rs.getString("description"));
                a.setPrix(rs.getDouble("prix"));
                a.setTypeActivite(rs.getString("typeActivite"));
                a.setNoteMoyenne(rs.getDouble("noteMoyenne"));

                int gid = rs.getInt("guide_id");
                a.setGuideId(rs.wasNull() ? 0 : gid);

                int did = rs.getInt("destination_id");
                a.setDestinationId(rs.wasNull() ? 0 : did);

                Timestamp td = rs.getTimestamp("date_debut");
                a.setDateDebut(td != null ? td.toLocalDateTime() : null);

                Timestamp tf = rs.getTimestamp("date_fin");
                a.setDateFin(tf != null ? tf.toLocalDateTime() : null);

                a.setStatus(rs.getString("status"));

                activites.add(a);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return activites;
    }

    public void updateNoteMoyenne(int activiteId) {
        String sql = "UPDATE activite SET noteMoyenne = (" +
                "SELECT IFNULL(AVG(note), 0) FROM avis WHERE activite_id = ?" +
                ") WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, activiteId);
            ps.executeUpdate();
            System.out.println("Average note updated for activity " + activiteId);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getGuideNameByActiviteId(int guideId) {
        String nomComplet = "N/A";
        String sql = "SELECT p.nom, p.prenom " +
                "FROM guide g " +
                "JOIN personne p ON g.id = p.id " +
                "WHERE g.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nomComplet;
    }
    public Map<String, Integer> getDestinationsMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT id, nom FROM destination ORDER BY nom";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("nom"), rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public String getDestinationNameById(int destinationId) {
        if (destinationId <= 0) return "";
        String sql = "SELECT nom FROM destination WHERE id = " + destinationId;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getString("nom");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getDestinationDisplayById(int destinationId) {
        if (destinationId <= 0) return "";
        String sql = "SELECT CONCAT(nom, ', ', pays) AS display FROM destination WHERE id = " + destinationId;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getString("display");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    public List<Activite> getByGuideId(int guideId) {

        String sql = "SELECT * FROM activite WHERE guide_id = ?";
        List<Activite> activites = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, guideId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Activite a = new Activite();
                    a.setId(rs.getInt("id"));
                    a.setNom(rs.getString("nom"));
                    a.setDescription(rs.getString("description"));
                    a.setPrix(rs.getDouble("prix"));
                    a.setTypeActivite(rs.getString("typeActivite"));
                    a.setNoteMoyenne(rs.getDouble("noteMoyenne"));

                    int gid = rs.getInt("guide_id");
                    a.setGuideId(rs.wasNull() ? 0 : gid);

                    int did = rs.getInt("destination_id");
                    a.setDestinationId(rs.wasNull() ? 0 : did);

                    Timestamp td = rs.getTimestamp("date_debut");
                    a.setDateDebut(td != null ? td.toLocalDateTime() : null);

                    Timestamp tf = rs.getTimestamp("date_fin");
                    a.setDateFin(tf != null ? tf.toLocalDateTime() : null);

                    a.setStatus(rs.getString("status"));

                    activites.add(a);
                }
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return activites;
    }



}
