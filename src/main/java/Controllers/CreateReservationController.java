package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import models.Personne;
import models.Reservation;
import models.Ticket;
import services.ReservationService;
import services.TicketService;
import util.Session;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class CreateReservationController {

    @FXML
    private DatePicker dpStartDate;

    @FXML
    private DatePicker dpEndDate;

    // ✅ THIS comes from previous page
    private List<Ticket> selectedTickets;

    private final ReservationService reservationService = new ReservationService();
    private final TicketService ticketService = new TicketService();

    Personne CURRENT_USER = Session.getCurrentUser();
    int loggedUserId=CURRENT_USER.getId();


    // ================= RECEIVE DATA =================

    public void setSelectedTickets(List<Ticket> tickets) {
        this.selectedTickets = tickets;

        // OPTIONAL: auto-fill dates from first ticket
        if (tickets != null && !tickets.isEmpty()) {

            Ticket first = tickets.get(0);


        }
    }

    // ================= CREATE RESERVATION =================

    @FXML
    void handleCreateReservation(ActionEvent event) {

        // ❌ no tickets received
        if (selectedTickets == null || selectedTickets.isEmpty()) {
            System.out.println("No tickets selected");
            return;
        }

        // ❌ missing dates
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            System.out.println("Select dates");
            return;
        }

        try {
            Reservation reservation = new Reservation();

            reservation.setPersonneId(loggedUserId);

            // take destination from first ticket
            reservation.setDestinationId(selectedTickets.get(0).getDestinationId());

            reservation.setDateReservation(Date.valueOf(LocalDate.now()));
            reservation.setStatut("Reserved");
            reservation.setDateDebut(Date.valueOf(dpStartDate.getValue()));
            reservation.setDateFin(Date.valueOf(dpEndDate.getValue()));

            // ✅ calculate total
            double total = selectedTickets.stream()
                    .mapToDouble(Ticket::getPrix)
                    .sum();

            reservation.setCoutTotal(total);

            // ✅ insert reservation
            reservationService.add(reservation);

            // ✅ get generated ID
            int reservationId = reservationService.getLastInsertedId();

            // ✅ link tickets
            for (Ticket t : selectedTickets) {
                t.setReservationId(reservationId);
                t.setStatut("Reserved");
                ticketService.update(t);
            }

            // ✅ close popup
            ((Stage) dpStartDate.getScene().getWindow()).close();

            // ✅ go to reservations page
            openMyReservations();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NAVIGATION =================

    private void openMyReservations() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("My Reservations");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}