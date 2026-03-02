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
import models.Preference;
import models.Reservation;
import models.Ticket;
import services.PreferenceService;
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
    @FXML private TableColumn<Ticket, String> colRecommended;

    @FXML private Label titleLabel;
    @FXML private Label recommendationLabel;

    private TicketService ticketService = new TicketService();

    private PreferenceService preferenceService = new PreferenceService();
    private final int loggedUserId = 1;

    private Reservation reservation;
    private LocalDate selectedDate;
    private String ticketType; // ✅ NEW
    private boolean addMode = false;
    private int destinationId;
    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
        tryLoadTickets();
    }

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
        colRecommended.setCellValueFactory(data -> {

            Preference pref = preferenceService.getByPersonneId(loggedUserId);

            if (pref != null &&
                    data.getValue().getPrix() >= pref.getBudgetMin() &&
                    data.getValue().getPrix() <= pref.getBudgetMax()) {

                return new SimpleStringProperty("⭐ Recommended");
            }

            return new SimpleStringProperty("");
        });

        ticketTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Ticket ticket, boolean empty) {
                super.updateItem(ticket, empty);

                if (ticket == null || empty) {
                    setStyle("");
                } else {
                    Preference pref = preferenceService.getByPersonneId(loggedUserId);

                    if (pref != null &&
                            ticket.getPrix() >= pref.getBudgetMin() &&
                            ticket.getPrix() <= pref.getBudgetMax()) {

                        setStyle("-fx-background-color: #e6ffe6;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // ===============================
    // LOAD DATA WITH FILTER
    // ===============================
    public void setTicketType(String type) {
        this.ticketType = type;

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

        tryLoadTickets();
    }

    private void loadTickets() {
        List<Ticket> tickets =
                ticketService.getAvailableTicketsByTypeAndVille(ticketType, destinationId);

        List<Ticket> recommended = getRecommendedTickets(tickets);

        ObservableList<Ticket> list = FXCollections.observableArrayList(tickets);
        ticketTable.setItems(list);
        System.out.println("Tickets found: " + tickets.size());
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/BookingType.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private List<Ticket> getRecommendedTickets(List<Ticket> tickets) {

        Preference pref = preferenceService.getByPersonneId(loggedUserId);

        if (pref == null) {
            recommendationLabel.setText("");
            return tickets;
        }

        double min = pref.getBudgetMin();
        double max = pref.getBudgetMax();

        List<Ticket> sorted = tickets.stream()
                .sorted((t1, t2) -> {
                    boolean t1InBudget = t1.getPrix() >= min && t1.getPrix() <= max;
                    boolean t2InBudget = t2.getPrix() >= min && t2.getPrix() <= max;

                    if (t1InBudget && !t2InBudget) return -1;
                    if (!t1InBudget && t2InBudget) return 1;

                    return Double.compare(
                            Math.abs(t1.getPrix() - max),
                            Math.abs(t2.getPrix() - max)
                    );
                })
                .toList();

        // 🔥 BEST MATCH = FIRST ELEMENT
        if (!sorted.isEmpty()) {
            Ticket best = sorted.get(0);

            if (best.getPrix() >= min && best.getPrix() <= max) {
                recommendationLabel.setText(
                        "⭐ Recommended for you: " + best.getType()
                                + " - " + best.getPrix() + " TND (Matches your budget)"
                );
            } else {
                recommendationLabel.setText(
                        "⚠ No tickets fully match your budget."
                );
            }
        }

        return sorted;
    }

    private void tryLoadTickets() {
        if (ticketType != null && destinationId != 0) {
            loadTickets();
        }
    }
}