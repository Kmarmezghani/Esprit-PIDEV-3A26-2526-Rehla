package rehla;

import util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import models.Reservation;
import services.IService;
import services.ReservationService;

public class Main {
    public static void main(String[] args) {
        IService<Reservation> service = new ReservationService();

        Reservation r = new Reservation(
                Date.valueOf("2026-02-08"),   // dateReservation
                Date.valueOf("2026-07-01"),   // dateDebut
                Date.valueOf("2026-07-10"),   // dateFin
                "CONFIRMEE",                  // statut
                1750.00,                      // coutTotal
                1,                            // personne_id (FK)
                1                             // destination_id (FK)
        );

        service.add(r);

        System.out.println(service.getAll());
    }
}
