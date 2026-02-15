package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Reservation;
import models.Ticket;
import services.TicketService;

import java.sql.Date;
import java.time.LocalDate;

public class TicketController {
    @FXML
    private ChoiceBox<String> cb_status;

    @FXML
    private ChoiceBox<String> cb_type;

    @FXML
    private DatePicker dp_end;

    @FXML
    private DatePicker dp_start;

    @FXML
    private TextField tf_prix;

    private String[] status = {"Valid", "Pending", "Cancelled"};
    private String[] types = {"Flight", "Hotel", "Transport", "Activity", "Tour"};
    private Ticket ticket;
    private int reservationId;
    private TicketService ticketService = new TicketService();

    private Reservation reservation;

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        this.reservationId = reservation.getId(); // 🔥 THIS LINE FIXES EVERYTHING
    }

    // ===== INIT =====
    public void initialize() {
        cb_status.getItems().addAll(status);
        cb_status.setValue("Pending");
        cb_status.setDisable(true);
        cb_type.getItems().addAll(types);
    }
    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    // Pour modification
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;

        // Remplir le formulaire
        tf_prix.setText(String.valueOf(ticket.getPrix()));
        cb_status.setValue(ticket.getStatut());
        cb_type.setValue(ticket.getType());
        dp_start.setValue(ticket.getDateDebut().toLocalDate());
        dp_end.setValue(ticket.getDateFin().toLocalDate());

        this.reservationId = ticket.getReservationId();
        cb_status.setDisable(false);
    }


    @FXML
    void ajouterTicket(ActionEvent event) {
        String type = cb_type.getValue();
        String statut;

        if (ticket == null) {
            statut = "Pending"; // force Pending in ADD
        } else {
            statut = cb_status.getValue();
        }
        String prixStr = tf_prix.getText();
        LocalDate dateDebutLD = dp_start.getValue();
        LocalDate dateFinLD = dp_end.getValue();

        // Validation
        if (type == null || statut == null || prixStr.isEmpty()
                || dateDebutLD == null || dateFinLD == null) {
            showError("All fields must be filled.");
            return;
        }

        double prix;

        try {
            prix = Double.parseDouble(prixStr);
        } catch (NumberFormatException e) {
            showError("Price must be a valid number.");
            return;
        }

        if (prix <= 0) {
            showError("Price must be greater than 0.");
            return;
        }

        if (dateDebutLD.isBefore(LocalDate.now())) {
            showError("Start date cannot be before today.");
            return;
        }

        if (dateFinLD.isBefore(dateDebutLD)) {
            showError("End date cannot be before start date.");
            return;
        }

        if (reservationId == 0) {
            showError("No reservation selected.");
            return;
        }

        Date dateDebut = Date.valueOf(dateDebutLD);
        Date dateFin = Date.valueOf(dateFinLD);

        LocalDate reservationStart = reservation.getDateDebut().toLocalDate();
        LocalDate reservationEnd = reservation.getDateFin().toLocalDate();
        if (dateDebutLD.isBefore(reservationStart) ||
                dateFinLD.isAfter(reservationEnd)) {

            showError("Ticket dates must be within the reservation period ("
                    + reservationStart + " to " + reservationEnd + ").");
            return;
        }


        if (ticket == null) {
            // ===== AJOUT =====
            Ticket newTicket = new Ticket(
                    reservationId, // FK correctly set
                    type,
                    prix,
                    statut,
                    dateDebut,
                    dateFin
            );

            ticketService.add(newTicket);

        } else {
            // ===== MODIFICATION =====
            ticket.setType(type);
            ticket.setPrix(prix);
            ticket.setStatut(statut);
            ticket.setDateDebut(dateDebut);
            ticket.setDateFin(dateFin);

            ticketService.update(ticket);
        }

        // Fermer popup
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
        stage.close();
    }


    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
        stage.close();

    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
