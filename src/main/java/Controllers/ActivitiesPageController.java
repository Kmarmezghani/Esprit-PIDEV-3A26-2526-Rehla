package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesPageController {

    @FXML private FlowPane activitiesFlowPane;
    @FXML private TextField searchField;

    private final ActiviteService activiteService = new ActiviteService();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");

    private List<Activite> allActivities = new ArrayList<>();

    @FXML
    public void initialize() {
        reloadFromDB();
    }

    // ======================
    // SEARCH
    // ======================
    @FXML
    void handleSearch(javafx.event.ActionEvent event) {
        applySearchFilter();
    }

    private void applySearchFilter() {
        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        if (q.isEmpty()) {
            renderActivities(allActivities);
            return;
        }

        List<Activite> filtered = new ArrayList<>();
        for (Activite a : allActivities) {
            String name = safe(a.getNom()).toLowerCase();
            String dest = safe(activiteService.getDestinationDisplayById(a.getDestinationId())).toLowerCase();

            if (name.contains(q) || dest.contains(q)) {
                filtered.add(a);
            }
        }

        renderActivities(filtered);
    }

    // ======================
    // RENDER CARDS
    // ======================
    private void renderActivities(List<Activite> list) {
        activitiesFlowPane.getChildren().clear();
        for (Activite a : list) {
            activitiesFlowPane.getChildren().add(createActivityCard(a));
        }
    }

    private VBox createActivityCard(Activite a) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 15;
            -fx-border-radius: 15;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 10, 0, 0, 3);
            -fx-padding: 16;
        """);

        ImageView img = new ImageView();
        try {
            img.setImage(new Image(getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png")));
        } catch (Exception ignored) {}
        img.setFitWidth(248);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        Label title = new Label(safe(a.getNom()));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #223f91;");

        String dest = activiteService.getDestinationDisplayById(a.getDestinationId());
        Label destination = new Label("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));
        destination.setStyle("-fx-font-size: 13; -fx-text-fill: #4a5f88;");

        String start = (a.getDateDebut() != null) ? a.getDateDebut().format(dtf) : "—";
        String end = (a.getDateFin() != null) ? a.getDateFin().format(dtf) : "—";
        Label date = new Label("🕒 " + start + "  →  " + end);
        date.setStyle("-fx-font-size: 12; -fx-text-fill: #5a6b8a;");

        Label price = new Label(String.format("💰 %.2f TND", a.getPrix()));
        price.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3A5BC7;");

        Label rating = new Label("⭐ " + String.format("%.1f", a.getNoteMoyenne()));
        rating.setStyle("-fx-font-size: 13; -fx-text-fill: #223f91;");

        HBox infoRow = new HBox(12, price, rating);

        Button detailsBtn = new Button("View details");
        detailsBtn.setStyle("""
            -fx-background-color: #3A5BC7;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 10;
            -fx-padding: 10 18;
            -fx-cursor: hand;
        """);
        detailsBtn.setOnAction(e -> openActivityDetails(a));

        Button bookBtn = new Button("Book");
        bookBtn.setStyle("""
            -fx-background-color: #223f91;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 10;
            -fx-padding: 10 18;
            -fx-cursor: hand;
        """);
        bookBtn.setOnAction(e -> showInfo("Booking", "Booking for: " + safe(a.getNom())));

        HBox buttons = new HBox(12, detailsBtn, bookBtn);

        card.getChildren().addAll(img, title, destination, date, infoRow, buttons);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #f7fbff;"));
        card.setOnMouseExited(e ->
                card.setStyle(card.getStyle().replace("-fx-background-color: #f7fbff;", "-fx-background-color: white;"))
        );

        return card;
    }

    private void openActivityDetails(Activite a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivityDetailsPage.fxml"));
            Parent root = loader.load();

            ActivityDetailsController controller = loader.getController();
            controller.setActivity(a);

            Stage stage = (Stage) activitiesFlowPane.getScene().getWindow();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception ex) {
            ex.printStackTrace();
            showInfo("Error", "Cannot open activity details.");
        }
    }

    // ======================
    // DATA
    // ======================
    public void reloadFromDB() {
        allActivities = activiteService.getDisponibles();
        applySearchFilter();
    }

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

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void goToHome(javafx.event.ActionEvent event) { switchScene(event, "/Frontoffice/HomePage.fxml"); }
    @FXML public void goToDestinations(javafx.event.ActionEvent event) { /* TODO */ }
    @FXML public void goToPosts(javafx.event.ActionEvent event) { switchScene(event, "/Frontoffice/PostsPage.fxml"); }
    @FXML public void goToactivities(javafx.event.ActionEvent event) { /* already here */ }

    @FXML public void goToMyProfile(javafx.event.ActionEvent event) { /* TODO */ }
    @FXML public void goToMyPosts(javafx.event.ActionEvent event) { /* TODO */ }
    @FXML public void goToMyReservations(javafx.event.ActionEvent event) { /* TODO */ }
    @FXML public void goToMyActivities(javafx.event.ActionEvent event) { switchScene(event, "/Frontoffice/MyActivitiesPage.fxml"); }

    @FXML
    public void handleLogout(javafx.event.ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) showInfo("Logged Out", "You have been successfully logged out!");
        });
    }

    // ======================
    // WINDOW BUTTONS
    // ======================
    @FXML public void closewindow(javafx.event.ActionEvent event) { getStageFromEvent(event).close(); }
    @FXML public void minwindow(javafx.event.ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(javafx.event.ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }

    // ======================
    // UTILS
    // ======================
    private String safe(String s) { return s == null ? "" : s; }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
