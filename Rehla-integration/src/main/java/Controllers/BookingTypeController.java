package Controllers;

import javafx.event.ActionEvent;
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
        loadPage(e, "/Frontoffice/ActivitiesPage.fxml");
    }
}