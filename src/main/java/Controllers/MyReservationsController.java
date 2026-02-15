package Controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Reservation;
import models.Ticket;
import services.ReservationService;
import services.TicketService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;

public class MyReservationsController {
    @FXML
    private TextField searchField;

    // ================= RESERVATION TABLE =================

    @FXML private TableView<Reservation> tableReservation;
    @FXML private TableColumn<Reservation, Integer> colIdReservation;
    @FXML private TableColumn<Reservation, Date> colDateDebut;
    @FXML private TableColumn<Reservation, Date> colDateFin;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Double> colCoutTotal;
    @FXML private TableColumn<Reservation, Integer> colNbrTickets;
    @FXML private TableColumn<Reservation, Void> colDeleteReservation;

    // ================= TICKET TABLE =================

    @FXML
    private AnchorPane calendarContainer;


    private final ReservationService reservationService = new ReservationService();
    private final TicketService ticketService = new TicketService();

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private Reservation selectedReservation;

    // ================= INITIALIZE =================

    @FXML
    public void initialize() {

        // ===== Reservation columns =====
        colIdReservation.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Total cost calculated dynamically
        colCoutTotal.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            double total = ticketService.sumPrixByReservation(reservationId);
            return new SimpleDoubleProperty(total).asObject();
        });
        colNbrTickets.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            int count = ticketService.countTicketsByReservation(reservationId); // méthode à créer si non existante
            return new SimpleIntegerProperty(count).asObject();
        });


        // ===== Ticket columns =====


        addDeleteReservationButton();


        tableReservation.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    selectedReservation = newSelection;
                    if (newSelection != null) {
                        loadTicketsByReservation(newSelection.getId());
                        showCalendarForReservation(newSelection);
                    } else {
                        calendarContainer.getChildren().clear();
                    }
                }
        );
        tableReservation.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            double available = w - 20;


            colDateDebut.setPrefWidth(available * 0.15);
            colDateFin.setPrefWidth(available * 0.15);
            colStatut.setPrefWidth(available * 0.12);
            colCoutTotal.setPrefWidth(available * 0.15);
            colDeleteReservation.setPrefWidth(available * 0.10);
        });
        tableReservation.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Reservation r = row.getItem();
                    openEditReservationPopup(r);
                }
            });
            return row;
        });


        refreshReservationTable();
    }

    // ================= LOAD DATA =================

    private void refreshReservationTable() {
        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);
    }

    private void loadTicketsByReservation(int reservationId) {
        ticketList.setAll(ticketService.getTicketsByReservation(reservationId));

    }

    // ================= DELETE RESERVATION =================

    private void addDeleteReservationButton() {

        colDeleteReservation.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/icons/poubelle.png")
                ));
                icon.setFitWidth(18);
                icon.setFitHeight(18);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                deleteBtn.setOnAction(event -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());

                    reservationService.delete(reservation);

                    refreshReservationTable();

                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    // ================= DELETE TICKET =================



    // ================= ADD TICKET =================

    @FXML
    private void handleAddTicket(ActionEvent event) {

        if (selectedReservation == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Reservation Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a reservation first.");
            alert.showAndWait();
            return;
        }

        try {
            // Charger le FXML du popup d'ajout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.setReservation(selectedReservation);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();


            // 🔹 Recharger les tickets depuis la DB
            ticketList.setAll(ticketService.getTicketsByReservation(selectedReservation.getId()));

            // Reload tickets
            loadTicketsByReservation(selectedReservation.getId());

// Refresh totals without destroying selection
            tableReservation.refresh();

// Refresh calendar safely
            showCalendarForReservation(selectedReservation);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ================= GO HOME =================

    @FXML
    private void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tableReservation.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCalendarForReservation(Reservation reservation) {

        calendarContainer.getChildren().clear();

        VBox mainBox = new VBox(15);
        mainBox.setStyle("-fx-padding:15;");

        // ===== HEADER =====
        HBox header = new HBox(10);

        LocalDate startDate = reservation.getDateDebut().toLocalDate();
        YearMonth yearMonth = YearMonth.from(startDate);

        Label monthLabel = new Label(yearMonth.getMonth() + " " + yearMonth.getYear());
        monthLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addTicketBtn = new Button("➕ Add Ticket");
        addTicketBtn.setStyle("-fx-background-color:#3A5BC7; -fx-text-fill:white;");
        addTicketBtn.setOnAction(e -> handleAddTicket(null));

        header.getChildren().addAll(monthLabel, spacer, addTicketBtn);

        // ===== CALENDAR GRID =====
        GridPane calendarGrid = new GridPane();
        calendarGrid.setGridLinesVisible(true);

        LocalDate firstOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();

        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon

        int row = 0;
        int col = dayOfWeek - 1;

        for (int day = 1; day <= daysInMonth; day++) {

            LocalDate currentDate = yearMonth.atDay(day);

            VBox dayBox = new VBox(5);
            dayBox.setPrefSize(150, 120);
            dayBox.setStyle("-fx-padding:5;");

            Label dayNumber = new Label(String.valueOf(day));
            dayNumber.setStyle("-fx-font-weight:bold;");
            dayBox.getChildren().add(dayNumber);

            // 🔥 Ajouter tickets du jour
            for (Ticket ticket : ticketList) {

                LocalDate ticketStart = ticket.getDateDebut().toLocalDate();
                LocalDate ticketEnd = ticket.getDateFin().toLocalDate();

                if (!currentDate.isBefore(ticketStart) && !currentDate.isAfter(ticketEnd)) {

                    HBox ticketNode = createTicketNode(ticket);
                    dayBox.getChildren().add(ticketNode);
                }
            }

            calendarGrid.add(dayBox, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        mainBox.getChildren().addAll(header, calendarGrid);
        calendarContainer.getChildren().add(mainBox);
    }


    private HBox createTicketNode(Ticket ticket) {

        HBox box = new HBox(5);
        box.setStyle("-fx-background-color:#3A5BC7; -fx-background-radius:8; -fx-padding:5;");
        box.setPrefHeight(25);

        Label label = new Label(ticket.getType() + " - " + ticket.getPrix() + "€");
        label.setStyle("-fx-text-fill:white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:white;");

        // 🔥 DELETE
        deleteBtn.setOnAction(e -> {
            ticketService.delete(ticket);

            loadTicketsByReservation(selectedReservation.getId());
            tableReservation.refresh();
            showCalendarForReservation(selectedReservation);

        });

        // 🔥 DOUBLE CLICK = UPDATE
        box.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openUpdateTicket(ticket);
            }
        });

        box.getChildren().addAll(label, spacer, deleteBtn);

        return box;
    }

    private void openUpdateTicket(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.setTicket(ticket);

            // ✅ Toujours récupérer la réservation du ticket
            Reservation reservation = reservationService.getById(ticket.getReservationId());
            controller.setReservation(reservation);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Update Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // 🔹 Refresh après update
            loadTicketsByReservation(reservation.getId()); // utilisez la réservation récupérée
            refreshReservationTable();
            showCalendarForReservation(reservation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openEditReservationPopup(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajoutReservation.fxml")); // ton FXML de formulaire
            Parent root = loader.load();

            // Récupérer le controller du popup
            AjouterReservationController controller = loader.getController();

            // Pré-remplir les champs avec la réservation sélectionnée
            controller.setReservation(reservation);

            Stage stage = new Stage();
            stage.setTitle("Modifier Réservation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Après fermeture, refresh TableView et tickets si besoin
            refreshReservationTable();
            if (selectedReservation != null) {
                loadTicketsByReservation(selectedReservation.getId());
                showCalendarForReservation(selectedReservation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Window Controls
    @FXML
    void closewindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void minwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    void maxwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    void goToDestinations(ActionEvent event) {
        System.out.println("Destinations - to be implemented");
        showInfo("Destinations", "Implement navigation to your module page");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PostsPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Posts", "Posts page loading...");
        }
    }




}