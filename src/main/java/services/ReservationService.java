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

    // =========================
    // CRUD (fixed with PreparedStatement)
    // =========================

    @Override
    public void add(Reservation reservation) {
        String sql = """
            INSERT INTO reservation
            (dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id, activite_id, nb_tickets)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            if (reservation.getActiviteId() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, reservation.getActiviteId());

            ps.setInt(9, reservation.getNbTickets());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservation SET
              dateReservation = ?,
              dateDebut = ?,
              dateFin = ?,
              statut = ?,
              coutTotal = ?,
              personne_id = ?,
              destination_id = ?,
              activite_id = ?,
              nb_tickets = ?
            WHERE id = ?
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

            if (reservation.getActiviteId() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, reservation.getActiviteId());

            ps.setInt(9, reservation.getNbTickets());
            ps.setInt(10, reservation.getId());

            ps.executeUpdate();
            System.out.println("Reservation updated successfully!");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Reservation reservation) {
        String sql = "DELETE FROM reservation WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservation.getId());
            ps.executeUpdate();
            System.out.println("Reservation deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

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
    public String getDestinationNomById(int id) {
        String nom = "";
        String sql = "SELECT nom FROM destination WHERE id = " + id;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                nom = rs.getString("nom");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nom;
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

        int act = rs.getInt("activite_id");
        r.setActiviteId(rs.wasNull() ? null : act);

        r.setNbTickets(rs.getInt("nb_tickets"));

        return r;
    }


    public int sumTicketsConfirmedByActiviteId(int activiteId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(nb_tickets),0) AS taken
            FROM reservation
            WHERE activite_id = ?
            AND statut = 'reserved'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("taken") : 0;
        }
    }


    public void bookWithQty(int userId, int activiteId, int qty, double prixUnitaire, Integer destinationId) throws SQLException {

        try {
            conn.setAutoCommit(false);

            // 1) lock activity row + get max_places + get dates (date_debut/date_fin)
            Integer maxPlaces = null;
            Date actStartDate;
            Date actEndDate;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT max_places, date_debut, date_fin FROM activite WHERE id = ? FOR UPDATE")) {

                ps.setInt(1, activiteId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) throw new SQLException("Activity not found.");

                int mp = rs.getInt("max_places");
                maxPlaces = rs.wasNull() ? null : mp;

                // activite.date_debut/date_fin are DATETIME in DB -> we take DATE part
                actStartDate = rs.getDate("date_debut");
                actEndDate = rs.getDate("date_fin");

                if (actStartDate == null || actEndDate == null) {
                    throw new SQLException("Activity dates are missing (date_debut/date_fin).");
                }
            }

            // 2) sum taken tickets inside transaction
            int taken = 0;
            try (PreparedStatement ps = conn.prepareStatement("""
            SELECT COALESCE(SUM(nb_tickets),0)
            FROM reservation
            WHERE activite_id = ?
              AND statut IN ('CONFIRMED','CONFIRMEE','PAYEE','APPROUVEE')
            FOR UPDATE
        """)) {
                ps.setInt(1, activiteId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) taken = rs.getInt(1);
            }

            // 3) check availability
            if (maxPlaces != null) {
                int available = Math.max(0, maxPlaces - taken);
                if (qty > available) {
                    throw new SQLException("Not enough spots. Only " + available + " left.");
                }
            }

            // 4) insert reservation (dateDebut/dateFin = activity dates)
            double total = prixUnitaire * qty;
            int reservationId;

            String insertRes = """
            INSERT INTO reservation(dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id, activite_id, nb_tickets)
            VALUES (?, ?, ?, 'reserved', ?, ?, ?, ?, ?)
        """;

            try (PreparedStatement ps = conn.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ps.setDate(2, actStartDate);
                ps.setDate(3, actEndDate);
                ps.setDouble(4, total);
                ps.setInt(5, userId);

                if (destinationId == null) ps.setNull(6, Types.INTEGER);
                else ps.setInt(6, destinationId);

                ps.setInt(7, activiteId);
                ps.setInt(8, qty);

                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Failed to create reservation.");
                reservationId = keys.getInt(1);
            }

            // 5) insert N tickets (same transaction)
            ticketService.createTicketsBatch(conn, reservationId, qty, prixUnitaire, destinationId);

            conn.commit();

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
}