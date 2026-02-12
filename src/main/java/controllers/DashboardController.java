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
import javafx.stage.Stage;
import models.Post;
import services.PostService;


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

    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<Post> tablepost;
    @FXML private TableView<?> tableuser;


    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;


    private final ToggleGroup dashboardGroup = new ToggleGroup();


    @FXML private TableColumn<Post, Integer> colIdPost;
    @FXML private TableColumn<Post, String> colTitrePost;
    @FXML private TableColumn<Post, String> colContenuPost;
    @FXML private TableColumn<Post, java.time.LocalDate> colDatePost;
    @FXML private TableColumn<Post, Integer> colPopularitePost;
    @FXML private TableColumn<Post, Integer> colAuteurPost;
    @FXML private TableColumn<Post, Integer> colLikesPost;



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

        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });
        loadPosts();

    }
    private PostService postService = new PostService();
    private ObservableList<Post> postList = FXCollections.observableArrayList();




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

    private void loadPosts() {

        colIdPost.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitrePost.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colContenuPost.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDatePost.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colPopularitePost.setCellValueFactory(new PropertyValueFactory<>("popularite"));
        colLikesPost.setCellValueFactory(new PropertyValueFactory<>("nbLikes"));

        // Auteur → on affiche juste son ID
        colAuteurPost.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cellData.getValue().getAuteur().getId()
                ).asObject()
        );

        postList.setAll(postService.getAll());
        tablepost.setItems(postList);
    }

    @FXML
    private void openAddPostForm() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireAddPost.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Post");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadPosts(); // refresh après fermeture

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
