package services;

import models.Activite;
import models.Preference; // (pas utilisé ici, juste au cas)
import models.WaitlistEntry;
import util.DBConnection;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.Optional;

public class WaitlistService {

    private final Connection cnx;

    // Dépendances déjà existantes chez toi
    private final ActiviteService activiteService = new ActiviteService();
    private final ReservationService reservationService = new ReservationService();
    private final PersonneService personneService = new PersonneService();
    private final EmailService emailService = new EmailService();

    // HOLD duration
    private static final int HOLD_MINUTES = 20;

    public WaitlistService() {
        cnx = DBConnection.getInstance().getConn();
    }

    // =========================
    // 1) JOIN WAITLIST
    // =========================
    public void joinWaitlist(int personneId, int activiteId) throws SQLException {
        // éviter doublons (WAITING ou HOLD)
        String check = """
            SELECT id
            FROM waitlist
            WHERE activite_id = ? AND personne_id = ?
              AND status IN ('WAITING','HOLD')
            LIMIT 1
        """;

        try (PreparedStatement ps = cnx.prepareStatement(check)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // déjà dans waitlist
                return;
            }
        }

        String insert = """
            INSERT INTO waitlist (activite_id, personne_id, status, created_at)
            VALUES (?, ?, 'WAITING', NOW())
        """;
        try (PreparedStatement ps = cnx.prepareStatement(insert)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ps.executeUpdate();
        }
    }

    // =========================
    // 2) LEAVE WAITLIST
    // =========================
    public void leaveWaitlist(int personneId, int activiteId) throws SQLException {
        String sql = """
            UPDATE waitlist
            SET status = 'CANCELLED'
            WHERE activite_id = ? AND personne_id = ?
              AND status IN ('WAITING','HOLD')
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ps.executeUpdate();
        }
    }

    // =========================
    // 3) EXPIRE HOLDS (global)
    // =========================
    public int expireAllHolds() throws SQLException {
        String sql = """
            UPDATE waitlist
            SET status = 'EXPIRED', hold_token = NULL, hold_expires_at = NULL
            WHERE status = 'HOLD' AND hold_expires_at IS NOT NULL AND hold_expires_at < NOW()
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // =========================
    // 4) PROMOTE NEXT (if seat available)
    // Appelée après annulation / suppression ticket
    // =========================
    public void promoteNextIfSeatAvailable(int activiteId) throws SQLException {
        // 1) expirer les anciens HOLD pour éviter blocage
        expireAllHolds();

        // 2) si pas de place, stop
        if (!hasFreeSeat(activiteId)) return;

        // 3) prendre le premier WAITING
        Optional<WaitlistEntry> next = getNextWaiting(activiteId);
        if (next.isEmpty()) return;

        // 4) créer un HOLD + envoyer mail
        WaitlistEntry holdEntry = createHold(next.get().getId(), HOLD_MINUTES);

        // ✅✅✅ ONLY CHANGE: email template like your others (HTML + activity details)
        try {
            int userId = holdEntry.getPersonneId();
            String email = personneService.getEmailById(userId);
            String name  = personneService.getFullNameById(userId);

            if (email != null && !email.isBlank()) {

                // récupérer activité
                Activite a = activiteService.getById(activiteId);
                String activityName = (a != null && a.getNom() != null) ? a.getNom() : ("Activité #" + activiteId);

                // destination display (nom, pays)
                String destinationDisplay = "";
                if (a != null) {
                    destinationDisplay = activiteService.getDestinationDisplayById(a.getDestinationId());
                }

                // dates (string simple)
                String startDate = (a != null && a.getDateDebut() != null) ? a.getDateDebut().toString() : "—";
                String endDate   = (a != null && a.getDateFin() != null) ? a.getDateFin().toString() : "—";

                // si tu as une page confirm (sinon garde vide et le mail affichera juste le token)
                String confirmUrl = ""; // ex: "http://localhost:8080/waitlist/confirm?token=" + holdEntry.getHoldToken();

                emailService.sendWaitlistHoldEmail(
                        email,
                        name,
                        activityName,
                        destinationDisplay,
                        startDate,
                        endDate,
                        holdEntry.getHoldToken(),
                        HOLD_MINUTES,
                        confirmUrl
                );
            }
        } catch (Exception ignored) {
            // si l’email fail, on garde le hold (ou tu peux revert si tu veux)
        }
    }

    // =========================
    // 5) GET NEXT WAITING
    // =========================
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
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();
            return Optional.of(map(rs));
        }
    }

    // =========================
    // 6) CREATE HOLD
    // =========================
    public WaitlistEntry createHold(int waitlistId, int minutes) throws SQLException {
        // récupère l’entrée
        WaitlistEntry e = getById(waitlistId)
                .orElseThrow(() -> new SQLException("Waitlist entry not found: " + waitlistId));

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
            if (updated == 0) {
                throw new SQLException("Cannot create hold. Entry is not WAITING anymore.");
            }
        }

        // reload
        return getById(waitlistId)
                .orElseThrow(() -> new SQLException("Waitlist entry not found after hold: " + waitlistId));
    }

    // =========================
    // 7) CONFIRM HOLD
    // Token venant de l’email / UI
    // Crée réservation + tickets (qty=1 ici)
    // =========================
    public void confirmHold(String token) throws SQLException {
        if (token == null || token.isBlank()) throw new SQLException("Token is empty.");

        boolean oldAutoCommit = cnx.getAutoCommit();
        cnx.setAutoCommit(false);

        try {
            // 1) lock row
            String lockSql = """
                SELECT id, activite_id, personne_id, status, hold_expires_at
                FROM waitlist
                WHERE hold_token = ?
                LIMIT 1
                FOR UPDATE
            """;
            int waitlistId;
            int activiteId;
            int personneId;
            String status;
            Timestamp holdExp;

            try (PreparedStatement ps = cnx.prepareStatement(lockSql)) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new SQLException("Invalid token.");
                }
                waitlistId = rs.getInt("id");
                activiteId = rs.getInt("activite_id");
                personneId = rs.getInt("personne_id");
                status = rs.getString("status");
                holdExp = rs.getTimestamp("hold_expires_at");
            }

            if (!"HOLD".equalsIgnoreCase(status)) {
                throw new SQLException("This token is not in HOLD status.");
            }
            if (holdExp == null || holdExp.before(new Timestamp(System.currentTimeMillis()))) {
                // expire
                markExpired(waitlistId);
                cnx.commit();
                throw new SQLException("Hold expired.");
            }

            // 2) vérifier place dispo
            if (!hasFreeSeat(activiteId)) {
                // garder HOLD ? ici on expire pour éviter blocage
                markExpired(waitlistId);
                cnx.commit();
                throw new SQLException("No seat available anymore.");
            }

            // 3) créer réservation/ticket (1 place)
            Activite a = activiteService.getById(activiteId);
            if (a == null) throw new SQLException("Activite not found.");

            reservationService.bookWithQty(
                    personneId,
                    activiteId,
                    1,
                    a.getPrix(),
                    a.getDestinationId()
            );

            // 4) statut CONFIRMED
            String okSql = """
                UPDATE waitlist
                SET status='CONFIRMED', hold_token=NULL, hold_expires_at=NULL
                WHERE id = ?
            """;
            try (PreparedStatement ps = cnx.prepareStatement(okSql)) {
                ps.setInt(1, waitlistId);
                ps.executeUpdate();
            }

            cnx.commit();
        } catch (SQLException ex) {
            cnx.rollback();
            throw ex;
        } finally {
            cnx.setAutoCommit(oldAutoCommit);
        }
    }

    // =========================
    // Helpers DB
    // =========================
    public Optional<WaitlistEntry> getById(int id) throws SQLException {
        String sql = """
            SELECT id, activite_id, personne_id, status, created_at, hold_expires_at, hold_token
            FROM waitlist
            WHERE id = ?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();
            return Optional.of(map(rs));
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

    // =========================
    // Seats check
    // =========================
    private boolean hasFreeSeat(int activiteId) {
        try {
            Activite a = activiteService.getById(activiteId);
            if (a == null) return false;

            Integer max = a.getMaxPlaces();
            if (max == null) return true; // unlimited

            int booked = reservationService.sumTicketsConfirmedByActiviteId(activiteId);
            return booked < max;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // Token generator
    // =========================
    private String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // =========================
    // 0) CHECK IF USER IS WAITING/HOLD
    // =========================
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
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    // (optionnel mais utile) récupérer le statut exact
    public String getUserWaitlistStatus(int personneId, int activiteId) throws SQLException {
        String sql = """
            SELECT status
            FROM waitlist
            WHERE activite_id = ? AND personne_id = ?
            ORDER BY created_at DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ps.setInt(2, personneId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("status") : null;
        }
    }
}