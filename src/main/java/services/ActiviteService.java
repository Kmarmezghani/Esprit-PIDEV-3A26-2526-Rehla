package services;

import models.Activite;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiviteService implements IService<Activite> {

    private final Connection conn;

    public ActiviteService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    // =========================
    // CRUD
    // =========================

    @Override
    public void add(Activite activite) {
        activite.setNoteMoyenne(0);

        String sql = """
            INSERT INTO activite
              (nom, description, prix, typeActivite, noteMoyenne,
               guide_id, destination_id, date_debut, date_fin, status, max_places, image,
               is_flash_sale, flash_price, flash_expires_at)
            VALUES (?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?)
        """;

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

            Integer mp = activite.getMaxPlaces();
            if (mp != null && mp > 0) ps.setInt(11, mp);
            else ps.setNull(11, Types.INTEGER);

            if (activite.getImage() != null && !activite.getImage().isBlank()) ps.setString(12, activite.getImage());
            else ps.setNull(12, Types.VARCHAR);

            // ✅ FLASH FIELDS
            ps.setInt(13, activite.isFlashSale() ? 1 : 0);

            if (activite.getFlashPrice() != null) ps.setDouble(14, activite.getFlashPrice());
            else ps.setNull(14, Types.DOUBLE);

            if (activite.getFlashExpiresAt() != null) ps.setTimestamp(15, Timestamp.valueOf(activite.getFlashExpiresAt()));
            else ps.setNull(15, Types.TIMESTAMP);

            ps.executeUpdate();
            System.out.println("Activite added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Activite activite) {
        String sql = """
            UPDATE activite SET
              nom=?,
              description=?,
              prix=?,
              typeActivite=?,
              noteMoyenne=?,
              guide_id=?,
              destination_id=?,
              date_debut=?,
              date_fin=?,
              status=?,
              max_places=?,
              image=?,
              is_flash_sale=?,
              flash_price=?,
              flash_expires_at=?
            WHERE id=?
        """;

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

            Integer mp = activite.getMaxPlaces();
            if (mp != null && mp > 0) ps.setInt(11, mp);
            else ps.setNull(11, Types.INTEGER);

            if (activite.getImage() != null && !activite.getImage().isBlank()) ps.setString(12, activite.getImage());
            else ps.setNull(12, Types.VARCHAR);

            // ✅ FLASH
            ps.setInt(13, activite.isFlashSale() ? 1 : 0);

            if (activite.getFlashPrice() != null) ps.setDouble(14, activite.getFlashPrice());
            else ps.setNull(14, Types.DOUBLE);

            if (activite.getFlashExpiresAt() != null) ps.setTimestamp(15, Timestamp.valueOf(activite.getFlashExpiresAt()));
            else ps.setNull(15, Types.TIMESTAMP);

            ps.setInt(16, activite.getId());

            ps.executeUpdate();
            revalidateFlashForActivity(activite.getId());
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
                activites.add(mapActivite(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return activites;
    }

    // =========================
    // Helpers
    // =========================

    private Activite mapActivite(ResultSet rs) throws SQLException {
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

        int mp = rs.getInt("max_places");
        a.setMaxPlaces(rs.wasNull() ? null : mp);

        a.setImage(rs.getString("image"));

        // ✅ FLASH FIELDS
        try {
            a.setFlashSale(rs.getInt("is_flash_sale") == 1);

            Double fp = (Double) rs.getObject("flash_price");
            a.setFlashPrice(fp);

            Timestamp fe = rs.getTimestamp("flash_expires_at");
            a.setFlashExpiresAt(fe == null ? null : fe.toLocalDateTime());
        } catch (SQLException ignored) {
            // au cas où la table n'a pas encore les colonnes (en dev)
        }

        return a;
    }

    // =========================
    // Extras (unchanged)
    // =========================

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
        String sql = """
            SELECT p.nom, p.prenom
            FROM guide g
            JOIN personne p ON g.id = p.id
            WHERE g.id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
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
            while (rs.next()) map.put(rs.getString("nom"), rs.getInt("id"));
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
                while (rs.next()) activites.add(mapActivite(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return activites;
    }

    public List<Activite> getDisponibles() {
        markExpiredActivitiesAsUnavailable();

        String sql = "SELECT * FROM activite WHERE status = 'DISPONIBLE'";
        List<Activite> activites = new ArrayList<>();

        try (Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {

            while (rs.next()) activites.add(mapActivite(rs));

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return activites;
    }

    public Activite getById(int id) {
        String sql = "SELECT * FROM activite WHERE id = ? LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapActivite(rs);
            }
        } catch (SQLException e) {
            System.out.println("getById error: " + e.getMessage());
            return null;
        }
    }

    public void markExpiredActivitiesAsUnavailable() {
        String sql = """
            UPDATE activite
            SET status = 'INDISPONIBLE'
            WHERE date_fin IS NOT NULL
              AND date_fin <= NOW()
              AND status <> 'INDISPONIBLE'
        """;

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public List<Activite> getFlashSales() {
        String sql = """
        SELECT *
        FROM activite
        WHERE status = 'DISPONIBLE'
          AND IFNULL(is_flash_sale,0) = 1
          AND flash_price IS NOT NULL
          AND flash_expires_at IS NOT NULL
          AND flash_expires_at > NOW()
        ORDER BY flash_expires_at ASC
    """;

        List<Activite> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapActivite(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
    public void refreshFlashSales() throws SQLException {

        // 1) Désactiver les flash expirées
        String expireSql = """
        UPDATE activite
        SET is_flash_sale = 0,
            flash_price = NULL,
            flash_expires_at = NULL
        WHERE IFNULL(is_flash_sale,0) = 1
          AND flash_expires_at IS NOT NULL
          AND flash_expires_at <= NOW()
    """;

        // 2) Activer automatiquement les flash sales selon tes règles
        // - DISPONIBLE
        // - date_debut dans 3 jours
        // - max_places NOT NULL
        // - remise 20% par défaut
        // - si booked < 50% ET noteMoyenne < 3.5 => remise 30%
        // - durée flash: 12h
        String activateSql = """
        UPDATE activite a
        LEFT JOIN (
            SELECT t.activite_id, COUNT(*) AS booked
            FROM ticket t
            JOIN reservation r
              ON r.id = t.reservation_id
             AND UPPER(IFNULL(r.statut,'')) = 'RESERVED'
            WHERE t.activite_id IS NOT NULL
            GROUP BY t.activite_id
        ) x ON x.activite_id = a.id
        SET a.is_flash_sale = 1,
            a.flash_expires_at = DATE_ADD(NOW(), INTERVAL 12 HOUR),
            a.flash_price = CASE
                WHEN IFNULL(x.booked,0) < (a.max_places * 0.5)
                 AND a.noteMoyenne < 3.5
                THEN ROUND(a.prix * 0.70, 2)  -- ✅ 30% remise
                ELSE ROUND(a.prix * 0.80, 2)  -- ✅ 20% remise
            END
        WHERE a.status = 'DISPONIBLE'
          AND (a.is_flash_sale = 0 OR a.is_flash_sale IS NULL)
          AND a.date_debut IS NOT NULL
          AND a.date_debut BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 3 DAY)
          AND a.max_places IS NOT NULL
          AND a.max_places > 0
          AND a.prix > 0
    """;

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(expireSql);
            st.executeUpdate(activateSql);
        }
    }
    public void revalidateFlashForActivity(int activiteId) throws SQLException {

        // 1) Si flash expiré ou champs incohérents => reset
        String resetSql = """
        UPDATE activite
        SET is_flash_sale = 0,
            flash_price = NULL,
            flash_expires_at = NULL
        WHERE id = ?
          AND (
                IFNULL(is_flash_sale,0) = 1
                AND (
                     flash_price IS NULL
                  OR flash_price <= 0
                  OR flash_expires_at IS NULL
                  OR flash_expires_at <= NOW()
                )
          )
    """;

        // 2) Si l’activité n’est plus éligible (ex: date_debut changée) => reset
        // ⚠️ adapte ici TES règles (j’ai mis les mêmes que refreshFlashSales)
        String notEligibleSql = """
        UPDATE activite
        SET is_flash_sale = 0,
            flash_price = NULL,
            flash_expires_at = NULL
        WHERE id = ?
          AND IFNULL(is_flash_sale,0) = 1
          AND (
                status <> 'DISPONIBLE'
             OR date_debut IS NULL
             OR date_debut NOT BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 3 DAY)
             OR max_places IS NULL
             OR noteMoyenne >= 3.5
          )
    """;

        try (PreparedStatement ps1 = conn.prepareStatement(resetSql);
             PreparedStatement ps2 = conn.prepareStatement(notEligibleSql)) {

            ps1.setInt(1, activiteId);
            ps1.executeUpdate();

            ps2.setInt(1, activiteId);
            ps2.executeUpdate();
        }
    }
}