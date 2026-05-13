
package services;

import models.Personne;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PersonneService implements IService<Personne> {

    public PersonneService() {}

    /** Always returns the live connection — auto-reconnects if stale. */
    private Connection conn() {
        return DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Personne p) {
        String sql = "INSERT INTO `personne` (`nom`, `prenom`, `email`, `motDePasse`, `dateInscription`, `role`, `statutCompte`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getMotDePasse());
            // DB column dateInscription is DATE (not DATETIME)
            LocalDateTime dt = p.getDateInscription() != null ? p.getDateInscription() : LocalDateTime.now();
            ps.setDate(5, java.sql.Date.valueOf(dt.toLocalDate()));
            ps.setString(6, toDbRole(p.getRole()));
            ps.setString(7, toDbStatut(p.getStatutCompte()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) p.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService add failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not save user: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Personne p) {
        String sql = """
        UPDATE `personne` 
        SET `nom` = ?, 
            `prenom` = ?, 
            `email` = ?, 
            `motDePasse` = ?, 
            `role` = ?, 
            `statutCompte` = ?, 
            `telephone` = ?, 
            `heureNotif` = ?, 
            `notifSmsActive` = ?, 
            `profile_photo` = ? 
        WHERE `id` = ?
    """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getMotDePasse());
            ps.setString(5, toDbRole(p.getRole()));               // CLIENT, GUIDE, ADMIN
            ps.setString(6, toDbStatut(p.getStatutCompte()));     // ACTIF, INACTIF, SUSPENDU
            ps.setString(7, p.getTelephone());
            ps.setTime(8, p.getHeureNotif() != null ? java.sql.Time.valueOf(p.getHeureNotif()) : java.sql.Time.valueOf("08:00:00"));
            ps.setBoolean(9, p.getNotifSmsActive() != null ? p.getNotifSmsActive() : true); // par défaut activé
            ps.setString(10, p.getProfilePhoto());
            ps.setInt(11, p.getId());

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Aucune personne trouvée avec l'id " + p.getId());
            }

        } catch (SQLException e) {
            System.err.println("[DB] PersonneService update failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not update user: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Personne p) {
        String sql = "DELETE FROM `personne` WHERE `id` = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService delete: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Personne> getAll() {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM `personne`";
        try (Statement st = conn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService getAll: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /** Find user by email for login. */
    public Personne findByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT * FROM `personne` WHERE `email` = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService findByEmail: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Personne getById(int id) {
        String sql = "SELECT * FROM `personne` WHERE `id` = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService getById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Personne mapRow(ResultSet rs) throws SQLException {
        Personne p = new Personne();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setMotDePasse(rs.getString("motDePasse"));
        Timestamp ts = rs.getTimestamp("dateInscription");
        p.setDateInscription(ts != null ? ts.toLocalDateTime() : null);
        try {
            Timestamp last = rs.getTimestamp("last_login_at");
            if (last != null) p.setLastLoginAt(last.toLocalDateTime());
        } catch (SQLException ignored) {}
        p.setRole(fromDbRole(rs.getString("role")));
        p.setStatutCompte(fromDbStatut(rs.getString("statutCompte")));
        try { p.setTelephone(rs.getString("telephone")); } catch (SQLException ignored) {}
        try {
            Time t = rs.getTime("heureNotif");
            if (t != null) p.setHeureNotif(t.toLocalTime());
        } catch (SQLException ignored) {}
        try { p.setNotifSmsActive(rs.getBoolean("notifSmsActive")); } catch (SQLException ignored) {}
        try { p.setProfilePhoto(rs.getString("profile_photo")); } catch (SQLException ignored) {}
        return p;
    }

    /** DB column `role` is ENUM('CLIENT','GUIDE','ADMIN') — must send uppercase. */
    private static String toDbRole(String role) {
        if (role == null || role.isBlank()) return "CLIENT";
        String r = role.trim().toLowerCase();
        if (r.startsWith("admin")) return "ADMIN";
        if (r.startsWith("guide")) return "GUIDE";
        return "CLIENT";
    }

    private static String fromDbRole(String db) {
        if (db == null || db.isBlank()) return "user";
        String d = db.trim().toUpperCase();
        if ("ADMIN".equals(d)) return "admin";
        if ("GUIDE".equals(d)) return "guide";
        return "user";
    }

    /** DB column `statutCompte` is ENUM('ACTIF','INACTIF','SUSPENDU') — must send uppercase. */
    private static String toDbStatut(String statut) {
        if (statut == null || statut.isBlank()) return "ACTIF";
        String s = statut.trim().toUpperCase();
        if (s.startsWith("INACT")) return "INACTIF";
        if (s.startsWith("SUSP")) return "SUSPENDU";
        return "ACTIF";
    }

    private static String fromDbStatut(String db) {
        if (db == null || db.isBlank()) return "actif";
        String d = db.trim().toUpperCase();
        if ("INACTIF".equals(d)) return "inactif";
        if ("SUSPENDU".equals(d)) return "suspendu";
        return "actif";
    }

    public Personne findByRole(String role) {

        String sql = "SELECT * FROM `personne` WHERE `role` = ? LIMIT 1";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setString(1, toDbRole(role)); // utilise ta méthode existante 🔥

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {
            System.err.println("[DB] PersonneService findByRole: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Récupérer toutes les personnes avec notifications SMS activées
    public List<Personne> findAllWithSmsActive() {
        List<Personne> users = new ArrayList<>();

        String sql = "SELECT id, nom, prenom, email, telephone, heureNotif, role, statutCompte " +
                "FROM personne " +
                "WHERE telephone IS NOT NULL AND heureNotif IS NOT NULL";

        try (PreparedStatement ps = this.conn().prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Personne p = new Personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setTelephone(rs.getString("telephone"));

                Time t = rs.getTime("heureNotif");
                if(t != null) {
                    p.setHeureNotif(t.toLocalTime());
                }

                p.setRole(rs.getString("role"));
                p.setStatutCompte(rs.getString("statutCompte"));

                users.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
    public Personne getByEmail(String email) {
        String sql = "SELECT * FROM personne WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Personne p = new Personne();

                    // ⚠️ adapte les setters/colonnes selon ton modèle
                    p.setId(rs.getInt("id"));
                    p.setEmail(rs.getString("email"));
                    p.setNom(rs.getString("nom"));
                    p.setPrenom(rs.getString("prenom"));

                    p.setStatutCompte(rs.getString("statutCompte")); // si existe

                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getEmailById(int id) {
        String sql = "SELECT email FROM personne WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getFullNameById(int id) {
        String sql = "SELECT nom, prenom FROM personne WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    return (prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "User";
    }
    public boolean existsById(int id) throws SQLException {
        String sql = "SELECT 1 FROM personne WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Update last_login_at when a user successfully logs in.
     */
    public void updateLastLogin(int userId, LocalDateTime when) {
        String sql = "UPDATE personne SET last_login_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setTimestamp(1, when != null ? Timestamp.valueOf(when) : null);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService updateLastLogin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set accounts to INACTIF when they have not logged in for the given number of days.
     * If last_login_at is null we fall back to dateInscription.
     *
     * @return number of rows updated
     */
    public int deactivateInactiveAccounts(int inactivityDays) {
        if (inactivityDays <= 0) return 0;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(inactivityDays);

        String sql = """
            UPDATE personne
            SET statutCompte = 'INACTIF'
            WHERE statutCompte = 'ACTIF'
              AND (
                    (last_login_at IS NOT NULL AND last_login_at < ?)
                 OR (last_login_at IS NULL AND dateInscription < ?)
              )
        """;

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            ps.setTimestamp(2, Timestamp.valueOf(cutoff));
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] PersonneService deactivateInactiveAccounts: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}
