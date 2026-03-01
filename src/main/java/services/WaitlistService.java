package services;

import models.Activite;
import models.WaitlistEntry;
import util.DBConnection;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

public class WaitlistService {

    private final Connection cnx;

    private final ActiviteService activiteService = new ActiviteService();
    private final PersonneService personneService = new PersonneService();
    private final EmailService emailService = new EmailService();
    private final NotificationService notificationService = new NotificationService();
    private final TicketService ticketService = new TicketService();

    private static final int HOLD_MINUTES = 20;

    public WaitlistService() {
        cnx = DBConnection.getInstance().getConn();
    }

    // ✅ IMPORTANT: pour ON DUPLICATE KEY, il faut un UNIQUE(activite_id, personne_id)
    public void joinWaitlist(int personneId, int activiteId) throws SQLException {
        String sql = """
            INSERT INTO waitlist (activite_id, personne_id, status, created_at, hold_token, hold_expires_at)
            VALUES (?, ?, 'WAITING', NOW(), NULL, NULL)
            ON DUPLICATE KEY UPDATE
                status = IF(status IN ('WAITING','HOLD'), status, 'WAITING'),
                created_at = IF(status IN ('WAITING','HOLD'), created_at, NOW()),
                hold_token = IF(status IN ('WAITING','HOLD'), hold_token, NULL),
                hold_expires_at = IF(status IN ('WAITING','HOLD'), hold_expires_at, NULL)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ps.executeUpdate();
        }
    }

    public void leaveWaitlist(int personneId, int activiteId) throws SQLException {
        String sql = """
            UPDATE waitlist
            SET status = 'CANCELLED', hold_token=NULL, hold_expires_at=NULL
            WHERE activite_id = ? AND personne_id = ?
              AND status IN ('WAITING','HOLD')
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ps.executeUpdate();
        }
    }

    public int expireAllHolds() throws SQLException {
        String sql = """
            UPDATE waitlist
            SET status = 'EXPIRED', hold_token = NULL, hold_expires_at = NULL
            WHERE status = 'HOLD'
              AND hold_expires_at IS NOT NULL
              AND hold_expires_at < NOW()
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    public void promoteNextIfSeatAvailable(int activiteId) throws SQLException {
        expireAllHolds();
        if (!hasFreeSeat(activiteId)) return;

        Optional<WaitlistEntry> next = getNextWaiting(activiteId);
        if (next.isEmpty()) return;

        WaitlistEntry holdEntry = createHold(next.get().getId(), HOLD_MINUTES);

        // notif
        // notif (✅ corrigé)
        try {
            int userId = holdEntry.getPersonneId();
            Activite a = activiteService.getById(activiteId);

            String activityName = (a != null && a.getNom() != null)
                    ? a.getNom()
                    : ("Activité #" + activiteId);

            Integer senderId = null;

            // senderId = guideId seulement si valide
            if (a != null && a.getGuideId() > 0) {
                int gid = a.getGuideId();
                if (personneService.existsById(gid)) senderId = gid; // sinon reste NULL
            }

            int rows = notificationService.createWaitlistHoldNotif(
                    senderId,     // ✅ NULL autorisé
                    userId,
                    activiteId,
                    "Une place s’est libérée pour \"" + activityName + "\". Réserve dans " + HOLD_MINUTES + " minutes."
            );

            System.out.println("[Waitlist] notif inserted rows=" + rows);

        } catch (Exception ex) {
            System.err.println("[Waitlist] Failed to create WAITLIST_HOLD notification: " + ex.getMessage());
            ex.printStackTrace();
        }
        // email (optionnel)
        try {
            int userId = holdEntry.getPersonneId();
            String email = personneService.getEmailById(userId);
            String name = personneService.getFullNameById(userId);

            if (email != null && !email.isBlank()) {
                Activite a = activiteService.getById(activiteId);
                String activityName = (a != null && a.getNom() != null) ? a.getNom() : ("Activité #" + activiteId);

                String destinationDisplay = (a != null) ? activiteService.getDestinationDisplayById(a.getDestinationId()) : "";
                String startDate = (a != null && a.getDateDebut() != null) ? a.getDateDebut().toString() : "—";
                String endDate   = (a != null && a.getDateFin() != null) ? a.getDateFin().toString() : "—";

                emailService.sendWaitlistHoldEmail(
                        email, name, activityName, destinationDisplay, startDate, endDate,
                        holdEntry.getHoldToken(), HOLD_MINUTES, ""
                );
            }
        } catch (Exception ignored) {}
    }

    public Optional<WaitlistEntry> getNextWaiting(int activiteId) throws SQLException {
        String sql = """
            SELECT id, activite_id, personne_id, status, created_at, hold_expires_at, hold_token
            FROM waitlist
            WHERE activite_id = ? AND status = 'WAITING'
            ORDER BY created_at ASC
            LIMIT 1
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        }
    }

    public WaitlistEntry createHold(int waitlistId, int minutes) throws SQLException {
        String token = generateToken();
        String sql = """
            UPDATE waitlist
            SET status = 'HOLD',
                hold_token = ?,
                hold_expires_at = DATE_ADD(NOW(), INTERVAL ? MINUTE)
            WHERE id = ? AND status = 'WAITING'
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, minutes);
            ps.setInt(3, waitlistId);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Cannot create hold. Entry is not WAITING anymore.");
        }
        return getById(waitlistId).orElseThrow(() -> new SQLException("Waitlist entry not found after hold."));
    }

    // ✅✅✅ CONFIRM SANS TOKEN (ta demande)
    public void confirmHold(int personneId, int activiteId) throws SQLException {

        boolean oldAuto = cnx.getAutoCommit();
        cnx.setAutoCommit(false);

        try {
            // 1) lock waitlist row
            String lockWait = """
                SELECT id, hold_expires_at, status
                FROM waitlist
                WHERE activite_id = ?
                  AND personne_id = ?
                  AND status = 'HOLD'
                LIMIT 1
                FOR UPDATE
            """;

            int waitId;
            Timestamp holdExp;
            String status;

            try (PreparedStatement ps = cnx.prepareStatement(lockWait)) {
                ps.setInt(1, activiteId);
                ps.setInt(2, personneId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Aucun HOLD actif (expiré ou déjà utilisé).");
                    waitId = rs.getInt("id");
                    holdExp = rs.getTimestamp("hold_expires_at");
                    status = rs.getString("status");
                }
            }

            if (!"HOLD".equalsIgnoreCase(status)) throw new SQLException("HOLD invalide.");
            if (holdExp == null || holdExp.before(new Timestamp(System.currentTimeMillis()))) {
                markExpired(waitId);
                cnx.commit();
                throw new SQLException("HOLD expiré.");
            }

            // 2) lock activity row
            Integer maxPlaces;
            Date actStart;
            Date actEnd;
            double prix;
            Integer destinationId;

            String lockAct = "SELECT max_places, date_debut, date_fin, prix, destination_id FROM activite WHERE id=? FOR UPDATE";
            try (PreparedStatement ps = cnx.prepareStatement(lockAct)) {
                ps.setInt(1, activiteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Activité introuvable.");
                    int mp = rs.getInt("max_places");
                    maxPlaces = rs.wasNull() ? null : mp;

                    actStart = rs.getDate("date_debut");
                    actEnd   = rs.getDate("date_fin");
                    prix     = rs.getDouble("prix");

                    int dest = rs.getInt("destination_id");
                    destinationId = rs.wasNull() ? null : dest;
                }
            }

            if (actStart == null || actEnd == null) throw new SQLException("Dates activité manquantes.");

            // 3) check seats (sur la même connexion)
            int taken = countReservedTicketsForActivity(activiteId);
            if (maxPlaces != null && taken >= maxPlaces) {
                markExpired(waitId);
                cnx.commit();
                throw new SQLException("Plus de place disponible.");
            }

            // 4) create reservation
            int reservationId;
            String insertRes = """
                INSERT INTO reservation(dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id, nb_tickets)
                VALUES (?, ?, ?, 'reserved', ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = cnx.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ps.setDate(2, actStart);
                ps.setDate(3, actEnd);
                ps.setDouble(4, prix); // qty=1
                ps.setInt(5, personneId);
                if (destinationId == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, destinationId);
                ps.setInt(7, 1);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Création réservation échouée.");
                    reservationId = keys.getInt(1);
                }
            }

            // 5) create ticket (qty=1) - même transaction
            ticketService.createTicketsBatch(cnx, reservationId, activiteId, 1, prix, destinationId);

            // 6) confirm waitlist
            String okSql = """
                UPDATE waitlist
                SET status='CONFIRMED', hold_token=NULL, hold_expires_at=NULL
                WHERE id = ?
            """;
            try (PreparedStatement ps = cnx.prepareStatement(okSql)) {
                ps.setInt(1, waitId);
                ps.executeUpdate();
            }

            cnx.commit();

        } catch (SQLException ex) {
            // ✅ rollback safe
            try {
                if (!cnx.getAutoCommit()) cnx.rollback();
            } catch (SQLException ignored) {}
            throw ex;
        } finally {
            try { cnx.setAutoCommit(oldAuto); } catch (SQLException ignored) {}
        }
    }

    // ---- helpers ----

    private int countReservedTicketsForActivity(int activiteId) throws SQLException {
        String sql = """
            SELECT COALESCE(COUNT(*),0) AS taken
            FROM ticket t
            JOIN reservation r ON r.id = t.reservation_id
            WHERE t.activite_id = ?
              AND LOWER(IFNULL(t.type,'')) IN ('activity','activite')
              AND LOWER(IFNULL(r.statut,'')) = 'reserved'
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("taken") : 0;
            }
        }
    }

    public Optional<WaitlistEntry> getById(int id) throws SQLException {
        String sql = """
            SELECT id, activite_id, personne_id, status, created_at, hold_expires_at, hold_token
            FROM waitlist
            WHERE id = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        }
    }

    public boolean isUserWaiting(int personneId, int activiteId) throws SQLException {
        String sql = """
            SELECT 1
            FROM waitlist
            WHERE activite_id = ? AND personne_id = ?
              AND status IN ('WAITING','HOLD')
            LIMIT 1
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markExpired(int id) throws SQLException {
        String sql = """
            UPDATE waitlist
            SET status='EXPIRED', hold_token=NULL, hold_expires_at=NULL
            WHERE id = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private WaitlistEntry map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp holdExp = rs.getTimestamp("hold_expires_at");

        return new WaitlistEntry(
                rs.getInt("id"),
                rs.getInt("activite_id"),
                rs.getInt("personne_id"),
                rs.getString("status"),
                created == null ? null : created.toLocalDateTime(),
                holdExp == null ? null : holdExp.toLocalDateTime(),
                rs.getString("hold_token")
        );
    }

    private boolean hasFreeSeat(int activiteId) {
        try {
            Activite a = activiteService.getById(activiteId);
            if (a == null) return false;
            Integer max = a.getMaxPlaces();
            if (max == null) return true;
            int booked = countReservedTicketsForActivity(activiteId);
            return booked < max;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}