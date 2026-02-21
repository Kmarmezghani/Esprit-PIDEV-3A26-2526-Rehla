package Controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import org.json.JSONObject;
import services.ReservationService;
import services.TicketService;
import services.WeatherService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;

public class MyReservationsController {
    @FXML
    private TextField searchField;

    // ================= RESERVATION TABLE =================

    @FXML private TableView<Reservation> tableReservation;
    @FXML private TableColumn<Reservation, Date> colDateDebut;
    @FXML private TableColumn<Reservation, Date> colDateFin;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Double> colCoutTotal;
    @FXML private TableColumn<Reservation, Integer> colNbrTickets;
    @FXML private TableColumn<Reservation, String> colDestination;
    @FXML private TableColumn<Reservation, Void> colDeleteReservation;

    // ================= TICKET TABLE =================

    @FXML
    private AnchorPane calendarContainer;

    private final WeatherService weatherService = new WeatherService();
    private final ReservationService reservationService = new ReservationService();
    private final TicketService ticketService = new TicketService();

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private Reservation selectedReservation;

    // ================= INITIALIZE =================

    @FXML
    public void initialize() {

        // ===== Reservation columns =====
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Total cost calculated dynamically
        colCoutTotal.setCellValueFactory(new PropertyValueFactory<>("coutTotal"));
        colNbrTickets.setCellValueFactory(new PropertyValueFactory<>("nbTickets"));
        colDestination.setCellValueFactory(cell -> {
            String nom = reservationService.getDestinationNomById(cell.getValue().getDestinationId());
            return new SimpleStringProperty(nom);
        });


        // ===== Ticket columns =====


        addDeleteReservationButton();


        tableReservation.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    selectedReservation = newSelection;
                    if (newSelection != null) {
                        loadTicketsByReservation(newSelection.getId());
                        showCalendarForReservation(newSelection);

                        // 🔥 Show weather prediction
                        String city = reservationService.getDestinationNomById(newSelection.getDestinationId());
                        LocalDate date = newSelection.getDateDebut().toLocalDate();
                        String forecast = weatherService.getWeatherForecast(city, date);
                        showInfo("Weather Forecast", forecast);

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
        Integer selectedId = null;

        if (selectedReservation != null) {
            selectedId = selectedReservation.getId();
        }

        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);

        if (selectedId != null) {
            for (Reservation r : reservationList) {
                if (r.getId() == selectedId) {
                    tableReservation.getSelectionModel().select(r);
                    selectedReservation = r;
                    break;
                }
            }
        }
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

                    if (selectedReservation != null &&
                            selectedReservation.getId() == reservation.getId()) {

                        selectedReservation = null;
                        ticketList.clear();
                        calendarContainer.getChildren().clear();
                    }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.setReservation(selectedReservation);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();


            refreshReservationTable();   // 🔥 recharge les nouvelles valeurs nbTickets
            loadTicketsByReservation(selectedReservation.getId());
            showCalendarForReservation(selectedReservation);

            showCalendarForReservation(selectedReservation);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ================= GO HOME =================

    @FXML
    private void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
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

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button addTicketBtn = new Button("➕ Add Ticket");
        addTicketBtn.setStyle("-fx-background-color:#3A5BC7; -fx-text-fill:white;");
        addTicketBtn.setOnAction(e -> handleAddTicket(null));

        header.getChildren().addAll(monthLabel, spacerHeader, addTicketBtn);

        // ===== CALENDAR GRID =====
        GridPane calendarGrid = new GridPane();
        calendarGrid.setGridLinesVisible(true);

        LocalDate firstOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();

        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon
        int row = 0;
        int col = dayOfWeek - 1;

        LocalDate reservationStart = reservation.getDateDebut().toLocalDate();
        LocalDate reservationEnd = reservation.getDateFin().toLocalDate();

        // 🔥 Get city once
        String city = reservationService.getDestinationNomById(reservation.getDestinationId());

        for (int day = 1; day <= daysInMonth; day++) {

            LocalDate currentDate = yearMonth.atDay(day);

            VBox dayBox = new VBox(5);
            dayBox.setPrefSize(150, 120);
            dayBox.setStyle("-fx-padding:5; -fx-border-color: #ccc; -fx-border-width:1; -fx-background-radius:5; -fx-border-radius:5;");

            Label dayNumber = new Label(String.valueOf(day));
            dayNumber.setStyle("-fx-font-weight:bold;");
            dayBox.getChildren().add(dayNumber);

            LocalDate selectedDate = currentDate;
            dayBox.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                    openTicketSelectionPopup(selectedDate);
                }
            });

            // ===== Only show buttons & tickets for reservation days =====
            if (!currentDate.isBefore(reservationStart) && !currentDate.isAfter(reservationEnd)) {

                // ===== Weather button =====
                Button weatherBtn = new Button("🌤 Weather");
                weatherBtn.setStyle("-fx-background-color:#FFD700; -fx-text-fill:black; -fx-padding:3 8 3 8; -fx-background-radius:5;");
                weatherBtn.setOnAction(e -> {
                    JSONObject forecast = weatherService.getFullForecastForDate(city, selectedDate);
                    if (forecast != null) {
                        showCustomWeatherDialog(selectedDate, city, forecast);
                    } else {
                        showInfo("Weather Forecast", "No forecast available for this date.");
                    }
                });
                dayBox.getChildren().add(weatherBtn);

                // ===== Tickets for this day =====
                for (Ticket ticket : ticketList) {
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

    // ===== Custom Weather Dialog for WeatherAPI.com =====
    private void showCustomWeatherDialog(LocalDate date, String city, JSONObject fullForecast) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Weather Forecast");

        VBox root = new VBox(15);
        root.setStyle(
                "-fx-padding:20;" +
                        "-fx-background-color: #f0f8ff;" +  // light blue background
                        "-fx-border-color: #3A5BC7;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;" +
                        "-fx-background-radius: 15;"
        );

        Label title = new Label("Weather Forecast for " + city + " on " + date);
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill: #3A5BC7;");

        // Extract weather info (WeatherAPI.com format)
        double avgTemp = fullForecast.getDouble("avgtemp_c");
        double maxTemp = fullForecast.getDouble("maxtemp_c");
        double minTemp = fullForecast.getDouble("mintemp_c");
        double rain = fullForecast.getDouble("totalprecip_mm");
        double wind = fullForecast.getDouble("maxwind_kph");
        String condition = fullForecast.getJSONObject("condition").getString("text");

        String contentText = String.format(
                "Condition: %s\nAverage Temp: %.1f°C\nMax Temp: %.1f°C\nMin Temp: %.1f°C\nRain: %.1f mm\nWind: %.1f kph",
                condition, avgTemp, maxTemp, minTemp, rain, wind
        );

        Label content = new Label(contentText);
        content.setStyle("-fx-font-size:14px; -fx-text-fill:#333333;");
        content.setWrapText(true);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color: #3A5BC7; " +
                        "-fx-text-fill:white; " +
                        "-fx-padding:8 25 8 25; " +
                        "-fx-background-radius:8;"
        );
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, content, closeBtn);
        root.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(root, 450, 350); // bigger popup
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    private void openTicketSelectionPopup(LocalDate selectedDate) {
        if (selectedReservation == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/NewTicket.fxml")
            );

            Parent root = loader.load();

            // GET controller of ticket page
            FrontTicketsController controller = loader.getController();

            // 🔥 send reservation + selected date
            controller.setReservation(selectedReservation);
            controller.setSelectedDate(selectedDate);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Select Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            reservationService.updateReservationStats(selectedReservation.getId());

            refreshReservationTable();   // 🔥 reload updated nbTickets + coutTotal
            loadTicketsByReservation(selectedReservation.getId());
            showCalendarForReservation(selectedReservation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createTicketNode(Ticket ticket) {

        HBox box = new HBox(5);
        box.setStyle("-fx-background-color:#3A5BC7; -fx-background-radius:8; -fx-padding:5;");
        box.setPrefHeight(25);

        Label label = new Label(ticket.getType() + " - " + ticket.getPrix() + "€");
        label.setStyle("-fx-text-fill:white;");

        Region spacerTicket = new Region(); // rename to avoid duplicate
        HBox.setHgrow(spacerTicket, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:white;");

        deleteBtn.setOnAction(e -> {

            int reservationId = ticket.getReservationId();

            // 1️⃣ release ticket
            ticketService.releaseTicket(ticket.getId());

            // 2️⃣ update reservation stats
            reservationService.updateReservationStats(reservationId);

            // 3️⃣ refresh UI
            refreshReservationTable();

            if (selectedReservation != null) {
                loadTicketsByReservation(selectedReservation.getId());
                showCalendarForReservation(selectedReservation);
            }
        });


        box.getChildren().addAll(label, spacerTicket, deleteBtn);
        return box;
    }



    private void openEditReservationPopup(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutReservation.fxml")); // ton FXML de formulaire
            Parent root = loader.load();


            AjouterReservationController controller = loader.getController();

            controller.setReservation(reservation);

            Stage stage = new Stage();
            stage.setTitle("Modifier Réservation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

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