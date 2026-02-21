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
    // CRUD
    // =========================

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
        String sql = """
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
            ps.setInt(9, reservation.getId());

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
        String sql = "SELECT nom FROM destination WHERE id = ?";
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

    // =========================
    // Availability check (NOW from TICKET, not reservation.activite_id)
    // =========================

    public int sumTicketsConfirmedByActiviteId(int activiteId) throws SQLException {
        // ⚠️ Ici on suppose que le "volume" est dans reservation.nb_tickets
        // donc on somme reservation.nb_tickets pour les reservations qui contiennent cette activité dans ticket.
        String sql = """
            SELECT COALESCE(SUM(r.nb_tickets), 0) AS taken
            FROM ticket t
            JOIN reservation r ON r.id = t.reservation_id
            WHERE t.activite_id = ?
              AND r.statut = 'reserved'
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("taken") : 0;
        }
    }

    // =========================
    // Booking (transaction)
    // =========================

    public void bookWithQty(int userId, int activiteId, int qty, double prixUnitaire, Integer destinationId) throws SQLException {
        try {
            conn.setAutoCommit(false);

            // 1) Lock activity + read max_places + dates
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

            // 2) Taken spots (based on tickets+reservations)
            int taken = sumTicketsConfirmedByActiviteId(activiteId);

            // 3) Availability check
            if (maxPlaces != null) {
                int available = Math.max(0, maxPlaces - taken);
                if (qty > available) {
                    throw new SQLException("Not enough spots. Only " + available + " left.");
                }
            }

            // 4) Insert reservation (NO activite_id here)
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

            // 5) Insert ticket line that links reservation <-> activite
            ticketService.addActivityTicket(conn, reservationId, activiteId);

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