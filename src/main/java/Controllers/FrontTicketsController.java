package Controllers;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

public class FrontTicketsController {

    @FXML private TableView<Ticket> ticketTable;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, Double> colPrice;
    @FXML private TableColumn<Ticket, String> colDestination;
    @FXML private TableColumn<Ticket, Boolean> colSelect;

    private TicketService ticketService = new TicketService();
    private Reservation reservation;
    private LocalDate selectedDate;

    @FXML
    public void initialize() {

        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrix()));
        colDestination.setCellValueFactory(data -> new SimpleStringProperty(
                ticketService.getDestinationName(data.getValue().getDestinationId()) // fetch destination name
        ));

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

        ticketTable.getItems().addAll(ticketService.getAvailableTickets());

        // Populate table
        List<Ticket> tickets = ticketService.getAvailableTickets();
        ObservableList<Ticket> ticketList = FXCollections.observableArrayList(tickets);
        ticketTable.setItems(ticketList);
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }



    private void openReservationPopup(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CreateReservation.fxml"));
            Parent root = loader.load();

            CreateReservationController controller = loader.getController();
            ObservableList<Ticket> selectedTickets =
                    ticketTable.getSelectionModel().getSelectedItems();

// ✅ PASS THEM
            controller.setSelectedTickets(selectedTickets);

            Stage stage = new Stage();
            stage.setTitle("Create Reservation");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
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

    @FXML
    private void handleBookTicket() {

        Ticket selectedTicket = ticketTable.getSelectionModel().getSelectedItem();

        if (selectedTicket == null) {
            System.out.println("Select a ticket");
            return;
        }

        try {
            // 🔥 attach ticket to reservation
            selectedTicket.setReservationId(reservation.getId());

            // optional: assign date (if you add date field later)
            // selectedTicket.setDate(...);

            selectedTicket.setStatut("Reserved");

            TicketService ticketService = new TicketService();
            ticketService.update(selectedTicket);

            // close popup
            ((Stage) ticketTable.getScene().getWindow()).close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}