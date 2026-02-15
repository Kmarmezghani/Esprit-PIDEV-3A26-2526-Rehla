package Controllers;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import models.Reservation;
import services.IService;
import services.ReservationService;
import util.DBConnection;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class AjouterReservationController {
    private ReservationService reservationService = new ReservationService();
    private Reservation reservation;

    @FXML
    private ChoiceBox<String> cb_destination;
    @FXML
    private ChoiceBox<String> cb_status;
    private String[] status = {"Confirmed","Pending","Cancelled"};
    private Map<String, Integer> destinationMap = new HashMap<>();

    public void initialize() {
        cb_status.getItems().addAll(status);
        cb_status.setValue("Pending");
        cb_status.setDisable(true);
        loadDestinationsFromDB();

    }

    @FXML
    private DatePicker dp_end;

    @FXML
    private DatePicker dp_start;
    private void loadDestinationsFromDB() {
        try {
            Connection conn = DBConnection.getInstance().getConn();
            String sql = "SELECT id, nom FROM destination";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");

                destinationMap.put(nom, id);
                cb_destination.getItems().add(nom);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void ajouterReservation(ActionEvent event) {
        String selectedDestination = cb_destination.getValue();
        String statut;
        if (reservation == null) {
            statut = "Pending";
        } else {
            statut = cb_status.getValue();
        }
        LocalDate dateDebutLD = dp_start.getValue();
        LocalDate dateFinLD = dp_end.getValue();

        if (statut == null || dateDebutLD == null || dateFinLD == null || selectedDestination == null) {
            showError("All fields must be filled.");
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

        Date dateReservation = Date.valueOf(LocalDate.now());
        Date dateDebut = Date.valueOf(dateDebutLD);
        Date dateFin = Date.valueOf(dateFinLD);

        double coutTotal = 0;
        int personneId = 1;
        int destinationId = destinationMap.get(selectedDestination);

        if (reservation == null) {
            // Ajout
            Reservation newRes = new Reservation(
                    dateReservation,
                    dateDebut,
                    dateFin,
                    statut,
                    coutTotal,
                    personneId,
                    destinationId
            );
            reservationService.add(newRes);
        } else {
            // Modification
            reservation.setDateReservation(dateReservation);
            reservation.setDateDebut(dateDebut);
            reservation.setDateFin(dateFin);
            reservation.setStatut(statut);
            reservation.setCoutTotal(coutTotal);
            reservation.setDestinationId(destinationId);

            reservationService.update(reservation);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
        stage.close();

    }
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;

        // Remplir les champs du formulaire
        dp_start.setValue(reservation.getDateDebut().toLocalDate());
        dp_end.setValue(reservation.getDateFin().toLocalDate());
        cb_status.setValue(reservation.getStatut());
        cb_status.setDisable(false);
        int destinationId = reservation.getDestinationId();

        for (Map.Entry<String, Integer> entry : destinationMap.entrySet()) {
            if (entry.getValue() == destinationId) {
                cb_destination.setValue(entry.getKey());
                break;
            }
        }

    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
