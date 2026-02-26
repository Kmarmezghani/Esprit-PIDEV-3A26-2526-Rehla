package Controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
import services.*;

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
    @FXML private TableColumn<Reservation, Void> colPdf;

    // ================= TICKET TABLE =================

    @FXML
    private AnchorPane calendarContainer;

    private final WeatherService weatherService = new WeatherService();
    private final ReservationService reservationService = new ReservationService();
    private final TicketService ticketService = new TicketService();
    private final CurrencyService currencyService = new CurrencyService();
    private String selectedCurrency = "TND";


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
        addPdfButton();


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
        Integer selectedId = null;

        if (selectedReservation != null) {
            selectedId = selectedReservation.getId();
        }

        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);

        tableReservation.refresh();

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
        ComboBox<String> currencyBox = new ComboBox<>();
        currencyBox.getItems().addAll("EUR", "USD", "TND");
        currencyBox.setValue(selectedCurrency);

        currencyBox.setStyle(
                "-fx-background-color:#3A5BC7;" +
                        "-fx-background-radius:8;" +
                        "-fx-mark-color:white;"   // arrow color
        );
        currencyBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(javafx.scene.paint.Color.WHITE);
                }
            }
        });


        currencyBox.setOnAction(e -> {
            selectedCurrency = currencyBox.getValue();
            showCalendarForReservation(reservation); // refresh
        });
        header.getChildren().addAll(monthLabel, spacerHeader, currencyBox);


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
        String rawCity = reservationService.getDestinationNomById(reservation.getDestinationId());

        final String city = (rawCity != null)
                ? rawCity.trim().replace(" ", "%20")
                : "";

// DEBUG (very important)
        System.out.println("CITY SENT TO API = [" + city + "]");

        for (int day = 1; day <= daysInMonth; day++) {

            LocalDate currentDate = yearMonth.atDay(day);

            VBox dayBox = new VBox(5);
            dayBox.setPrefSize(150, 120);
            dayBox.setStyle("-fx-padding:5; -fx-border-color: #ccc; -fx-border-width:1; -fx-background-radius:5; -fx-border-radius:5;");

            Label dayNumber = new Label(String.valueOf(day));
            dayNumber.setStyle("-fx-font-weight:bold;");
            dayBox.getChildren().add(dayNumber);


            LocalDate selectedDate = currentDate;

            if (!currentDate.isBefore(reservationStart) && !currentDate.isAfter(reservationEnd)) {

                // ✅ ENABLE ONLY VALID DAYS
                dayBox.setStyle(dayBox.getStyle() + "; -fx-background-color: #e8f0ff;");

                dayBox.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) { // ✅ DOUBLE CLICK ONLY
                        openTicketSelectionPopup(selectedDate);
                    }
                });

            } else {

                // ❌ DISABLE INVALID DAYS
                dayBox.setStyle(dayBox.getStyle() + "; -fx-background-color: #f5f5f5; -fx-opacity:0.6;");
                dayBox.setDisable(true); // 🔥 BLOCK ANY CLICK
            }

            // ===== Only show buttons & tickets for reservation days =====
            if (!currentDate.isBefore(reservationStart) && !currentDate.isAfter(reservationEnd)) {

                // ===== Weather button =====
                Button weatherBtn = new Button("🌤 Weather");
                weatherBtn.setStyle("-fx-background-color:#FFD700; -fx-text-fill:black; -fx-padding:3 8 3 8; -fx-background-radius:5;");
                weatherBtn.setOnAction(e -> {

                    if (city == null || city.isEmpty()) {
                        showInfo("Weather", "Invalid destination");
                        return;
                    }

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
                    VBox ticketNode = createTicketNode(ticket);
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

        // ===== ROOT CONTAINER =====
        VBox root = new VBox(20);
        root.setStyle(
                "-fx-padding:30;" +
                        "-fx-background-color: linear-gradient(to bottom right, #4e73df, #1cc88a);" +
                        "-fx-background-radius:20;"
        );
        root.setAlignment(Pos.CENTER);

        // ===== WHITE CARD =====
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-padding:25;" +
                        "-fx-background-radius:20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20,0,0,10);"
        );
        card.setAlignment(Pos.CENTER);

        // ===== TITLE =====
        Label title = new Label("🌤 Weather in " + city);
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#4e73df;");

        Label subTitle = new Label(date.toString());
        subTitle.setStyle("-fx-font-size:14px; -fx-text-fill:gray;");

        // ===== Extract Weather Data =====
        double avgTemp = fullForecast.getDouble("avgtemp_c");
        double maxTemp = fullForecast.getDouble("maxtemp_c");
        double minTemp = fullForecast.getDouble("mintemp_c");
        double rain = fullForecast.getDouble("totalprecip_mm");
        double wind = fullForecast.getDouble("maxwind_kph");
        String condition = fullForecast.getJSONObject("condition").getString("text");

        // ===== Big Temperature =====
        Label tempLabel = new Label(String.format("%.1f°C", avgTemp));
        tempLabel.setStyle("-fx-font-size:48px; -fx-font-weight:bold; -fx-text-fill:#e74a3b;");

        Label conditionLabel = new Label(condition);
        conditionLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#555;");

        // ===== Info Grid =====
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);
        infoGrid.setAlignment(Pos.CENTER);

        infoGrid.add(new Label("⬆ Max Temp:"), 0, 0);
        infoGrid.add(new Label(String.format("%.1f°C", maxTemp)), 1, 0);

        infoGrid.add(new Label("⬇ Min Temp:"), 0, 1);
        infoGrid.add(new Label(String.format("%.1f°C", minTemp)), 1, 1);

        infoGrid.add(new Label("🌧 Rain:"), 0, 2);
        infoGrid.add(new Label(String.format("%.1f mm", rain)), 1, 2);

        infoGrid.add(new Label("💨 Wind:"), 0, 3);
        infoGrid.add(new Label(String.format("%.1f kph", wind)), 1, 3);

        // ===== Close Button =====
        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color:#4e73df;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14px;" +
                        "-fx-padding:8 30 8 30;" +
                        "-fx-background-radius:20;"
        );

        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle("-fx-background-color:#2e59d9; -fx-text-fill:white; -fx-font-size:14px; -fx-padding:8 30 8 30; -fx-background-radius:20;")
        );

        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle("-fx-background-color:#4e73df; -fx-text-fill:white; -fx-font-size:14px; -fx-padding:8 30 8 30; -fx-background-radius:20;")
        );

        closeBtn.setOnAction(e -> dialog.close());

        // ===== Assemble Card =====
        card.getChildren().addAll(title, subTitle, tempLabel, conditionLabel, infoGrid, closeBtn);

        root.getChildren().add(card);

        Scene scene = new Scene(root, 500, 500);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    private void addPdfButton() {

        colPdf.setCellFactory(param -> new TableCell<>() {

            private final Button pdfBtn = new Button("PDF");

            {
                pdfBtn.setStyle("-fx-background-color:#3862B0; -fx-text-fill:white;");
                pdfBtn.setOnAction(event -> {

                    Reservation reservation = getTableView().getItems().get(getIndex());

                    PdfTicketService pdfService = new PdfTicketService();
                    pdfService.generateReservationPdf(reservation);

                    showInfo("PDF", "PDF generated on Desktop!");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pdfBtn);
            }
        });
    }

    private void openTicketSelectionPopup(LocalDate selectedDate) {
        if (selectedReservation == null) return;

        try {
            // 1️⃣ OPEN BOOKING TYPE PAGE (same as Home)
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/BookingType.fxml")
            );

            Parent root = loader.load();

            BookingTypeController controller = loader.getController();

            // 🔥 PASS CONTEXT
            controller.setReservation(selectedReservation);
            controller.setSelectedDate(selectedDate);
            controller.setAddMode(true); // 🔥 IMPORTANT

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Choose Ticket Type");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // refresh after closing
            reservationService.updateReservationStats(selectedReservation.getId());
            refreshReservationTable();
            loadTicketsByReservation(selectedReservation.getId());
            showCalendarForReservation(selectedReservation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createTicketNode(Ticket ticket) {

        VBox ticketBox = new VBox(8);
        ticketBox.setStyle(
                "-fx-background-color:#ffffff;" +
                        "-fx-padding:12;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-radius:10;" +
                        "-fx-border-color:#d0d0d0;" +
                        "-fx-border-width:1;"
        );

        // ===== Convert Price =====
        double basePrice = ticket.getPrix(); // original price in TND
        double convertedPrice;

        if (selectedCurrency.equals("TND")) {
            convertedPrice = basePrice;
        } else {
            convertedPrice = currencyService.convert(basePrice, "TND", selectedCurrency);
        }

        String symbol;
        switch (selectedCurrency) {
            case "USD":
                symbol = "$";
                break;
            case "EUR":
                symbol = "€";
                break;
            default:
                symbol = "TND";
        }

        String formattedPrice = String.format("%.2f %s", convertedPrice, symbol);

        // ===== Labels =====
        Label nameLabel = new Label("Ticket: " + ticket.getType());
        nameLabel.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");

        Label priceLabel = new Label("Price: " + formattedPrice);
        priceLabel.setStyle("-fx-text-fill:#3A5BC7; -fx-font-size:13px;");


        // ===== Delete Button =====
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color:#ff4d4d;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:8;" +
                        "-fx-padding:6 15 6 15;"
        );

        deleteBtn.setOnAction(e -> { int reservationId = ticket.getReservationId();
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

        ticketBox.getChildren().addAll(nameLabel, priceLabel, deleteBtn);

        return ticketBox;
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