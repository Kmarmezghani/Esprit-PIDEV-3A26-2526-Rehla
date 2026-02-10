package services;

import models.Reservation;
import util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    }

    @Override
    public void delete(Reservation reservation) {

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
}
