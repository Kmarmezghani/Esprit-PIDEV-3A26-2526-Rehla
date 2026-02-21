package Controllers;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;
import javafx.stage.*;
import models.Reservation;
import models.Ticket;
import services.ReservationService;
import services.TicketService;

import java.sql.Date;

public class DashboardController {

// ======================================================
// ======================= TABS ==========================
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

// ======================= TAB PANES ====================

    @FXML private TabPane activitetabpanmain;
    @FXML private TabPane destinationtabpanmain;
    @FXML private TabPane posttabpanmain;
    @FXML private TabPane reservationtabpanmain;
    @FXML private TabPane usertabpanmain;

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
    @FXML private TableColumn<Ticket, Void> colDestinationTicket;

// ======================================================
// ================= OTHER TABLES ========================
// ======================================================

    @FXML private TableView<?> tableactivite;
    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<?> tableuser;

// ======================================================
// ================= DASHBOARD BUTTONS ===================
// ======================================================

    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;

// ======================================================
// ===================== SERVICES ========================
// ======================================================

    private final ToggleGroup dashboardGroup = new ToggleGroup();

    private final TicketService ticketService = new TicketService();
    private final ReservationService reservationService = new ReservationService();

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private Reservation selectedReservation;
    private Reservation currentReservationForTickets;

// ======================================================
// ===================== INITIALIZE ======================
// ======================================================

    @FXML
    public void initialize() {

        // Toggle group
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

        // Tables
        initReservationTable();
        initTicketTable();

        addDeleteButton();
        addTicketButton();
        addDeleteTicketButton();

        // Tab behavior
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

        // Window ESC shortcut
        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });

        // Column resizing
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
            colDestinationTicket.setPrefWidth(available * 0.10);
        });
    }

// ======================================================
// ================= RESERVATION METHODS =================
// ======================================================

    private void initReservationTable() {

        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCoutTotal.setCellValueFactory(new PropertyValueFactory<>("coutTotal"));

        colDestination.setCellValueFactory(cell -> {
            String nom = reservationService.getDestinationNomById(cell.getValue().getDestinationId());
            return new SimpleStringProperty(nom);
        });

        colNbrTickets.setCellValueFactory(new PropertyValueFactory<>("nbTickets"));

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
        colDestinationTicket.setCellValueFactory(new PropertyValueFactory<>("destinationNom"));

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
// ================= UI / DASHBOARD ======================
// ======================================================

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

    private void showPane(TabPane pane) {
        hideAllPanes();
        if (pane == null) return;
        pane.setVisible(true);
        pane.setManaged(true);
        pane.toFront();
    }

    private void applySelectedStyles() {
        styleToggle(dashuserbut);
        styleToggle(dashdesbut);
        styleToggle(dashresbut);
        styleToggle(dashactbut);
        styleToggle(dashpostbut);
    }

    private void styleToggle(ToggleButton b) {
        if (b == null) return;
        if (b.isSelected()) {
            b.setStyle("-fx-background-color: #98acd8; -fx-background-radius: 30 0 0 30; -fx-border-color: #18377C; -fx-border-width: 0 0 0 3; -fx-font-weight: 700;");
        } else b.setStyle("");
    }

    @FXML
    void dashboardButtonClicked(ActionEvent e) {
        ToggleButton b = (ToggleButton) e.getSource();

        if (b == dashuserbut) showPane(usertabpanmain);
        else if (b == dashactbut) showPane(activitetabpanmain);
        else if (b == dashdesbut) showPane(destinationtabpanmain);
        else if (b == dashresbut) showPane(reservationtabpanmain);
        else if (b == dashpostbut) showPane(posttabpanmain);
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

            // Récupérer le controller du popup
            AjouterReservationController popupController = loader.getController();

            // Pré-remplir les champs
            popupController.setReservation(reservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Reservation");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            // Après fermeture, refresh TableView
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openEditTicketPopup(Ticket ticket) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Backoffice/ajoutTicket.fxml")
            );

            Parent root = loader.load();
            TicketController popupController = loader.getController();

            popupController.setTicket(ticket);

            // 🔥 IMPORTANT : vérifier si le ticket a une réservation
            Integer reservationId = ticket.getReservationId();

            if (reservationId != null) {
                Reservation reservation = reservationService.getById(reservationId);
                popupController.setReservation(reservation);
            }

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            if (currentReservationForTickets != null) {
                reservationService.updateReservationStats(currentReservationForTickets.getId());
            }

            refreshTicketTable();


            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddTicketPopup(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Backoffice/formTicketBack.fxml")
            );

            Parent root = loader.load();
            TicketController popupController = loader.getController();

            // ✅ Si une réservation est sélectionnée → on la passe
            if (currentReservationForTickets != null) {
                popupController.setReservation(currentReservationForTickets);
            } else if (selectedReservation != null) {
                popupController.setReservation(selectedReservation);
            }
            // Sinon → on n'envoie rien (ticket libre)

            Stage popupStage = new Stage();
            popupStage.setTitle("Add Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            if (currentReservationForTickets != null) {
                reservationService.updateReservationStats(currentReservationForTickets.getId());
            }

            // ✅ Refresh
            if (currentReservationForTickets != null) {
                loadTicketsByReservation(currentReservationForTickets.getId());
            } else {
                refreshTicketTable(); // recharge tous les tickets libres
            }

            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


// ======================================================
// ================= BUTTON FACTORIES ====================
// ======================================================

    private void addDeleteButton() {
        colDeleteReservation.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                btn.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    reservationService.delete(r);
                    getTableView().getItems().remove(r);
                    refreshReservationTable();
                    tableTicket.getItems().clear();
                    currentReservationForTickets = null;
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
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/ticket.png")));
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

                btn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    ticketService.delete(t);

                    // 🔥 ADD THIS
                    if (t.getReservationId() != null) {
                        reservationService.updateReservationStats(t.getReservationId());
                    }

                    getTableView().getItems().remove(t);

                    if (currentReservationForTickets != null)
                        loadTicketsByReservation(currentReservationForTickets.getId());
                    else refreshTicketTable();

                    refreshReservationTable();
                });
            }

            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }



    // ======================================================
    // ================= WINDOW CONTROLS =====================
    // ======================================================

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

}
