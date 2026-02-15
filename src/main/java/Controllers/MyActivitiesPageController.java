package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;

import java.util.Collections;
import java.util.List;

public class MyActivitiesPageController {

    private static final int CURRENT_USER_ID = 3;

    @FXML private FlowPane myActivitiesFlowPane;
    @FXML private TextField searchField;
    @FXML private Label LBLcount;

    private final ActiviteService activiteService = new ActiviteService();
/*
    @FXML
    public void initialize() {
        loadMyActivities("");
    }

    // ======================
    // Actions in the HERO
    // ======================
    @FXML
    public void handleSearch(javafx.event.ActionEvent event) {
        String q = (searchField.getText() == null) ? "" : searchField.getText().trim();
        loadMyActivities(q);
    }

    /*@FXML
    public void createActivity(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController ctrl = loader.getController();
            ctrl.setGuideId(CURRENT_USER_ID);

            Stage popup = new Stage();
            popup.initOwner(((Node) event.getSource()).getScene().getWindow());
            popup.initModality(javafx.stage.Modality.WINDOW_MODAL);
            popup.setTitle("Create Activity");
            popup.setResizable(false);

            Scene scene = new Scene(root);
            popup.setScene(scene);

            // ✅ when popup closes -> refresh list
            popup.setOnHidden(e -> refreshMyActivities());

            popup.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ you call your service here and rebuild cards in FlowPane
    private void refreshMyActivities() {
        // example:
        // List<Activite> all = activiteService.getByGuideId(CURRENT_USER_ID);
        // renderActivities(all);
    }



    private void loadMyActivities(String query) {
        myActivitiesFlowPane.getChildren().clear();

        List<Activite> list = safeGetMyActivities(query);
        if (LBLcount != null) LBLcount.setText(list.size() + " items");

        for (Activite a : list) {
            myActivitiesFlowPane.getChildren().add(activityCard(a));
        }
    }

    private List<Activite> safeGetMyActivities(String query) {
        try {
            // TEMP: adapt to your real method later (guideId/creatorId)
            List<Activite> all = activiteService.getByGuideId(CURRENT_USER_ID);
            if (all == null) return Collections.emptyList();

            String q = (query == null) ? "" : query.toLowerCase();

            return all.stream()
                    .filter(a -> a != null)
                    // .filter(a -> a.getGuideId() == CURRENT_USER_ID) // enable when you have the field
                    .filter(a -> q.isBlank() || ((a.getNom() == null ? "" : a.getNom().toLowerCase()).contains(q)))
                    .toList();

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private VBox activityCard(Activite a) {
        VBox card = new VBox(8);
        card.setPrefWidth(270);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-padding: 14;
            -fx-border-color: #eef2ff;
            -fx-border-radius: 16;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 3);
        """);

        javafx.scene.control.Label title = new javafx.scene.control.Label(a.getNom() == null ? "Untitled" : a.getNom());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #111827;");

        javafx.scene.control.Label price = new javafx.scene.control.Label(String.format("💰 %.2f TND", a.getPrix()));
        price.setStyle("-fx-text-fill: #4a5f88; -fx-font-size: 13;");

        javafx.scene.control.Label type = new javafx.scene.control.Label("Type: " + (a.getTypeActivite() == null ? "—" : a.getTypeActivite()));
        type.setStyle("-fx-text-fill: #667085; -fx-font-size: 12;");

        card.getChildren().addAll(title, price, type);

        // click later -> open details/edit
        card.setOnMouseClicked(e -> System.out.println("Clicked activity id=" + a.getId()));

        return card;
    }*/

    // ======================
    // NAVIGATION
    // ======================*/
    @FXML public void goToHome(javafx.event.ActionEvent e) { switchScene(e, "/HomePage.fxml"); }
    @FXML public void goToDestinations(javafx.event.ActionEvent e) { /* TODO */ }
    @FXML public void goToPosts(javafx.event.ActionEvent e) { switchScene(e, "/PostsPage.fxml"); }
    @FXML public void goToactivities(javafx.event.ActionEvent e) { switchScene(e, "/ActivitiesPage.fxml"); }

    @FXML public void goToMyProfile(javafx.event.ActionEvent e) { /* TODO */ }
    @FXML public void goToMyPosts(javafx.event.ActionEvent e) { /* TODO */ }
    @FXML public void goToMyReservations(javafx.event.ActionEvent e) { /* TODO */ }
    @FXML public void goToMyActivities(javafx.event.ActionEvent e) { switchScene(e, "/MyActivitiesPage.fxml"); }

    @FXML public void handleLogout(javafx.event.ActionEvent e) { /* TODO */ }

    // ======================
    // WINDOW BUTTONS
    // ======================
    @FXML
    public void minwindow(javafx.event.ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setIconified(true);
    }

    @FXML
    public void maxwindow(javafx.event.ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    public void closewindow(javafx.event.ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.close();
    }

    // ======================
    // SCENE SWITCH HELPERS
    // ======================
    private Stage getStageFromEvent(javafx.event.ActionEvent event) {
        Object src = event.getSource();

        if (src instanceof Node n) {
            return (Stage) n.getScene().getWindow();
        }
        if (src instanceof MenuItem mi) {
            return (Stage) mi.getParentPopup().getOwnerWindow();
        }
        throw new IllegalArgumentException("Unknown event source: " + src);
    }

    private void switchScene(javafx.event.ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = getStageFromEvent(event);
            stage.setScene(new Scene(root));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
