package Controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Activite;
import models.Review;
import services.ActiviteService;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.ReviewService;


public class DashboardController {

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
// ================= OTHER TABLES =======================
// ======================================================

    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<?> tableuser;



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

        // =========================
        // SELECTION LISTENERS
        // =========================
        tableactivite.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedActivite = newSelection;
        });

        reviewtab.setOnSelectionChanged(event -> {
            if (reviewtab.isSelected()) {
                loadReviewsForSelectedActivite();
            }
        });

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

        // =========================
        // COLUMN RESIZING (ACTIVITE)
        // =========================
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

    // ===================== ACTIVITE =====================

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
            String v = (a.getDateDebut() != null) ? a.getDateDebut().toString() : "";
            return new ReadOnlyStringWrapper(v);
        });

        coldateFactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String v = (a.getDateFin() != null) ? a.getDateFin().toString() : "";
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

                deleteBtn.setOnAction(e -> {
                    Activite a = getTableView().getItems().get(getIndex());
                    activiteService.delete(a);
                    getTableView().getItems().remove(a);
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
            controller.setAdminMode(true); // ✅ admin create => guide_id stays NULL

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
        coldateReview.setCellValueFactory(cellData -> {
            Review r = cellData.getValue();
            String dateStr = r.getDateAvis() != null ? r.getDateAvis().toString() : "";
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

                deleteBtn.setOnAction(e -> {
                    Review review = getTableView().getItems().get(getIndex());
                    reviewService.delete(review);
                    getTableView().getItems().remove(review);
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
