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
                "(reservation_id, type, prix, statut, dateDebut, dateFin) VALUES ('" +
                ticket.getReservationId() + "','" +
                ticket.getType() + "','" +
                ticket.getPrix() + "','" +
                ticket.getStatut() + "','" +
                ticket.getDateDebut() + "','" +
                ticket.getDateFin() + "')";

        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Ticket added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Ticket ticket) {

        String SQL = "UPDATE ticket SET " +
                "reservation_id = '" + ticket.getReservationId() + "', " +
                "type = '" + ticket.getType() + "', " +
                "prix = '" + ticket.getPrix() + "', " +
                "statut = '" + ticket.getStatut() + "', " +
                "dateDebut = '" + ticket.getDateDebut() + "', " +
                "dateFin = '" + ticket.getDateFin() + "' " +
                "WHERE id = " + ticket.getId();

        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Ticket updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
        String SQL = "SELECT * FROM ticket";

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while (rs.next()) {
                Ticket t = new Ticket();
                t.setId(rs.getInt("id"));
                t.setReservationId(rs.getInt("reservation_id"));
                t.setType(rs.getString("type"));
                t.setPrix(rs.getDouble("prix"));
                t.setStatut(rs.getString("statut"));
                t.setDateDebut(rs.getDate("dateDebut"));
                t.setDateFin(rs.getDate("dateFin"));

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



}
