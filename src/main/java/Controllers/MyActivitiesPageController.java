package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MyActivitiesPageController {

    private static final int CURRENT_USER_ID = 5;

    @FXML private FlowPane myActivitiesFlowPane;
    @FXML private TextField searchField;
    @FXML private Label LBLcount;
    @FXML private Button btnNotif, btnProfile;
    @FXML private ContextMenu profileMenu;

    private final ActiviteService activiteService = new ActiviteService();

    @FXML
    public void initialize() {
        loadMyActivities("");
    }

    // ======================
    // SEARCH
    // ======================
    @FXML
    public void handleSearch(javafx.event.ActionEvent event) {
        String q = (searchField.getText() == null) ? "" : searchField.getText().trim();
        loadMyActivities(q);
    }

    // ======================
    // CREATE (POPUP)
    // ======================
    @FXML
    public void createActivity(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController ctrl = loader.getController();
            ctrl.setGuideId(CURRENT_USER_ID);

            Stage popup = new Stage();
            popup.initOwner(getStageFromEvent(event));
            popup.initModality(Modality.WINDOW_MODAL);
            popup.setTitle("Create Activity");
            popup.setResizable(false);
            popup.setScene(new Scene(root));

            popup.setOnHidden(e -> loadMyActivities(""));

            popup.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================
    // DATA
    // ======================
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
            List<Activite> all = activiteService.getByGuideId(CURRENT_USER_ID);
            if (all == null) return Collections.emptyList();

            String q = (query == null) ? "" : query.toLowerCase().trim();

            return all.stream()
                    .filter(a -> a != null)
                    .filter(a -> q.isBlank() || ((a.getNom() == null ? "" : a.getNom().toLowerCase()).contains(q)))
                    .toList();

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private VBox activityCard(Activite a) {
        VBox card = new VBox(10);
        card.setPrefWidth(270);
        card.setStyle("""
        -fx-background-color: white;
        -fx-background-radius: 16;
        -fx-padding: 14;
        -fx-border-color: #eef2ff;
        -fx-border-radius: 16;
        -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 3);
    """);

        javafx.scene.layout.HBox top = new javafx.scene.layout.HBox(8);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label title = new Label(a.getNom() == null ? "Untitled" : a.getNom());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("""
        -fx-background-color: transparent;
        -fx-text-fill: #d32f2f;
        -fx-font-size: 16;
        -fx-cursor: hand;
        -fx-padding: 2 6;
    """);

        deleteBtn.setOnAction(e -> {
            e.consume();
            confirmAndDelete(a);
        });

        top.getChildren().addAll(title, spacer, deleteBtn);

        Label price = new Label(String.format("💰 %.2f TND", a.getPrix()));
        price.setStyle("-fx-text-fill: #4a5f88; -fx-font-size: 13;");

        Label type = new Label("Type: " + (a.getTypeActivite() == null ? "—" : a.getTypeActivite()));
        type.setStyle("-fx-text-fill: #667085; -fx-font-size: 12;");

        card.getChildren().addAll(top, price, type);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openEditActivityPopup(a, card);
            }
        });

        return card;
    }

    // ✅ SAME CONFIRMATION STYLE AS REVIEW DELETE
    private boolean confirmDeleteActivity(Activite a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete activity");
        confirm.setHeaderText("Are you sure you want to delete your activity?");
        confirm.setContentText("This action cannot be undone.\n\nActivity: " + (a.getNom() == null ? "" : a.getNom()));

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBtn, cancelBtn);

        Optional<ButtonType> res = confirm.showAndWait();
        return res.isPresent() && res.get() == deleteBtn;
    }

    private void confirmAndDelete(Activite a) {
        // ✅ optional safety: if not yours => refuse
        if (a.getGuideId() != CURRENT_USER_ID) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not allowed");
            alert.setHeaderText(null);
            alert.setContentText("You can only delete your own activities.");
            alert.showAndWait();
            return;
        }

        // ✅ ask confirmation
        if (!confirmDeleteActivity(a)) return;

        try {
            activiteService.delete(a);
            loadMyActivities("");
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Error");
            err.setHeaderText(null);
            err.setContentText("Delete failed.");
            err.showAndWait();
        }
    }

    private void openEditActivityPopup(Activite activite, Node anyNodeInScene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController ctrl = loader.getController();
            ctrl.setGuideId(CURRENT_USER_ID);
            ctrl.setActiviteToEdit(activite);

            Stage popup = new Stage();
            popup.initOwner(anyNodeInScene.getScene().getWindow());
            popup.initModality(Modality.WINDOW_MODAL);
            popup.setTitle("Edit Activity");
            popup.setResizable(false);
            popup.setScene(new Scene(root));

            popup.setOnHidden(e -> loadMyActivities(""));

            popup.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================
    // NAVIGATION (KEEP FULLSCREEN)
    // ======================
    @FXML public void goToHome(javafx.event.ActionEvent e) { switchScene(e, "/Frontoffice/HomePage.fxml"); }
    @FXML public void goToDestinations(javafx.event.ActionEvent e) { }
    @FXML public void goToPosts(javafx.event.ActionEvent e) { switchScene(e, "/Frontoffice/PostsPage.fxml"); }
    @FXML public void goToactivities(javafx.event.ActionEvent e) { switchScene(e, "/Frontoffice/ActivitiesPage.fxml"); }

    @FXML public void goToMyProfile(javafx.event.ActionEvent e) { }
    @FXML public void goToMyPosts(javafx.event.ActionEvent e) { }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void goToMyActivities(javafx.event.ActionEvent e) { switchScene(e, "/Frontoffice/MyActivitiesPage.fxml"); }

    @FXML public void handleLogout(javafx.event.ActionEvent e) { }

    // ======================
    // WINDOW BUTTONS
    // ======================
    @FXML public void minwindow(javafx.event.ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(javafx.event.ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }
    @FXML public void closewindow(javafx.event.ActionEvent event) { getStageFromEvent(event).close(); }

    // ======================
    // HELPERS
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);

            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }

            root.applyCss();
            root.layout();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }




        @FXML
        public void openNotifications(ActionEvent event) {
            // TODO: ouvrir notifications (page/popup)
        }

    @FXML
    public void openProfileMenu(ActionEvent e) {
        if (profileMenu == null || btnProfile == null) return;
        if (profileMenu.isShowing()) profileMenu.hide();
        else profileMenu.show(btnProfile, javafx.geometry.Side.BOTTOM, 0, 6);
    }

}
