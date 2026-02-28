package services;

import models.Reservation;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService implements IService<Reservation> {

    private final Connection conn;
    private final TicketService ticketService = new TicketService();

    public ReservationService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Reservation reservation) {
        String sql = """
            INSERT INTO reservation
            (dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id, nb_tickets)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, reservation.getDateReservation());
            ps.setDate(2, reservation.getDateDebut());
            ps.setDate(3, reservation.getDateFin());
            ps.setString(4, reservation.getStatut());
            ps.setDouble(5, reservation.getCoutTotal());
            ps.setInt(6, reservation.getPersonneId());

            if (reservation.getDestinationId() == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, reservation.getDestinationId());

            ps.setInt(8, reservation.getNbTickets());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Reservation reservation) {

        String updateSql = """
            UPDATE reservation SET
              dateReservation = ?,
              dateDebut = ?,
              dateFin = ?,
              statut = ?,
              coutTotal = ?,
              personne_id = ?,
              destination_id = ?,
              nb_tickets = ?
            WHERE id = ?
        """;

        boolean wantCancelled = "CANCELLED".equalsIgnoreCase(reservation.getStatut());
        boolean wasCancelledBefore = false;
        List<Integer> activiteIdsToPromote = new ArrayList<>();

        try {
            conn.setAutoCommit(false);

            wasCancelledBefore = isReservationAlreadyCancelled(conn, reservation.getId());

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDate(1, reservation.getDateReservation());
                ps.setDate(2, reservation.getDateDebut());
                ps.setDate(3, reservation.getDateFin());
                ps.setString(4, reservation.getStatut());
                ps.setDouble(5, reservation.getCoutTotal());
                ps.setInt(6, reservation.getPersonneId());

                if (reservation.getDestinationId() == null) ps.setNull(7, Types.INTEGER);
                else ps.setInt(7, reservation.getDestinationId());

                ps.setInt(8, reservation.getNbTickets());
                ps.setInt(9, reservation.getId());
                ps.executeUpdate();
            }

            if (wantCancelled && !wasCancelledBefore) {

                activiteIdsToPromote = getActivityIdsFromReservationForWaitlist(conn, reservation.getId());

                try (PreparedStatement ps2 = conn.prepareStatement("""
                    UPDATE ticket
                    SET statut = 'CANCELLED'
                    WHERE reservation_id = ?
                      AND UPPER(IFNULL(type,'')) IN ('ACTIVITY','ACTIVITE')
                """)) {
                    ps2.setInt(1, reservation.getId());
                    ps2.executeUpdate();
                }
            }

            conn.commit();

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            System.out.println(e.getMessage());
            return;

        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }

        if (wantCancelled && !wasCancelledBefore && activiteIdsToPromote != null && !activiteIdsToPromote.isEmpty()) {
            List<Integer> finalIds = new ArrayList<>(activiteIdsToPromote);
            new Thread(() -> {
                WaitlistService waitlistService = new WaitlistService();
                for (Integer activiteId : finalIds) {
                    if (activiteId == null) continue;
                    try {
                        waitlistService.promoteNextIfSeatAvailable(activiteId);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void delete(Reservation reservation) {

        List<Integer> activiteIdsToPromote = new ArrayList<>();

        try {
            conn.setAutoCommit(false);

            activiteIdsToPromote = getActivityIdsFromReservationForWaitlist(conn, reservation.getId());

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ticket WHERE reservation_id = ?")) {
                ps.setInt(1, reservation.getId());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reservation WHERE id = ?")) {
                ps.setInt(1, reservation.getId());
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            System.out.println(e.getMessage());
            return;

        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }

        if (activiteIdsToPromote != null && !activiteIdsToPromote.isEmpty()) {
            List<Integer> finalIds = new ArrayList<>(activiteIdsToPromote);
            new Thread(() -> {
                WaitlistService waitlistService = new WaitlistService();
                for (Integer activiteId : finalIds) {
                    if (activiteId == null) continue;
                    try {
                        waitlistService.promoteNextIfSeatAvailable(activiteId);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private boolean isReservationAlreadyCancelled(Connection c, int reservationId) throws SQLException {
        String sql = "SELECT statut FROM reservation WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String st = rs.getString(1);
                return "CANCELLED".equalsIgnoreCase(st);
            }
        }
    }

    private List<Integer> getActivityIdsFromReservationForWaitlist(Connection c, int reservationId) throws SQLException {
        String sql = """
            SELECT DISTINCT t.activite_id
            FROM ticket t
            WHERE t.reservation_id = ?
              AND t.activite_id IS NOT NULL
              AND UPPER(IFNULL(t.type,'')) IN ('ACTIVITY','ACTIVITE')
        """;
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    if (!rs.wasNull()) ids.add(id);
                }
            }
        }
        return ids;
    }

    // =========================
    // Getters
    // =========================

    public List<Reservation> getAll() {
        String sql = "SELECT * FROM reservation";
        ArrayList<Reservation> list = new ArrayList<>();

        try (Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public Reservation getById(int id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDestinationNomById(int id) {
        String sql = "SELECT nom FROM ville WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nom");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();

        r.setId(rs.getInt("id"));
        r.setDateReservation(rs.getDate("dateReservation"));
        r.setDateDebut(rs.getDate("dateDebut"));
        r.setDateFin(rs.getDate("dateFin"));
        r.setStatut(rs.getString("statut"));
        r.setCoutTotal(rs.getDouble("coutTotal"));
        r.setPersonneId(rs.getInt("personne_id"));

        int dest = rs.getInt("destination_id");
        r.setDestinationId(rs.wasNull() ? null : dest);

        r.setNbTickets(rs.getInt("nb_tickets"));
        return r;
    }

    public int sumTicketsConfirmedByActiviteId(int activiteId) throws SQLException {
        String sql = """
            SELECT COALESCE(COUNT(*),0) AS taken
            FROM ticket t
            JOIN reservation r ON r.id = t.reservation_id
            WHERE t.activite_id = ?
              AND UPPER(IFNULL(r.statut,'')) = 'RESERVED'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("taken") : 0;
        }
    }

    // =========================================================
    // ✅ BOOKING - version existante (inchangée au niveau "API")
    // =========================================================
    public void bookWithQty(int userId, int activiteId, int qty, double prixUnitaire, Integer destinationId) throws SQLException {
        // On garde la signature/usage existant
        bookWithQtyInternal(userId, activiteId, qty, prixUnitaire, destinationId, false);
    }

    // =========================================================
    // ✅ AJOUT : retourner l'ID de la réservation créée
    // =========================================================
    public int bookWithQtyReturnReservationId(int userId, int activiteId, int qty, double prixUnitaire, Integer destinationId) throws SQLException {
        return bookWithQtyInternal(userId, activiteId, qty, prixUnitaire, destinationId, true);
    }

    // =========================================================
    // ✅ AJOUT : retourner l'objet Reservation (parfait pour Email PDF)
    // =========================================================
    public Reservation bookWithQtyReturnReservation(int userId, int activiteId, int qty, double prixUnitaire, Integer destinationId) throws SQLException {
        int id = bookWithQtyInternal(userId, activiteId, qty, prixUnitaire, destinationId, true);
        return getById(id);
    }

    /**
     * Core booking : fait le lock, check seats, insert reservation, insert tickets, commit.
     * @return reservationId si returnId=true, sinon -1
     */
    private int bookWithQtyInternal(int userId,
                                    int activiteId,
                                    int qty,
                                    double prixUnitaire,
                                    Integer destinationId,
                                    boolean returnId) throws SQLException {

        try {
            conn.setAutoCommit(false);

            Integer maxPlaces;
            Date actStartDate;
            Date actEndDate;

            // Lock activity row
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT max_places, date_debut, date_fin FROM activite WHERE id = ? FOR UPDATE")) {

                ps.setInt(1, activiteId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new SQLException("Activity not found.");

                int mp = rs.getInt("max_places");
                maxPlaces = rs.wasNull() ? null : mp;

                actStartDate = rs.getDate("date_debut");
                actEndDate = rs.getDate("date_fin");

                if (actStartDate == null || actEndDate == null) {
                    throw new SQLException("Activity dates are missing (date_debut/date_fin).");
                }
            }

            int taken = sumTicketsConfirmedByActiviteId(activiteId);

            if (maxPlaces != null) {
                int available = Math.max(0, maxPlaces - taken);
                if (qty > available) throw new SQLException("Not enough spots. Only " + available + " left.");
            }

            double total = prixUnitaire * qty;
            int reservationId;

            String insertRes = """
                INSERT INTO reservation(dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id, nb_tickets)
                VALUES (?, ?, ?, 'reserved', ?, ?, ?, ?)
            """;

            try (PreparedStatement ps = conn.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ps.setDate(2, actStartDate);
                ps.setDate(3, actEndDate);
                ps.setDouble(4, total);
                ps.setInt(5, userId);

                if (destinationId == null) ps.setNull(6, Types.INTEGER);
                else ps.setInt(6, destinationId);

                ps.setInt(7, qty);

                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Failed to create reservation.");
                reservationId = keys.getInt(1);
            }

            ticketService.createTicketsBatch(conn, reservationId, activiteId, qty, prixUnitaire, destinationId);

            conn.commit();

            return returnId ? reservationId : -1;

        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public int getLastInsertedId() {
        String sql = "SELECT MAX(id) FROM reservation";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void updateReservationStats(int reservationId) {

        String countSql = "SELECT COUNT(*), COALESCE(SUM(prix),0) FROM ticket WHERE reservation_id=?";
        String updateSql = "UPDATE reservation SET nb_tickets=?, coutTotal=? WHERE id=?";

        try (
                PreparedStatement countStmt = conn.prepareStatement(countSql);
                PreparedStatement updateStmt = conn.prepareStatement(updateSql)
        ) {

            countStmt.setInt(1, reservationId);
            ResultSet rs = countStmt.executeQuery();

            if (rs.next()) {

                int nbTickets = rs.getInt(1);
                double total = rs.getDouble(2);

                updateStmt.setInt(1, nbTickets);
                updateStmt.setDouble(2, total);
                updateStmt.setInt(3, reservationId);

                updateStmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markAsPaid(int reservationId) {

        String sql = "UPDATE reservation SET statut = 'PAID' WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addActivityTicketsToExistingReservation(
            int reservationId,
            int userId,
            int activiteId,
            int qty,
            double prixUnitaire,
            Integer destinationId,
            LocalDate selectedDate
    ) throws SQLException {

        if (qty <= 0) throw new SQLException("Quantité invalide.");

        try {
            conn.setAutoCommit(false);

            // 1) Vérifier réservation + dates + ownership
            Date resStart;
            Date resEnd;

            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT dateDebut, dateFin, statut
                FROM reservation
                WHERE id = ? AND personne_id = ?
                FOR UPDATE
        """)) {
                ps.setInt(1, reservationId);
                ps.setInt(2, userId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) throw new SQLException("Réservation introuvable.");
                String statut = rs.getString("statut");
                if (statut != null && statut.equalsIgnoreCase("CANCELLED"))
                    throw new SQLException("Impossible d'ajouter sur une réservation annulée.");

                resStart = rs.getDate("dateDebut");
                resEnd = rs.getDate("dateFin");
                if (resStart == null || resEnd == null)
                    throw new SQLException("Dates réservation manquantes.");
            }

            LocalDate rStart = resStart.toLocalDate();
            LocalDate rEnd = resEnd.toLocalDate();

            if (selectedDate == null) throw new SQLException("selectedDate manquante.");
            if (selectedDate.isBefore(rStart) || selectedDate.isAfter(rEnd)) {
                throw new SQLException("Date sélectionnée hors intervalle de la réservation.");
            }

            // 2) Vérifier activité + dates + max places
            Integer maxPlaces;
            Date actStartDate;
            Date actEndDate;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT max_places, date_debut, date_fin FROM activite WHERE id = ? FOR UPDATE")) {

                ps.setInt(1, activiteId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new SQLException("Activity not found.");

                int mp = rs.getInt("max_places");
                maxPlaces = rs.wasNull() ? null : mp;

                actStartDate = rs.getDate("date_debut");
                actEndDate = rs.getDate("date_fin");

                if (actStartDate == null || actEndDate == null) {
                    throw new SQLException("Activity dates are missing (date_debut/date_fin).");
                }
            }

            LocalDate aStart = actStartDate.toLocalDate();
            LocalDate aEnd = actEndDate.toLocalDate();

            if (selectedDate.isBefore(aStart) || selectedDate.isAfter(aEnd)) {
                throw new SQLException("Cette activité n'est pas disponible pour la date sélectionnée.");
            }

            // 3) Check seats
            int taken = sumTicketsConfirmedByActiviteId(activiteId);
            if (maxPlaces != null) {
                int available = Math.max(0, maxPlaces - taken);
                if (qty > available) throw new SQLException("Not enough spots. Only " + available + " left.");
            }

            // 4) Créer tickets (batch)
            ticketService.createTicketsBatch(conn, reservationId, activiteId, qty, prixUnitaire, destinationId);

            // 5) Mettre à jour nb_tickets + coutTotal
            updateReservationStats(reservationId);

            conn.commit();

        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}