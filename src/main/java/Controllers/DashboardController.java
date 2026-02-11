package Controllers;

import javafx.application.Platform;
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
import services.ActiviteService;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class DashboardController {


    @FXML private Tab activitetab;
    @FXML private TabPane activitetabpanmain;

    @FXML private Tab admintab;

    @FXML private Tab attractiontab;
    @FXML private Tab commentairetab;

    @FXML private Tab destinationtab;
    @FXML private TabPane destinationtabpanmain;

    @FXML private Tab posttab;
    @FXML private TabPane posttabpanmain;

    @FXML private Tab reservationstab;
    @FXML private TabPane reservationtabpanmain;

    @FXML private Tab reviewtab;

    @FXML private Tab ticketstab;

    @FXML private Tab usertab;
    @FXML private TabPane usertabpanmain;

    // TableView
    @FXML
    private TableView<Activite> tableactivite;

    // Columns
    @FXML
    private TableColumn<Activite, String> colnameactivite;

    @FXML
    private TableColumn<Activite, String> coldescriptionactivite;

    @FXML
    private TableColumn<Activite, Double> colpriceactivite;

    @FXML
    private TableColumn<Activite, Double> coldureeactivite;

    @FXML
    private TableColumn<Activite, String> coltypeactivite;

    @FXML
    private TableColumn<Activite, Double> colavgratactivite;

    @FXML
    private TableColumn<Activite, Integer> colguideactivite;
    @FXML
    private TableColumn<Activite, Void> colDeleteactivite;

    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<?> tableuser;


    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;


    private final ToggleGroup dashboardGroup = new ToggleGroup();
    private final ActiviteService activiteService = new ActiviteService();
    private final ObservableList<Activite> activiteList = FXCollections.observableArrayList();


    @FXML
    public void initialize() {


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
        initActiviteTable();
        addDeleteButton();
        tableactivite.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            double available = w - 20;


            colnameactivite.setPrefWidth(available * 0.15);
            coldescriptionactivite.setPrefWidth(available * 0.3);
            colpriceactivite.setPrefWidth(available * 0.12);
            coldureeactivite.setPrefWidth(available * 0.12);
            coltypeactivite.setPrefWidth(available * 0.10);
            colavgratactivite.setPrefWidth(available * 0.12);
            colguideactivite.setPrefWidth(available * 0.10);
        });
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

    private void initActiviteTable() {

        colnameactivite.setCellValueFactory(new PropertyValueFactory<>("nom"));
        coldescriptionactivite.setCellValueFactory(new PropertyValueFactory<>("description"));
        colpriceactivite.setCellValueFactory(new PropertyValueFactory<>("prix"));
        coldureeactivite.setCellValueFactory(new PropertyValueFactory<>("duree"));
        coltypeactivite.setCellValueFactory(new PropertyValueFactory<>("typeActivite"));
        colavgratactivite.setCellValueFactory(new PropertyValueFactory<>("noteMoyenne"));
        colguideactivite.setCellValueFactory(new PropertyValueFactory<>("guideId"));

        tableactivite.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        refreshActiviteTable();

        Platform.runLater(() -> {
            colnameactivite.setPrefWidth(160);
            coldescriptionactivite.setPrefWidth(200);
            colpriceactivite.setPrefWidth(100);
            coldureeactivite.setPrefWidth(100);
            coltypeactivite.setPrefWidth(140);
            colavgratactivite.setPrefWidth(170);
            colguideactivite.setPrefWidth(90);
            colDeleteactivite.setPrefWidth(50);
        });
    }

    private void refreshActiviteTable() {
        activiteList.setAll(activiteService.getAll());
        tableactivite.setItems(activiteList);
    }



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
    void FXaddActivite(ActionEvent event) {

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
    void openAddPopupactivite(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FormulaireAddActivite.fxml"));

            Stage popupStage = new Stage();
            popupStage.setTitle("Add new user");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshActiviteTable();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void addDeleteButton() {

        colDeleteactivite.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/icons/poubelle.png")
                ));
                icon.setFitWidth(20);
                icon.setFitHeight(20);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                -fx-cursor: hand;
            """);

                deleteBtn.setOnAction(e -> {
                    Activite activite = getTableView().getItems().get(getIndex());

                    activiteService.delete(activite);      // DB
                    getTableView().getItems().remove(activite); // UI
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

}
