package Controllers;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Reservation;
import models.Ticket;
import services.TicketService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class FrontTicketsController {

    @FXML private TableView<Ticket> ticketTable;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, Double> colPrice;
    @FXML private TableColumn<Ticket, String> colDestination;
    @FXML private TableColumn<Ticket, Boolean> colSelect;

    @FXML private Label titleLabel;

    private TicketService ticketService = new TicketService();

    private Reservation reservation;
    private LocalDate selectedDate;
    private String ticketType; // ✅ NEW
    private boolean addMode = false;

    public void setAddMode(boolean addMode) {
        this.addMode = addMode;
    }



    // ===============================
    // INIT
    // ===============================
    @FXML
    public void initialize() {

        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getType()));

        colPrice.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getPrix()));

        // ✅ FIXED (NO DB CALL)
        colDestination.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDestinationNom()));

        // ✅ CHECKBOX COLUMN
        colSelect.setCellFactory(tc -> new TableCell<>() {

            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    ticket.setSelected(checkBox.isSelected());
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(ticket.isSelected());
                    setGraphic(checkBox);
                }
            }
        });
    }

    // ===============================
    // LOAD DATA WITH FILTER
    // ===============================
    public void setTicketType(String type) {
        this.ticketType = type;

        // 🔥 dynamic title
        switch (type.toLowerCase()) {
            case "flight":
                titleLabel.setText("✈ Available Flights");
                break;
            case "hotel":
                titleLabel.setText("🏨 Available Hotels");
                break;
            case "transport":
                titleLabel.setText("🚗 Available Transport");
                break;
            default:
                titleLabel.setText("Tickets");
        }

        loadTickets();
    }

    private void loadTickets() {
        List<Ticket> all = ticketService.getAvailableTickets();

        // ✅ FILTER BY TYPE
        List<Ticket> filtered = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase(ticketType))
                .collect(Collectors.toList());

        ObservableList<Ticket> list = FXCollections.observableArrayList(filtered);
        ticketTable.setItems(list);
    }

    // ===============================
    // SETTERS FROM OTHER PAGES
    // ===============================
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }

    // ===============================
    // NEXT BUTTON (MULTI SELECT)
    // ===============================
    @FXML
    void handleNext(ActionEvent event) {

        List<Ticket> selectedTickets = ticketTable.getItems()
                .stream()
                .filter(Ticket::isSelected)
                .toList();

        if (selectedTickets.isEmpty()) {
            System.out.println("Select at least one ticket");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CreateReservation.fxml"));
            Parent root = loader.load();

            CreateReservationController controller = loader.getController();

            controller.setSelectedTickets(selectedTickets);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Create Reservation");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // QUICK BOOK (SINGLE)
    // ===============================
    @FXML
    private void handleBack(ActionEvent event) {
        // Close the current window
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
    @FXML
    private void handleBookTicket() {

        List<Ticket> selectedTickets = ticketTable.getItems()
                .stream()
                .filter(Ticket::isSelected)
                .toList();

        if (selectedTickets.isEmpty()) {
            System.out.println("Select at least one ticket");
            return;
        }

        try {

            // ✅ CASE 1: ADD TO EXISTING RESERVATION
            if (addMode && reservation != null) {

                for (Ticket t : selectedTickets) {
                    t.setReservationId(reservation.getId());
                    t.setStatut("Reserved");

                    ticketService.update(t);
                }

                System.out.println("Tickets added to existing reservation ✅");

                // 🔥 CLOSE WINDOW
                ((Stage) ticketTable.getScene().getWindow()).close();
            }

            // ✅ CASE 2: NORMAL FLOW (CREATE RESERVATION)
            else {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CreateReservation.fxml"));
                Parent root = loader.load();

                CreateReservationController controller = loader.getController();
                controller.setSelectedTickets(selectedTickets);

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Create Reservation");
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}