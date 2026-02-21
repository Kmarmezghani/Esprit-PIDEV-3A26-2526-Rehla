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
                "dateReservation = '" + reservation.getDateReservation() + "', " +
                "dateDebut = '" + reservation.getDateDebut() + "', " +
                "dateFin = '" + reservation.getDateFin() + "', " +
                "statut = '" + reservation.getStatut() + "', " +
                "coutTotal = '" + reservation.getCoutTotal() + "', " +
                "personne_id = '" + reservation.getPersonneId() + "', " +
                "destination_id = '" + reservation.getDestinationId() + "' " +
                "WHERE id = " + reservation.getId();

        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(SQL);
            System.out.println("Reservation updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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

        Reservation reservation = null;

        String sql = "SELECT * FROM reservation WHERE id = ?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, id);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                reservation = new Reservation();

                reservation.setId(rs.getInt("id"));
                reservation.setDateReservation(rs.getDate("dateReservation"));
                reservation.setDateDebut(rs.getDate("dateDebut"));
                reservation.setDateFin(rs.getDate("dateFin"));
                reservation.setStatut(rs.getString("statut"));
                reservation.setCoutTotal(rs.getDouble("coutTotal"));
                reservation.setPersonneId(rs.getInt("personne_id"));
                reservation.setDestinationId(rs.getInt("destination_id"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservation;
    }

    public int getLastInsertedId() {
        String sql = "SELECT MAX(id) FROM reservation";

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }


}
