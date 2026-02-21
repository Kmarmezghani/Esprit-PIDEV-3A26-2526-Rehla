package services;

import models.Ticket;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketService implements IService<Ticket> {
    private final Connection conn;

    public TicketService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Ticket ticket) {

        String sql = """
            INSERT INTO ticket
            (reservation_id, activite_id, destination_id, type, prix, statut, dateDebut, dateFin)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // reservation_id nullable
            if (ticket.getReservationId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, ticket.getReservationId());

            // ✅ activite_id (required usually)
            if (ticket.getActiviteId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, ticket.getActiviteId());

            ps.setInt(3, ticket.getDestinationId());
            ps.setString(4, ticket.getType());
            ps.setDouble(5, ticket.getPrix());
            ps.setString(6, ticket.getStatut());
            ps.setDate(7, ticket.getDateDebut());
            ps.setDate(8, ticket.getDateFin());

            ps.executeUpdate();
            System.out.println("Ticket added successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Ticket ticket) {

        String sql = """
            UPDATE ticket SET
              reservation_id = ?,
              activite_id = ?,
              destination_id = ?,
              type = ?,
              prix = ?,
              statut = ?,
              dateDebut = ?,
              dateFin = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (ticket.getReservationId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, ticket.getReservationId());

            // ✅ activite_id
            if (ticket.getActiviteId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, ticket.getActiviteId());

            ps.setInt(3, ticket.getDestinationId());
            ps.setString(4, ticket.getType());
            ps.setDouble(5, ticket.getPrix());
            ps.setString(6, ticket.getStatut());
            ps.setDate(7, ticket.getDateDebut());
            ps.setDate(8, ticket.getDateFin());
            ps.setInt(9, ticket.getId());

            ps.executeUpdate();
            System.out.println("Ticket updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Ticket ticket) {
        String sql = "DELETE FROM ticket WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticket.getId());
            ps.executeUpdate();
            System.out.println("Ticket deleted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Ticket> getAll() {
        List<Ticket> tickets = new ArrayList<>();

        String sql = """
            SELECT t.*, d.nom AS destination_nom
            FROM ticket t
            LEFT JOIN destination d ON t.destination_id = d.id
        """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                Ticket t = new Ticket(
                        rs.getInt("id"),
                        rs.getObject("reservation_id") != null ? rs.getInt("reservation_id") : null,
                        rs.getObject("activite_id") != null ? rs.getInt("activite_id") : null, // ✅ NEW
                        rs.getString("type"),
                        rs.getDouble("prix"),
                        rs.getString("statut"),
                        rs.getDate("dateDebut"),
                        rs.getDate("dateFin"),
                        rs.getInt("destination_id")
                );

                t.setDestinationNom(rs.getString("destination_nom"));
                tickets.add(t);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return tickets;
    }

    public double sumPrixByReservation(int reservationId) {
        String sql = "SELECT COALESCE(SUM(prix),0) FROM ticket WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public List<Ticket> getTicketsByReservation(int reservationId) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket WHERE reservation_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Ticket t = new Ticket();
                t.setId(rs.getInt("id"));
                t.setReservationId(rs.getObject("reservation_id") != null ? rs.getInt("reservation_id") : null);
                t.setActiviteId(rs.getObject("activite_id") != null ? rs.getInt("activite_id") : null); // ✅ NEW
                t.setType(rs.getString("type"));
                t.setPrix(rs.getDouble("prix"));
                t.setStatut(rs.getString("statut"));
                t.setDateDebut(rs.getDate("dateDebut"));
                t.setDateFin(rs.getDate("dateFin"));
                t.setDestinationId(rs.getInt("destination_id"));
                list.add(t);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public int countTicketsByReservation(int reservationId) {
        String sql = "SELECT COUNT(*) FROM ticket WHERE reservation_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Ticket> getAvailableTickets() {
        List<Ticket> tickets = new ArrayList<>();

        String sql = """
            SELECT t.*, d.nom AS destination_nom
            FROM ticket t
            LEFT JOIN destination d ON t.destination_id = d.id
            WHERE t.reservation_id IS NULL
        """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ticket ticket = new Ticket();

                ticket.setId(rs.getInt("id"));
                ticket.setReservationId(rs.getObject("reservation_id") != null ? rs.getInt("reservation_id") : null);
                ticket.setActiviteId(rs.getObject("activite_id") != null ? rs.getInt("activite_id") : null); // ✅ NEW
                ticket.setType(rs.getString("type"));
                ticket.setPrix(rs.getDouble("prix"));
                ticket.setStatut(rs.getString("statut"));
                ticket.setDateDebut(rs.getDate("dateDebut"));
                ticket.setDateFin(rs.getDate("dateFin"));
                ticket.setDestinationId(rs.getInt("destination_id"));
                ticket.setDestinationNom(rs.getString("destination_nom"));

                tickets.add(ticket);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tickets;
    }

    public String getDestinationName(int destinationId) {
        String query = "SELECT nom FROM destination WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, destinationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nom");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public void createTicketsBatch(Connection cn, int reservationId, int activiteId,
                                   int qty, double prixUnitaire, Integer destinationId) throws SQLException {

        String sql = """
        INSERT INTO ticket(reservation_id, activite_id, destination_id, type, prix, statut, dateDebut, dateFin)
        VALUES (?, ?, ?, 'activity', ?, 'reserved', NULL, NULL)
    """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < qty; i++) {
                ps.setInt(1, reservationId);
                ps.setInt(2, activiteId);

                if (destinationId == null) ps.setNull(3, Types.INTEGER);
                else ps.setInt(3, destinationId);

                ps.setDouble(4, prixUnitaire);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    public void addActivityTicket(Connection cn, int reservationId, int activiteId) throws SQLException {

        // On récupère infos de l'activité pour remplir ticket proprement
        String fetchAct = "SELECT prix, destination_id, date_debut, date_fin FROM activite WHERE id = ?";
        double prix;
        Integer destinationId;
        Date dateDebut;
        Date dateFin;

        try (PreparedStatement ps = cn.prepareStatement(fetchAct)) {
            ps.setInt(1, activiteId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Activite not found for ticket insertion.");

            prix = rs.getDouble("prix");
            int dest = rs.getInt("destination_id");
            destinationId = rs.wasNull() ? null : dest;

            dateDebut = rs.getDate("date_debut");
            dateFin = rs.getDate("date_fin");
        }

        String insert = """
        INSERT INTO ticket (reservation_id, activite_id, destination_id, type, prix, statut, dateDebut, dateFin)
        VALUES (?, ?, ?, 'activity', ?, 'reserved', ?, ?)
    """;

        try (PreparedStatement ps = cn.prepareStatement(insert)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, activiteId);

            if (destinationId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, destinationId);

            ps.setDouble(4, prix);
            ps.setDate(5, dateDebut);
            ps.setDate(6, dateFin);

            ps.executeUpdate();
        }
    }
}