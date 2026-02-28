package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.Reservation;

import java.time.LocalDate;

public class BookingTypeController {

    private boolean addMode;
    private Reservation reservation;
    private LocalDate selectedDate;
    private int villeId;

    public void setVilleId(int villeId) {
        this.villeId = villeId;
    }

    public void setAddMode(boolean addMode) {
        this.addMode = addMode;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }

    private void loadPage(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadTicketPage(ActionEvent event, String type) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/FrontTickets.fxml"));
            Parent root = loader.load();

            FrontTicketsController controller = loader.getController();

            controller.setTicketType(type);
            controller.setDestinationId(villeId);

            // 🔥 PASS MODE + DATA
            controller.setAddMode(addMode);
            controller.setReservation(reservation);
            controller.setSelectedDate(selectedDate);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToFlights(ActionEvent e) {
        loadTicketPage(e, "flight");
    }

    public void goToHotels(ActionEvent e) {
        loadTicketPage(e, "hotel");
    }

    public void goToTransport(ActionEvent e) {
        loadTicketPage(e, "transport");
    }

    public void goToActivities(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivitiesPage.fxml"));
            Parent root = loader.load();

            ActivitiesPageController controller = loader.getController();

            // ✅ Passer le contexte pour “add to existing reservation”
            controller.setAddMode(addMode);
            controller.setReservation(reservation);
            controller.setSelectedDate(selectedDate);

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/HomePage.fxml"));

            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML file not found!");
            }

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}