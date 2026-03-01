package Controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import models.Personne;
import models.Reservation;
import models.Ticket;
import org.json.JSONObject;
import services.*;
import util.Session;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;

public class MyReservationsController {

    @FXML private TextField searchField;

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
    @FXML private TableColumn<Reservation, Void> colPay;

    @FXML private AnchorPane calendarContainer;

    @FXML private FlowPane countriesFlowPane;
    @FXML private Button btnProfile;
    @FXML private Button btnNotif;
    @FXML private ContextMenu profileMenu;
    @FXML
    private MenuItem menuMyActivities;

    // ================= SERVICES =================
    private final ReservationService reservationService = new ReservationService();
    private final TicketService ticketService = new TicketService();

    // from your code
    private final WaitlistService waitlistService = new WaitlistService();
    private final NotificationService notificationService = new NotificationService(); // si tu l’utilises ailleurs
    private final EmailService emailService = new EmailService();
    private final PersonneService personneService = new PersonneService();

    // from her code
    private final WeatherService weatherService = new WeatherService();
    private final CurrencyService currencyService = new CurrencyService();

    private String selectedCurrency = "TND";


    // ================= DATA =================
    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();
    private Reservation selectedReservation;

    Personne currentUser = Session.getCurrentUser();
    int userId = currentUser.getId();

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        Personne u = Session.getCurrentUser();
        boolean isGuide = (u != null) && "GUIDE".equalsIgnoreCase(u.getRole());

        if (!isGuide) {
            profileMenu.getItems().remove(menuMyActivities); // pas d’espace vide
        }

        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // total + tickets count (ton code)
        colCoutTotal.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            double total = ticketService.sumPrixByReservation(reservationId);
            return new SimpleDoubleProperty(total).asObject();
        });

        colNbrTickets.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            int count = ticketService.countTicketsByReservation(reservationId);
            return new SimpleIntegerProperty(count).asObject();
        });

        // destination (code amie)
        if (colDestination != null) {
            colDestination.setCellValueFactory(cell -> {
                String nom = reservationService.getDestinationNomById(cell.getValue().getDestinationId());
                return new SimpleStringProperty(nom);
            });
        }

        addDeleteReservationButton(); // ton code + waitlist
        addPdfButton();              // code amie
        addPayButton();

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

        // double click row -> edit (les deux codes)
        tableReservation.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {

                    Reservation res = row.getItem();

                    if (isReservationLocked(res)) {
                        showInfo("Not Allowed",
                                "This reservation cannot be modified.");
                    } else {
                        openEditReservationPopup(res);
                    }
                }
            });

            return row;
        });

        refreshReservationTable();
    }

    // ================= LOAD DATA =================
    private void refreshReservationTable() {
        Integer selectedId = (selectedReservation != null) ? selectedReservation.getId() : null;

        reservationList.setAll(
                reservationService.getAll().stream()
                        .filter(res -> res.getPersonneId() == userId)
                        .toList()
        );
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

    // ================= DELETE RESERVATION (ton code + waitlist) =================
    private void addDeleteReservationButton() {

        colDeleteReservation.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button();

            {
                // ✅ adapte le path icône selon ton projet
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/Backoffice/icons/poubelle.png")
                ));
                icon.setFitWidth(18);
                icon.setFitHeight(18);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                deleteBtn.setOnAction(event -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());

                    if (isReservationLocked(reservation)) {
                        showInfo("Not Allowed",
                                "This reservation cannot be deleted.");
                        return;
                    }

                    boolean isActivity = isActivityReservation(reservation.getId());
                    Integer activiteId = isActivity ? getActivityIdFromReservation(reservation.getId()) : null;

                    reservationService.delete(reservation);

                    if (isActivity && activiteId != null) {
                        try {
                            waitlistService.promoteNextIfSeatAvailable(activiteId);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

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

    // ================= PDF (code amie) =================
    private void addPdfButton() {
        if (colPdf == null) return;

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
    private void addPayButton() {

        colPay.setCellFactory(param -> new TableCell<>() {

            private final Button payBtn = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Reservation reservation = getTableView().getItems().get(getIndex());

                LocalDate today = LocalDate.now();
                LocalDate startDate = reservation.getDateDebut().toLocalDate();
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, startDate);

                // ================= ALREADY PAID =================
                if ("PAID".equalsIgnoreCase(reservation.getStatut())) {

                    payBtn.setText("PAID ✓");
                    payBtn.setStyle("-fx-background-color:#28a745; -fx-text-fill:white;");
                    payBtn.setDisable(true);
                }

                // ================= TOO LATE TO PAY =================
                else if (daysBetween < 3) {

                    payBtn.setText("Expired");
                    payBtn.setStyle("-fx-background-color:gray; -fx-text-fill:white;");
                    payBtn.setDisable(true);
                }

                // ================= PAYMENT ALLOWED =================
                else {

                    payBtn.setText("Pay");
                    payBtn.setStyle("-fx-background-color:#ff9800; -fx-text-fill:white;");
                    payBtn.setDisable(false);

                    payBtn.setOnAction(event -> {

                        PaymentService paymentService = new PaymentService();
                        paymentService.payReservation(
                                reservation.getId(),
                                reservation.getCoutTotal()
                        );

                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Confirm Payment");
                        confirm.setHeaderText("Did you complete the payment?");
                        confirm.setContentText("Click OK after successful payment.");

                        confirm.showAndWait().ifPresent(response -> {

                            if (response == ButtonType.OK) {

                                // 1️⃣ Update status
                                reservationService.markAsPaid(reservation.getId());

                                // 2️⃣ Generate invoice PDF
                                PdfInvoiceService invoiceService = new PdfInvoiceService();
                                invoiceService.generateInvoice(reservation);

                                // 3️⃣ Refresh UI
                                refreshReservationTable();

                                showInfo("Success", "Payment confirmed & Invoice generated!");
                            }
                        });
                    });
                }

                setGraphic(payBtn);
            }
        });
    }
    // ================= CALENDAR (fusion) =================
    private void showCalendarForReservation(Reservation reservation) {

        calendarContainer.getChildren().clear();

        VBox mainBox = new VBox(20);
        mainBox.setStyle("-fx-padding:15;");

        LocalDate reservationStart = reservation.getDateDebut().toLocalDate();
        LocalDate reservationEnd = reservation.getDateFin().toLocalDate();

        YearMonth startMonth = YearMonth.from(reservationStart);
        YearMonth endMonth = YearMonth.from(reservationEnd);

        String rawCity = reservationService.getDestinationNomById(reservation.getDestinationId());
        final String city = (rawCity != null) ? rawCity.trim().replace(" ", "%20") : "";

        // 🔁 LOOP THROUGH ALL MONTHS
        YearMonth currentMonth = startMonth;

        while (!currentMonth.isAfter(endMonth)) {

            VBox monthBox = new VBox(10);

            // ===== HEADER =====
            HBox header = new HBox(10);

            Label monthLabel = new Label(currentMonth.getMonth() + " " + currentMonth.getYear());
            monthLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

            Region spacerHeader = new Region();
            HBox.setHgrow(spacerHeader, Priority.ALWAYS);

            ComboBox<String> currencyBox = new ComboBox<>();
            currencyBox.getItems().addAll("EUR", "USD", "TND");
            currencyBox.setValue(selectedCurrency);

            currencyBox.setStyle(
                    "-fx-background-color:#3A5BC7;" +
                            "-fx-background-radius:8;" +
                            "-fx-mark-color:white;"
            );

// white text inside
            currencyBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) setText(null);
                    else {
                        setText(item);
                        setTextFill(javafx.scene.paint.Color.WHITE);
                    }
                }
            });
            currencyBox.setOnAction(e -> {
                selectedCurrency = currencyBox.getValue();
                showCalendarForReservation(reservation);
            });

            header.getChildren().addAll(monthLabel, spacerHeader, currencyBox);

            // ===== GRID =====
            GridPane calendarGrid = new GridPane();
            calendarGrid.setHgap(5);
            calendarGrid.setVgap(5);

// 🔥 WEEK HEADER
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

            for (int i = 0; i < 7; i++) {
                Label dayHeader = new Label(days[i]);
                dayHeader.setStyle(
                        "-fx-font-weight:bold;" +
                                "-fx-text-fill:#3A5BC7;" +
                                "-fx-alignment:center;"
                );
                dayHeader.setPrefWidth(150);
                calendarGrid.add(dayHeader, i, 0);
            }

// 🔥 DAYS
            LocalDate firstOfMonth = currentMonth.atDay(1);
            int daysInMonth = currentMonth.lengthOfMonth();

            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
            int row = 1; // ⬅ start AFTER header
            int col = dayOfWeek - 1;

            for (int day = 1; day <= daysInMonth; day++) {

                LocalDate currentDate = currentMonth.atDay(day);

                VBox dayBox = new VBox(5);
                dayBox.setPrefSize(150, 110);

                dayBox.setStyle(
                        "-fx-padding:6;" +
                                "-fx-background-color:white;" +
                                "-fx-border-color:#ddd;" +
                                "-fx-border-radius:8;" +
                                "-fx-background-radius:8;"
                );

                Label dayNumber = new Label(String.valueOf(day));
                dayNumber.setStyle("-fx-font-weight:bold;");
                dayBox.getChildren().add(dayNumber);

                boolean inRange = !currentDate.isBefore(reservationStart)
                        && !currentDate.isAfter(reservationEnd);

                if (inRange) {
                    dayBox.setStyle(dayBox.getStyle() + "; -fx-background-color:#e8f0ff;");

                    LocalDate selectedDate = currentDate;

                    dayBox.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            openTicketSelectionPopup(selectedDate);
                        }
                    });

                    // 🌤 WEATHER BUTTON
                    Button weatherBtn = new Button("🌤 Weather");
                    weatherBtn.setStyle(
                            "-fx-background-color:#FFD700;" +   // gold
                                    "-fx-text-fill:black;" +
                                    "-fx-padding:3 8 3 8;" +
                                    "-fx-background-radius:5;"
                    );

                    weatherBtn.setOnAction(e -> {
                        JSONObject forecast = weatherService.getFullForecastForDate(city, currentDate);
                        if (forecast != null)
                            showCustomWeatherDialog(currentDate, city, forecast);
                    });

                    dayBox.getChildren().add(weatherBtn);

                    // 🎟 Tickets
                    for (Ticket ticket : ticketList) {
                        dayBox.getChildren().add(createTicketNode(ticket));
                    }

                } else {
                    dayBox.setStyle(dayBox.getStyle() +
                            "; -fx-background-color:#f5f5f5; -fx-opacity:0.6;");
                    dayBox.setDisable(true);
                }

                calendarGrid.add(dayBox, col, row);

                col++;
                if (col == 7) {
                    col = 0;
                    row++;
                }
            }
            monthBox.getChildren().addAll(header, calendarGrid);
            mainBox.getChildren().add(monthBox);

            // ➡️ next month
            currentMonth = currentMonth.plusMonths(1);
        }

        calendarContainer.getChildren().add(mainBox);
    }
    // ================= Ticket Node (code amie + devise + delete release) =================
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

        double basePrice = ticket.getPrix(); // en TND
        double convertedPrice = basePrice;

        if (!"TND".equals(selectedCurrency)) {
            convertedPrice = currencyService.convert(basePrice, "TND", selectedCurrency);
        }

        String symbol = switch (selectedCurrency) {
            case "USD" -> "$";
            case "EUR" -> "€";
            default -> "TND";
        };

        String formattedPrice = String.format("%.2f %s", convertedPrice, symbol);

        Label nameLabel = new Label("Ticket: " + ticket.getType());
        nameLabel.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");

        Label priceLabel = new Label("Price: " + formattedPrice);
        priceLabel.setStyle("-fx-text-fill:#3A5BC7; -fx-font-size:13px;");

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color:#ff4d4d; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 15 6 15;");

        if (isReservationLocked(selectedReservation)) {
            deleteBtn.setDisable(true);
            deleteBtn.setStyle("-fx-background-color:gray; -fx-text-fill:white;");
        } else {
            deleteBtn.setOnAction(e -> {
                int reservationId = ticket.getReservationId();
                // si chez vous c’est “delete(ticket)” au lieu de “releaseTicket(id)” -> adapte
                ticketService.releaseTicket(ticket.getId());
                reservationService.updateReservationStats(reservationId);

                refreshReservationTable();
                if (selectedReservation != null) {
                    loadTicketsByReservation(selectedReservation.getId());
                    showCalendarForReservation(selectedReservation);
                }
            });
        }

        ticketBox.getChildren().addAll(nameLabel, priceLabel, deleteBtn);
        return ticketBox;
    }

    // ================= Add Ticket Popup (code amie: BookingType) =================
    private void openTicketSelectionPopup(LocalDate selectedDate) {
        if (selectedReservation == null) return;
        if (isReservationLocked(selectedReservation)) {
            showInfo("Not Allowed",
                    "You cannot add tickets to this reservation.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/BookingTypeEdit.fxml"));
            Parent root = loader.load();

            BookingTypeController controller = loader.getController();
            controller.setReservation(selectedReservation);
            controller.setSelectedDate(selectedDate);
            controller.setAddMode(true);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Choose Ticket Type");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            reservationService.updateReservationStats(selectedReservation.getId());
            refreshReservationTable();
            loadTicketsByReservation(selectedReservation.getId());
            showCalendarForReservation(selectedReservation);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= EDIT RESERVATION (ton code + email + waitlist) =================
    private void openEditReservationPopup(Reservation reservation) {
        try {
            String oldStatus = reservation.getStatut();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutReservation.fxml"));
            Parent root = loader.load();

            AjouterReservationController controller = loader.getController();
            controller.setReservation(reservation);

            Stage stage = new Stage();
            stage.setTitle("Modifier Réservation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            Reservation updated = reservationService.getById(reservation.getId());
            refreshReservationTable();

            if (updated != null) {
                String newStatus = updated.getStatut();

                if (!"CANCELLED".equalsIgnoreCase(oldStatus)
                        && "CANCELLED".equalsIgnoreCase(newStatus)) {

                    boolean hasActivity = ticketService.hasActivityTicket(updated.getId());
                    if (!hasActivity) return;

                    Integer activiteId = getActivityIdFromReservation(updated.getId());
                    if (activiteId != null) {
                        try {
                            waitlistService.promoteNextIfSeatAvailable(activiteId);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    String userEmail = personneService.getEmailById(updated.getPersonneId());
                    String userName  = personneService.getFullNameById(updated.getPersonneId());

                    if (userEmail != null && !userEmail.isBlank()) {

                        String startDate = updated.getDateDebut() != null ? updated.getDateDebut().toString() : "N/A";
                        String endDate   = updated.getDateFin() != null ? updated.getDateFin().toString() : "N/A";

                        int ticketCount = ticketService.countTicketsByReservation(updated.getId());
                        double total    = ticketService.sumPrixByReservationSafe(updated.getId());
                        String activityName = ticketService.getActivityNameByReservation(updated.getId());

                        new Thread(() -> {
                            try {
                                emailService.sendCancellationConfirmation(
                                        userEmail,
                                        userName,
                                        activityName,
                                        startDate,
                                        endDate,
                                        ticketCount,
                                        total
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            }

            if (selectedReservation != null) {
                loadTicketsByReservation(selectedReservation.getId());
                showCalendarForReservation(selectedReservation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= WAITLIST HELPERS =================
    private boolean isActivityReservation(int reservationId) {
        try { return ticketService.hasActivityTicket(reservationId); }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    private Integer getActivityIdFromReservation(int reservationId) {
        try { return ticketService.getActivityIdByReservation(reservationId); }
        catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ================= Weather Dialog (code amie) =================
    private void showCustomWeatherDialog(LocalDate date, String city, JSONObject fullForecast) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Weather Forecast");

        VBox root = new VBox(20);
        root.setStyle("-fx-padding:30;-fx-background-color: linear-gradient(to bottom right, #4e73df, #1cc88a);-fx-background-radius:20;");
        root.setAlignment(Pos.CENTER);

        VBox card = new VBox(15);
        card.setStyle("-fx-background-color:white;-fx-padding:25;-fx-background-radius:20;-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20,0,0,10);");
        card.setAlignment(Pos.CENTER);

        Label title = new Label("🌤 Weather in " + city);
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#4e73df;");
        Label subTitle = new Label(date.toString());
        subTitle.setStyle("-fx-font-size:14px; -fx-text-fill:gray;");

        double avgTemp = fullForecast.getDouble("avgtemp_c");
        double maxTemp = fullForecast.getDouble("maxtemp_c");
        double minTemp = fullForecast.getDouble("mintemp_c");
        double rain = fullForecast.getDouble("totalprecip_mm");
        double wind = fullForecast.getDouble("maxwind_kph");
        String condition = fullForecast.getJSONObject("condition").getString("text");

        Label tempLabel = new Label(String.format("%.1f°C", avgTemp));
        tempLabel.setStyle("-fx-font-size:48px; -fx-font-weight:bold; -fx-text-fill:#e74a3b;");
        Label conditionLabel = new Label(condition);
        conditionLabel.setStyle("-fx-font-size:16px; -fx-text-fill:#555;");

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

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color:#4e73df;-fx-text-fill:white;-fx-font-size:14px;-fx-padding:8 30 8 30;-fx-background-radius:20;");
        closeBtn.setOnAction(e -> dialog.close());

        card.getChildren().addAll(title, subTitle, tempLabel, conditionLabel, infoGrid, closeBtn);
        root.getChildren().add(card);

        dialog.setScene(new Scene(root, 500, 500));
        dialog.showAndWait();
    }

    // ================= NAVIGATION =================
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

    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/PostsPage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Posts", "Posts page loading...");
        }
    }

    @FXML
    void goToactivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivitiesPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Window Controls
    @FXML void closewindow(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
    @FXML void minwindow(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).setIconified(true);
    }
    @FXML void maxwindow(ActionEvent event) {
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
    public void openProfileMenu(ActionEvent event) {
        if (profileMenu == null || btnProfile == null) return;

        if (profileMenu.isShowing()) {
            profileMenu.hide();
            return;
        }

        profileMenu.show(btnProfile, Side.BOTTOM, 0, 6);
    }

    @FXML
    public void openNotifications(ActionEvent event) {
        // TODO: ouvrir notifications (page/popup)
    }
    @FXML
    void goToDestinations(ActionEvent event) {
        navigateToDestinations();
    }
    private void navigateToDestinations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryBrowsePage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    void goToMyProfile(ActionEvent event) {
        System.out.println("📱 My Profile - to be implemented");
        showInfo("Profile", "My Profile page - implement in your module");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("📝 My Posts - to be implemented");
        showInfo("My Posts", "My Posts page - implement in your module");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        System.out.println("🚪 Logging out...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("✅ User logged out");
                showInfo("Logged Out", "You have been successfully logged out!");
            }
        });
    }
    @FXML
    public void goToMyActivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyActivitiesPage.fxml"));
            Parent root = loader.load();

            MenuItem item = (MenuItem) event.getSource();
            Stage stage = (Stage) item.getParentPopup().getOwnerWindow();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isReservationLocked(Reservation reservation) {

        if (reservation == null) return true;

        String status = reservation.getStatut();

        // 1️⃣ Locked by status
        if ("PAID".equalsIgnoreCase(status) ||
                "CANCELLED".equalsIgnoreCase(status)) {
            return true;
        }

        // 2️⃣ Locked by payment rule (less than 3 days)
        LocalDate today = LocalDate.now();
        LocalDate startDate = reservation.getDateDebut().toLocalDate();

        long daysBetween = java.time.temporal.ChronoUnit.DAYS
                .between(today, startDate);

        return daysBetween < 3;
    }

}