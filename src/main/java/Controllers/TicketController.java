package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Reservation;
import models.Ticket;
import services.TicketService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

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

    @FXML
    private ComboBox<String> cb_destination;

    private String[] status = {"Available", "Reserved", "Cancelled"};
    private String[] types = {"Flight", "Hotel", "Transport", "Activity", "Tour"};
    private Ticket ticket;
    private Integer reservationId;
    private TicketService ticketService = new TicketService();
    private Map<String, Integer> destinationMap = new HashMap<>();

    private Reservation reservation;

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        this.reservationId = reservation.getId();
    }


    // ===== INIT =====
    public void initialize() {

        if (cb_status != null) {
            cb_status.getItems().addAll(status);
            cb_status.setValue("Available");
            cb_status.setDisable(true);
        }

        if (cb_type != null) {
            cb_type.getItems().addAll(types);
        }

        try {
            String sql = "SELECT * FROM destination";
            var conn = util.DBConnection.getInstance().getConn();
            var st = conn.createStatement();
            var rs = st.executeQuery(sql);

            while (rs.next()) {
                String nom = rs.getString("nom");
                int id = rs.getInt("id");

                cb_destination.getItems().add(nom);
                destinationMap.put(nom, id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🔥 IMPORTANT : Vérifier si DatePicker existe
        if (dp_start != null) {
            dp_start.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            });
        }

        if (dp_end != null) {
            dp_end.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);

                    if (dp_start.getValue() != null) {
                        setDisable(empty || date.isBefore(dp_start.getValue()));
                    }
                }
            });
        }
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    // Pour modification
    public void setTicket(Ticket ticket) {
        this.ticket = ticket;

        if (ticket == null) return; // sécurité

        // ----- PRIX -----
        Double prix = ticket.getPrix(); // wrapper Double
        tf_prix.setText(prix != null ? String.valueOf(prix) : "");

        // ----- STATUT -----
        String statut = ticket.getStatut();
        cb_status.setValue(statut != null ? statut : "Available");

        // ----- TYPE -----
        String type = ticket.getType();
        cb_type.setValue(type != null ? type : null);

        // ----- DATES -----
        dp_start.setValue(ticket.getDateDebut() != null ? ticket.getDateDebut().toLocalDate() : null);
        dp_end.setValue(ticket.getDateFin() != null ? ticket.getDateFin().toLocalDate() : null);

        // ----- DESTINATION -----
        Integer destId = ticket.getDestinationId();
        if (destId != null && destinationMap != null) {
            // destinationMap: Map<String,Integer> → on cherche la clé pour la valeur
            String nomDest = destinationMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(destId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            cb_destination.setValue(nomDest);
        } else {
            cb_destination.setValue(null);
        }

        // ----- RESERVATION -----
        Integer resId = ticket.getReservationId(); // wrapper Integer
        this.reservationId = resId; // peut être null pour ticket libre

        // ----- Gérer le statut selon réservation -----
        cb_status.setDisable(false); // ou true si ticket libre
    }



    @FXML
    void ajouterTicket(ActionEvent event) {

        String type = cb_type.getValue();
        String prixStr = tf_prix.getText();
        String selectedDestination = cb_destination.getValue();

        if (type == null || prixStr.isEmpty() || selectedDestination == null) {
            showError("All required fields must be filled.");
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

        Integer destinationId = destinationMap.get(selectedDestination);

        Ticket newTicket = new Ticket(
                null,
                destinationId,
                type,
                prix,
                "Available",
                null,
                null
        );

        ticketService.add(newTicket);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
        stage.close();
    }

    @FXML
    void updateTicket(ActionEvent event) {

        if (ticket == null) {
            showError("No ticket selected for update.");
            return;
        }

        String type = cb_type.getValue();
        String prixStr = tf_prix.getText();
        String selectedDestination = cb_destination.getValue();

        LocalDate localDateDebut = dp_start.getValue();
        LocalDate localDateFin = dp_end.getValue();

        // ===== VALIDATION BASIQUE =====
        if (type == null || prixStr.isEmpty() || selectedDestination == null) {
            showError("Type, price and destination are required.");
            return;
        }

        // ===== VALIDATION PRIX =====
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

        // ===== CHECK RESERVATION =====
        boolean hasReservation = ticket.getReservationId() != null;

        if (hasReservation) {

            if (localDateDebut == null || localDateFin == null) {
                showError("Dates are required for reserved tickets.");
                return;
            }

           /* if (localDateDebut.isBefore(LocalDate.now())) {
                showError("Start date cannot be in the past.");
                return;
            }*/

            if (localDateFin.isBefore(localDateDebut)) {
                showError("End date cannot be before start date.");
                return;
            }

            // SET DATES
            ticket.setDateDebut(Date.valueOf(localDateDebut));
            ticket.setDateFin(Date.valueOf(localDateFin));

        } else {
            // 🟢 Dates OPTIONAL
            if (localDateDebut != null && localDateFin != null) {

                if (localDateDebut.isBefore(LocalDate.now())) {
                    showError("Start date cannot be in the past.");
                    return;
                }

                if (localDateFin.isBefore(localDateDebut)) {
                    showError("End date cannot be before start date.");
                    return;
                }

                ticket.setDateDebut(Date.valueOf(localDateDebut));
                ticket.setDateFin(Date.valueOf(localDateFin));

            } else {
                // Optional → allow null in DB
                ticket.setDateDebut(null);
                ticket.setDateFin(null);
            }
        }

        // ===== DESTINATION =====
        Integer destinationId = destinationMap.get(selectedDestination);

        // ===== UPDATE OBJECT =====
        ticket.setType(type);
        ticket.setPrix(prix);
        ticket.setDestinationId(destinationId);

        // ===== UPDATE DATABASE =====
        ticketService.update(ticket);

        // ===== CLOSE WINDOW =====
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
