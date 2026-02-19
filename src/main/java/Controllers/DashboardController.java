package Controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Activite;
import models.Reservation;
import models.Review;
import models.Ticket;
import models.stats.AvgNoteRow;
import models.stats.NoteDistributionRow;
import models.stats.TopActiviteRow;
import services.ActiviteService;
import services.ReservationService;
import services.ReviewService;
import services.TicketService;
import services.StatsService;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.sql.Date;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    // ✅ CHANGE #1 (DATE FORMAT)
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ======================================================
    // ======================= TABS =========================
    // ======================================================

    @FXML private Tab activitetab;
    @FXML private Tab admintab;
    @FXML private Tab attractiontab;
    @FXML private Tab commentairetab;
    @FXML private Tab destinationtab;
    @FXML private Tab posttab;
    @FXML private Tab reservationstab;
    @FXML private Tab reviewtab;
    @FXML private Tab ticketstab;
    @FXML private Tab usertab;
    @FXML private Tab statstab;

    // ======================= TAB PANES ====================

    @FXML private TabPane activitetabpanmain;
    @FXML private TabPane destinationtabpanmain;
    @FXML private TabPane posttabpanmain;
    @FXML private TabPane reservationtabpanmain;
    @FXML private TabPane usertabpanmain;

    // ======================================================
    // ================== ACTIVITE TABLE ====================
    // ======================================================

    @FXML private TableView<Activite> tableactivite;

    @FXML private TableColumn<Activite, String> colnameactivite;
    @FXML private TableColumn<Activite, String> coldescriptionactivite;
    @FXML private TableColumn<Activite, Double> colpriceactivite;
    @FXML private TableColumn<Activite, String> coltypeactivite;
    @FXML private TableColumn<Activite, Double> colavgratactivite;
    @FXML private TableColumn<Activite, String> colguideactivite;
    @FXML private TableColumn<Activite, String> colDestinationactivite;
    @FXML private TableColumn<Activite, String> coldateDactivite;
    @FXML private TableColumn<Activite, String> coldateFactivite;
    @FXML private TableColumn<Activite, Void> colDeleteactivite;
    @FXML private TableColumn<Activite, String> colMaxPlaces;


    // ======================================================
    // ==================== REVIEW TABLE ====================
    // ======================================================

    @FXML private TableView<Review> tableReview;

    @FXML private TableColumn<Review, String> coluserReview;
    @FXML private TableColumn<Review, String> colcommentReview;
    @FXML private TableColumn<Review, Double> colnoteReview;
    @FXML private TableColumn<Review, String> coldateReview;
    @FXML private TableColumn<Review, Void> colDeleteReview;

    // ======================================================
    // ================= RESERVATION TABLE ===================
    // ======================================================

    @FXML private TableView<Reservation> tableReservation;

    @FXML private TableColumn<Reservation, Date> colDateReservation;
    @FXML private TableColumn<Reservation, Date> colDateDebut;
    @FXML private TableColumn<Reservation, Date> colDateFin;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Double> colCoutTotal;
    @FXML private TableColumn<Reservation, String> colDestination;
    @FXML private TableColumn<Reservation, Integer> colNbrTickets;
    @FXML private TableColumn<Reservation, Void> colDeleteReservation;
    @FXML private TableColumn<Reservation, Void> colTicketReservation;

    // ======================================================
    // ===================== TICKET TABLE ====================
    // ======================================================

    @FXML private TableView<Ticket> tableTicket;

    @FXML private TableColumn<Ticket, Date> colDateDebut1;
    @FXML private TableColumn<Ticket, Date> colDateFin1;
    @FXML private TableColumn<Ticket, String> colStatut1;
    @FXML private TableColumn<Ticket, Double> colPrix;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, Void> colDeleteTicket;

    // ======================================================
    // ================= OTHER TABLES =======================
    // ======================================================

    @FXML private TableView<?> tabledestination;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<?> tableuser;

    // ======================================================
    // ================= STATISTICS NODES ===================
    // ======================================================

    @FXML private BarChart<String, Number> barTopActivities;
    @FXML private CategoryAxis xTopActivities;
    @FXML private NumberAxis yTopActivities;

    @FXML private PieChart pieNotes;

    @FXML private TableView<AvgNoteRow> tableAvgNotes;
    @FXML private TableColumn<AvgNoteRow, String> colNom;
    @FXML private TableColumn<AvgNoteRow, Double> colAvg;
    @FXML private TableColumn<AvgNoteRow, Integer> colCount;

    // ======================================================
    // ================= DASHBOARD BUTTONS ==================
    // ======================================================

    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;

    // ======================================================
    // ===================== SERVICES =======================
    // ======================================================

    private final ToggleGroup dashboardGroup = new ToggleGroup();

    private final ActiviteService activiteService = new ActiviteService();
    private final ObservableList<Activite> activiteList = FXCollections.observableArrayList();
    private Activite selectedActivite;

    private final ReviewService reviewService = new ReviewService();
    private final ObservableList<Review> reviewList = FXCollections.observableArrayList();

    private final TicketService ticketService = new TicketService();
    private final ReservationService reservationService = new ReservationService();

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private Reservation selectedReservation;
    private Reservation currentReservationForTickets;

    private final StatsService statsService = new StatsService();

    @FXML
    public void initialize() {

        // =========================
        // DASHBOARD TOGGLE GROUP
        // =========================
        dashuserbut.setToggleGroup(dashboardGroup);
        dashdesbut.setToggleGroup(dashboardGroup);
        dashresbut.setToggleGroup(dashboardGroup);
        dashactbut.setToggleGroup(dashboardGroup);
        dashpostbut.setToggleGroup(dashboardGroup);

        dashboardGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                Platform.runLater(() -> dashboardGroup.selectToggle(oldT));
            }
            applySelectedStyles();
        });

        hideAllPanes();
        showPane(usertabpanmain);

        dashboardGroup.selectToggle(dashuserbut);
        applySelectedStyles();

        // =========================
        // TABLE INIT
        // =========================
        initActiviteTable();
        initReviewTable();
        initReservationTable();
        initTicketTable();

        addDeleteReservationButton();
        addTicketButton();
        addDeleteTicketButton();

        initStatsTables();

        // =========================
        // SELECTION LISTENERS
        // =========================
        ticketstab.setOnSelectionChanged(event -> {
            if (ticketstab.isSelected()) {
                if (currentReservationForTickets == null) {
                    refreshTicketTable();
                } else {
                    loadTicketsByReservation(currentReservationForTickets.getId());
                }
            }
        });

        reservationstab.setOnSelectionChanged(event -> {
            if (reservationstab.isSelected()) {
                currentReservationForTickets = null;
            }
        });

        tableactivite.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedActivite = newSelection;
        });

        reviewtab.setOnSelectionChanged(event -> {
            if (reviewtab.isSelected()) {
                loadReviewsForSelectedActivite();
            }
        });

        if (statstab != null) {
            statstab.setOnSelectionChanged(e -> {
                if (statstab.isSelected()) {
                    refreshActivityStats(null);
                }
            });
        }

        // =========================
        // DOUBLE CLICK ROW = EDIT
        // =========================
        tableactivite.setRowFactory(tv -> {
            TableRow<Activite> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openEditPopup(row.getItem());
                }
            });
            return row;
        });
        tableactivite.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedActivite = newSelection;
        });


        // =========================
        // COLUMN RESIZING (RESERVATION)
        // =========================
        tableReservation.widthProperty().addListener((obs, o, n) -> {
            double available = n.doubleValue() - 20;
            colDateReservation.setPrefWidth(available * 0.15);
            colDateDebut.setPrefWidth(available * 0.15);
            colDateFin.setPrefWidth(available * 0.15);
            colStatut.setPrefWidth(available * 0.12);
            colCoutTotal.setPrefWidth(available * 0.15);
            colDeleteReservation.setPrefWidth(available * 0.10);
        });

        tableTicket.widthProperty().addListener((obs, o, n) -> {
            double available = n.doubleValue() - 20;
            colType.setPrefWidth(available * 0.20);
            colPrix.setPrefWidth(available * 0.15);
            colStatut1.setPrefWidth(available * 0.15);
            colDateDebut1.setPrefWidth(available * 0.15);
            colDateFin1.setPrefWidth(available * 0.15);
            colDeleteTicket.setPrefWidth(available * 0.10);
        });

        tableactivite.widthProperty().addListener((obs, oldW, newW) -> {
            double available = newW.doubleValue() - 20;

            colnameactivite.setPrefWidth(available * 0.11);
            coldescriptionactivite.setPrefWidth(available * 0.22);
            coldateDactivite.setPrefWidth(available * 0.10);
            coldateFactivite.setPrefWidth(available * 0.10);
            colpriceactivite.setPrefWidth(available * 0.08);
            coltypeactivite.setPrefWidth(available * 0.09);
            colavgratactivite.setPrefWidth(available * 0.09);
            colDestinationactivite.setPrefWidth(available * 0.10);
            colguideactivite.setPrefWidth(available * 0.09);
            colDeleteactivite.setPrefWidth(available * 0.02);
            colMaxPlaces.setPrefWidth(available * 0.07);
        });



        // =========================
        // COLUMN RESIZING (REVIEW)
        // =========================
        tableReview.widthProperty().addListener((obs, oldW, newW) -> {
            double available = newW.doubleValue() - 20;

            coluserReview.setPrefWidth(available * 0.15);
            colcommentReview.setPrefWidth(available * 0.45);
            colnoteReview.setPrefWidth(available * 0.10);
            coldateReview.setPrefWidth(available * 0.20);
            colDeleteReview.setPrefWidth(available * 0.10);
        });

        // =========================
        // COLUMN RESIZING (STATS TABLE)
        // =========================
        tableAvgNotes.widthProperty().addListener((obs, oldW, newW) -> {
            double available = newW.doubleValue() - 20;

            colNom.setPrefWidth(available * 0.60);
            colAvg.setPrefWidth(available * 0.20);
            colCount.setPrefWidth(available * 0.20);
        });

        // =========================
        // WINDOW SHORTCUT
        // =========================
        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });
    }

    // ===========================
    // ===== ACTIVITY-RELATED =====
    // ===========================

    private void initActiviteTable() {
        colnameactivite.setCellValueFactory(new PropertyValueFactory<>("nom"));
        coldescriptionactivite.setCellValueFactory(new PropertyValueFactory<>("description"));
        colpriceactivite.setCellValueFactory(new PropertyValueFactory<>("prix"));
        coltypeactivite.setCellValueFactory(new PropertyValueFactory<>("typeActivite"));
        colavgratactivite.setCellValueFactory(new PropertyValueFactory<>("noteMoyenne"));

        colguideactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String guideName = activiteService.getGuideNameByActiviteId(a.getGuideId());
            return new ReadOnlyStringWrapper(guideName);
        });

        colDestinationactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String label = activiteService.getDestinationNameById(a.getDestinationId());
            return new ReadOnlyStringWrapper(label);
        });

        coldateDactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String v = (a.getDateDebut() != null) ? a.getDateDebut().format(DT) : "";
            return new ReadOnlyStringWrapper(v);
        });


        coldateFactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String v = (a.getDateFin() != null) ? a.getDateFin().format(DT) : "";
            return new ReadOnlyStringWrapper(v);
        });
        colMaxPlaces.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            Integer mp = a.getMaxPlaces();
            String v = (mp == null) ? "No limit" : String.valueOf(mp);
            return new ReadOnlyStringWrapper(v);
        });



        tableactivite.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        refreshActiviteTable();
        addDeleteButton();
    }

    private void refreshActiviteTable() {
        activiteList.setAll(activiteService.getAll());
        tableactivite.setItems(activiteList);
    }

    private void addDeleteButton() {
        colDeleteactivite.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-padding: 0;
                    -fx-cursor: hand;
                """);

                // ✅ CHANGE #2 (CONFIRMATION)
                deleteBtn.setOnAction(e -> {
                    Activite a = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete activity");
                    confirm.setContentText("Are you sure you want to delete: " + a.getNom() + " ?");

                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            activiteService.delete(a);
                            getTableView().getItems().remove(a);
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    @FXML
    private void openAddPopupactivite(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController controller = loader.getController();
            controller.setAdminMode(true);

            Stage popupStage = new Stage();
            popupStage.setTitle("Add new activity");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshActiviteTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopup(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController controller = loader.getController();
            controller.setAdminMode(true);
            controller.setActiviteToEdit(activite);

            Stage stage = new Stage();
            stage.setTitle("Edit Activity");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshActiviteTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------- Review Methods -------------------

    private void initReviewTable() {
        coluserReview.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getUserName())
        );
        colcommentReview.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        colnoteReview.setCellValueFactory(new PropertyValueFactory<>("note"));

        // ✅ CHANGE #1 (DATE FORMAT) - si dateAvis = LocalDateTime
        coldateReview.setCellValueFactory(cellData -> {
            Review r = cellData.getValue();
            String dateStr = r.getDateAvis() != null ? r.getDateAvis().format(DT) : "";
            return new ReadOnlyStringWrapper(dateStr);
        });

        tableReview.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        addDeleteReviewButton();
        tableReview.setItems(reviewList);
    }

    private void addDeleteReviewButton() {
        colDeleteReview.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");

                // ✅ CHANGE #2 (CONFIRMATION)
                deleteBtn.setOnAction(e -> {
                    Review review = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete review");
                    confirm.setContentText("Are you sure you want to delete this review?");

                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            reviewService.delete(review);
                            getTableView().getItems().remove(review);
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void loadReviewsForSelectedActivite() {
        if (selectedActivite == null) {
            reviewList.clear();
            return;
        }
        reviewList.setAll(reviewService.getReviewsByActiviteId(selectedActivite.getId()));
    }

    // ======================================================
    // ================= RESERVATION METHODS =================
    // ======================================================

    private void initReservationTable() {

        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCoutTotal.setCellValueFactory(cell -> {
            double total = ticketService.sumPrixByReservation(cell.getValue().getId());
            return new SimpleDoubleProperty(total).asObject();
        });

        colDestination.setCellValueFactory(cell -> {
            String nom = reservationService.getDestinationNomById(cell.getValue().getDestinationId());
            return new SimpleStringProperty(nom);
        });

        colNbrTickets.setCellValueFactory(cell -> {
            int count = ticketService.countTicketsByReservation(cell.getValue().getId());
            return new SimpleIntegerProperty(count).asObject();
        });

        tableReservation.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditPopup(row.getItem());
                }
            });
            return row;
        });

        tableReservation.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedReservation = newV;
            if (newV != null) loadTicketsByReservation(newV.getId());
        });

        refreshReservationTable();
    }

    private void refreshReservationTable() {
        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);
    }

    // ======================================================
    // ==================== TICKET METHODS ===================
    // ======================================================

    private void initTicketTable() {

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut1.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDateDebut1.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin1.setCellValueFactory(new PropertyValueFactory<>("dateFin"));

        tableTicket.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditTicketPopup(row.getItem());
                }
            });
            return row;
        });

        refreshTicketTable();
    }

    private void refreshTicketTable() {
        ticketList.setAll(ticketService.getAll());
        tableTicket.setItems(ticketList);
    }

    private void loadTicketsByReservation(int id) {
        ticketList.setAll(ticketService.getTicketsByReservation(id));
        tableTicket.setItems(ticketList);
    }

    // ======================================================
    // ==================== STATISTICS =======================
    // ======================================================

    private void initStatsTables() {
        if (tableAvgNotes == null) return;

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAvg.setCellValueFactory(new PropertyValueFactory<>("avgNote"));
        colCount.setCellValueFactory(new PropertyValueFactory<>("nbAvis"));
    }

    @FXML
    private void refreshActivityStats(ActionEvent event) {
        try {
            if (barTopActivities != null) {

                List<TopActiviteRow> top = statsService.getTopActivitiesByInscriptions(7);

                barTopActivities.getData().clear();

                XYChart.Series<String, Number> s = new XYChart.Series<>();
                for (TopActiviteRow r : top) {
                    s.getData().add(new XYChart.Data<>(r.nom(), r.nbAvis()));
                }

                barTopActivities.getData().add(s);

                int max = top.stream()
                        .mapToInt(TopActiviteRow::nbAvis)
                        .max()
                        .orElse(1);

                yTopActivities.setAutoRanging(false);
                yTopActivities.setLowerBound(0);
                yTopActivities.setUpperBound(max + 1);
                yTopActivities.setTickUnit(1);

                Platform.runLater(() -> {
                    barTopActivities.lookupAll(".default-color0.chart-bar").forEach(node ->
                            node.setStyle("-fx-bar-fill: #002b11;")
                    );
                });
            }

            if (pieNotes != null) {
                List<NoteDistributionRow> dist = statsService.getNoteDistribution();

                Map<Integer, Integer> map = new HashMap<>();
                for (NoteDistributionRow row : dist) {
                    map.put(row.getNote(), row.getNb());
                }
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (int note = 1; note <= 5; note++) {
                    int nb = map.getOrDefault(note, 0);
                    pieData.add(new PieChart.Data(note + " ★", nb));
                }
                pieNotes.setData(pieData);
            }

            if (tableAvgNotes != null) {
                List<AvgNoteRow> avg = statsService.getAvgNotesByActivity();
                tableAvgNotes.setItems(FXCollections.observableArrayList(avg));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage());
            a.showAndWait();
        }
    }

    // ======================================================
    // ======================= UI HELPERS ====================
    // ======================================================

    private void applySelectedStyles() {
        styleToggle(dashuserbut);
        styleToggle(dashdesbut);
        styleToggle(dashresbut);
        styleToggle(dashactbut);
        styleToggle(dashpostbut);
    }

    // ======================================================
    // ======================= POPUPS ========================
    // ======================================================

    @FXML
    private void openAddPopup(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Backoffice/ajoutReservation.fxml"));

            Stage stage = new Stage();
            stage.setTitle("Add new reservation");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/logoblue.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(((Node) e.getSource()).getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshReservationTable();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openEditPopup(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutReservation.fxml"));
            Parent root = loader.load();

            AjouterReservationController popupController = loader.getController();
            popupController.setReservation(reservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Reservation");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditTicketPopup(Ticket ticket) {
        try {
            int reservationId = ticket.getReservationId();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController popupController = loader.getController();
            Reservation reservation = reservationService.getById(ticket.getReservationId());

            popupController.setTicket(ticket);
            popupController.setReservation(reservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            loadTicketsByReservation(reservationId);
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddTicketPopup(ActionEvent event) {
        if (currentReservationForTickets == null) {
            if (selectedReservation != null) {
                currentReservationForTickets = selectedReservation;
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une réservation !");
                alert.showAndWait();
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutTicket.fxml"));
            Parent root = loader.load();
            TicketController popupController = loader.getController();

            popupController.setReservation(currentReservationForTickets);

            Stage popupStage = new Stage();
            popupStage.setTitle("Add Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            loadTicketsByReservation(currentReservationForTickets.getId());
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================================================
    // ================= BUTTON FACTORIES ====================
    // ======================================================

    private void addDeleteReservationButton() {
        colDeleteReservation.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                // ✅ CHANGE #2 (CONFIRMATION)
                btn.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete reservation");
                    confirm.setContentText("Are you sure you want to delete this reservation?");

                    confirm.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.OK) {
                            reservationService.delete(r);
                            getTableView().getItems().remove(r);
                            refreshReservationTable();
                            tableTicket.getItems().clear();
                            currentReservationForTickets = null;
                        }
                    });
                });
            }

            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addTicketButton() {
        colTicketReservation.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/ticket.png")));
                icon.setFitWidth(22);
                icon.setFitHeight(27);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                btn.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    currentReservationForTickets = r;
                    reservationtabpanmain.getSelectionModel().select(ticketstab);
                    loadTicketsByReservation(r.getId());
                });
            }

            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addDeleteTicketButton() {
        colDeleteTicket.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                // ✅ CHANGE #2 (CONFIRMATION)
                btn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete ticket");
                    confirm.setContentText("Are you sure you want to delete this ticket?");

                    confirm.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.OK) {
                            ticketService.delete(t);
                            getTableView().getItems().remove(t);

                            if (currentReservationForTickets != null)
                                loadTicketsByReservation(currentReservationForTickets.getId());
                            else
                                refreshTicketTable();

                            refreshReservationTable();
                        }
                    });
                });
            }

            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ------------------- Dashboard / Tab Methods -------------------

    private void hideAllPanes() {
        hidePane(usertabpanmain);
        hidePane(activitetabpanmain);
        hidePane(destinationtabpanmain);
        hidePane(reservationtabpanmain);
        hidePane(posttabpanmain);
    }

    private void hidePane(TabPane p) {
        if (p == null) return;
        p.setVisible(false);
        p.setManaged(false);
    }

    private void showPane(TabPane paneToShow) {
        hideAllPanes();
        if (paneToShow == null) return;
        paneToShow.setVisible(true);
        paneToShow.setManaged(true);
        paneToShow.toFront();
    }

    private void styleToggle(ToggleButton b) {
        if (b == null) return;

        if (b.isSelected()) {
            b.setStyle(
                    "-fx-background-color: #98acd8;" +
                            "-fx-background-radius: 30 0 0 30;" +
                            "-fx-border-color: #18377C;" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-font-weight: 700;"
            );
        } else {
            b.setStyle("");
        }
    }

    @FXML
    void dashboardButtonClicked(ActionEvent event) {
        ToggleButton clicked = (ToggleButton) event.getSource();

        if (clicked == dashuserbut) {
            showPane(usertabpanmain);
        } else if (clicked == dashactbut) {
            showPane(activitetabpanmain);
        } else if (clicked == dashdesbut) {
            showPane(destinationtabpanmain);
        } else if (clicked == dashresbut) {
            showPane(reservationtabpanmain);
        } else if (clicked == dashpostbut) {
            showPane(posttabpanmain);
        }
    }

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

    @FXML
    private void FXaddActivite(ActionEvent event) {
    }
}
