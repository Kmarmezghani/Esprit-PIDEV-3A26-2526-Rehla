package services;

import models.Ticket;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketService implements IService <Ticket>{
    Connection conn;

    public TicketService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Ticket ticket) {

        String SQL = "INSERT INTO ticket " +
                "(reservation_id, destination_id, type, prix, statut, dateDebut, dateFin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = conn.prepareStatement(SQL);

            if (ticket.getReservationId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, ticket.getReservationId());
            }

            ps.setInt(2, ticket.getDestinationId());
            ps.setString(3, ticket.getType());
            ps.setDouble(4, ticket.getPrix());
            ps.setString(5, ticket.getStatut());
            ps.setDate(6, ticket.getDateDebut());
            ps.setDate(7, ticket.getDateFin());

            ps.executeUpdate();
            System.out.println("Ticket added successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void update(Ticket ticket) {

        String SQL = "UPDATE ticket SET " +
                "reservation_id = ?, " +
                "destination_id = ?, " +
                "type = ?, " +
                "prix = ?, " +
                "statut = ?, " +
                "dateDebut = ?, " +
                "dateFin = ? " +
                "WHERE id = ?";

        try {
            PreparedStatement ps = conn.prepareStatement(SQL);

            // ✅ reservation_id
            if (ticket.getReservationId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, ticket.getReservationId());
            }

            ps.setInt(2, ticket.getDestinationId());
            ps.setString(3, ticket.getType());
            ps.setDouble(4, ticket.getPrix());
            ps.setString(5, ticket.getStatut());
            ps.setDate(6, ticket.getDateDebut());
            ps.setDate(7, ticket.getDateFin());
            ps.setInt(8, ticket.getId());

            ps.executeUpdate();

            System.out.println("Ticket updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Ticket ticket) {
        String SQL = "DELETE FROM ticket WHERE id = " + ticket.getId();

        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Ticket deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Ticket> getAll() {
        List<Ticket> tickets = new ArrayList<>();
        String SQL = "SELECT t.*, d.nom AS destination_nom " +
                "FROM ticket t " +
                "LEFT JOIN destination d ON t.destination_id = d.id";


        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while (rs.next()) {

                Ticket t = new Ticket(
                        rs.getInt("id"),
                        rs.getObject("reservation_id") != null ? rs.getInt("reservation_id") : null,
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
        double total = 0;
        String SQL = "SELECT SUM(prix) FROM ticket WHERE reservation_id = " + reservationId;

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            if (rs.next()) {
                total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return total;
    }

    public List<Ticket> getTicketsByReservation(int reservationId) {

        List<Ticket> list = new ArrayList<>();

        String SQL = "SELECT * FROM ticket WHERE reservation_id = " + reservationId;

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(SQL);

            while (rs.next()) {

                Ticket t = new Ticket();

                t.setId(rs.getInt("id"));
                t.setReservationId(rs.getInt("reservation_id"));
                t.setType(rs.getString("type"));
                t.setPrix(rs.getDouble("prix"));
                t.setStatut(rs.getString("statut"));
                t.setDateDebut(rs.getDate("dateDebut"));
                t.setDateFin(rs.getDate("dateFin"));

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
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Ticket> getAvailableTickets() {

        List<Ticket> tickets = new ArrayList<>();

        String query = "SELECT t.*, d.nom AS destination_nom " +
                "FROM ticket t " +
                "LEFT JOIN destination d ON t.destination_id = d.id " +
                "WHERE t.reservation_id IS NULL"; // optional: only available tickets

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {

                Ticket ticket = new Ticket();

                ticket.setId(rs.getInt("id"));
                ticket.setReservationId(
                        rs.getObject("reservation_id") != null
                                ? rs.getInt("reservation_id")
                                : null
                );
                ticket.setType(rs.getString("type"));
                ticket.setPrix(rs.getDouble("prix"));
                ticket.setStatut(rs.getString("statut"));
                ticket.setDateDebut(rs.getDate("dateDebut"));
                ticket.setDateFin(rs.getDate("dateFin"));
                ticket.setDestinationId(rs.getInt("destination_id"));

                // 🔥 THIS FILLS THE NAME
                ticket.setDestinationNom(rs.getString("destination_nom"));

                tickets.add(ticket);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tickets;
    }

    // Return the name of a destination given its ID
    public String getDestinationName(int destinationId) {
        try {
            String query = "SELECT nom FROM destination WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, destinationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("nom");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Unknown";
    }




}
