package services;

import models.Reservation;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class ReservationService implements IService <Reservation>{
    Connection conn;

    public ReservationService() {
        this.conn = DBConnection.getInstance().getConn();
    }

    @Override
    public void add(Reservation reservation) {
        String SQL = "INSERT INTO reservation " +
                "(dateReservation, dateDebut, dateFin, statut, coutTotal, personne_id, destination_id) VALUES ('" +
                reservation.getDateReservation() + "','" +
                reservation.getDateDebut() + "','" +
                reservation.getDateFin() + "','" +
                reservation.getStatut() + "','" +
                reservation.getCoutTotal() + "','" +
                reservation.getPersonneId() + "','" +
                reservation.getDestinationId() + "')";

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Reservation reservation) {
        String SQL = "UPDATE reservation SET " +
                "dateReservation = ?, " +
                "dateDebut = ?, " +
                "dateFin = ?, " +
                "statut = ?, " +
                "coutTotal = ?, " +
                "personne_id = ?, " +
                "destination_id = ? " +
                "WHERE id = ?";

        try {
            PreparedStatement stmt = conn.prepareStatement(SQL);
            stmt.setDate(1, reservation.getDateReservation());
            stmt.setDate(2, reservation.getDateDebut());
            stmt.setDate(3, reservation.getDateFin());
            stmt.setString(4, reservation.getStatut());
            stmt.setDouble(5, reservation.getCoutTotal());
            stmt.setInt(6, reservation.getPersonneId());
            stmt.setInt(7, reservation.getDestinationId());
            stmt.setInt(8, reservation.getId());

            stmt.executeUpdate();
            System.out.println("Reservation updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void delete(Reservation reservation) {
        String SQL = "DELETE FROM reservation WHERE id = " + reservation.getId();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("reservation deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public List<Reservation> getAll() {
        String req = "SELECT * FROM `reservation`";
        ArrayList<Reservation> reservations = new ArrayList<>();
        Statement stm;
        try {
            stm = this.conn.createStatement();


            ResultSet rs=  stm.executeQuery(req);
            while (rs.next()){
                Reservation r = new Reservation();

                r.setId(rs.getInt("id"));
                r.setDateReservation(rs.getDate("dateReservation"));
                r.setDateDebut(rs.getDate("dateDebut"));
                r.setDateFin(rs.getDate("dateFin"));
                r.setStatut(rs.getString("statut"));
                r.setCoutTotal(rs.getDouble("coutTotal"));
                r.setPersonneId(rs.getInt("personne_id"));
                r.setDestinationId(rs.getInt("destination_id"));

                reservations.add(r);
            }


        } catch (SQLException ex) {

            System.out.println(ex.getMessage());

        }
        return reservations;
    }

    public int getNombreTickets(int reservationId) {
        String sql = "SELECT COUNT(*) FROM ticket WHERE reservation_id = " + reservationId;
        int count = 0;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
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


}
