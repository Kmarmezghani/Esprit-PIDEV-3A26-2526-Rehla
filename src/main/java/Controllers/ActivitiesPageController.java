package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;
import services.InscriptionActiviteService;
import services.EmailService;
import services.PersonneService;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesPageController {

    @FXML private FlowPane activitiesFlowPane;
    @FXML private TextField searchField;

    private final ActiviteService activiteService = new ActiviteService();
    private final InscriptionActiviteService inscriptionService = new InscriptionActiviteService();

    private final PersonneService personneService = new PersonneService();

    private final EmailService emailService = new EmailService();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");
    private List<Activite> allActivities = new ArrayList<>();

    private static final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        reloadFromDB();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applySearchFilter());
        }
    }

    // ======================
    // SEARCH
    // ======================
    @FXML
    void handleSearch(ActionEvent event) {
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
            if (name.contains(q) || dest.contains(q)) filtered.add(a);
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

        // ================= IMAGE =================
        ImageView img = new ImageView();
        try {
            img.setImage(new Image(getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png")));
        } catch (Exception ignored) {}
        img.setFitWidth(248);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        // ================= TITLE =================
        Label title = new Label(safe(a.getNom()));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #223f91;");

        // ================= DESTINATION =================
        String dest = activiteService.getDestinationDisplayById(a.getDestinationId());
        Label destination = new Label("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));
        destination.setStyle("-fx-font-size: 13; -fx-text-fill: #4a5f88;");

        // ================= DATES =================
        String start = (a.getDateDebut() != null) ? a.getDateDebut().format(dtf) : "—";
        String end = (a.getDateFin() != null) ? a.getDateFin().format(dtf) : "—";
        Label date = new Label("🕒 " + start + "  →  " + end);
        date.setStyle("-fx-font-size: 12; -fx-text-fill: #5a6b8a;");

        // ================= PRICE =================
        Label price = new Label(String.format("💰 %.2f TND", a.getPrix()));
        price.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3A5BC7;");

        // ================= RATING =================
        Label rating = new Label("⭐ " + String.format("%.1f", a.getNoteMoyenne()));
        rating.setStyle("-fx-font-size: 13; -fx-text-fill: #223f91;");

        HBox infoRow = new HBox(12, price, rating);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        // ================= DETAILS BUTTON =================
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

        // ================= LIMIT/PLACES LOGIC =================
        Integer guideId = a.getGuideId();
        boolean hasGuide = (guideId != null && guideId > 0);

        Integer max = a.getMaxPlaces(); // null => admin => unlimited (no label)
        boolean isLimited = (max != null);

        int booked = 0;
        int left = -1;
        boolean isFull = false;

        if (isLimited) {
            booked = inscriptionService.countConfirmedByActiviteId(a.getId());
            left = Math.max(0, max - booked);
            isFull = (left == 0);
        }

        // ================= PLACES TEXT (ENGLISH) =================
        Label placesText = new Label();
        placesText.setVisible(false);
        placesText.setManaged(false);
        placesText.setStyle("-fx-font-size: 12; -fx-font-weight: 800;");

        if (isLimited) {
            placesText.setVisible(true);
            placesText.setManaged(true);

            if (isFull) {
                placesText.setText("Sold out");
                placesText.setTextFill(Color.web("#ef4444"));
            } else {
                placesText.setText(left + " spots left");

                // ✅ Better rules (fix your 1/3 case)
                // - 1 spot left => RED (always)
                // - 2 spots left => ORANGE
                // - else: ratio based
                if (left <= 1) {
                    placesText.setTextFill(Color.web("#ef4444")); // red
                } else if (left == 2) {
                    placesText.setTextFill(Color.web("#f59e0b")); // orange
                } else {
                    double ratio = left / (double) max;
                    if (ratio > 0.50) placesText.setTextFill(Color.web("#002b11"));   // green
                    else if (ratio > 0.20) placesText.setTextFill(Color.web("#f59e0b")); // orange
                    else placesText.setTextFill(Color.web("#ef4444"));                // red
                }
            }
        }

        // ================= BOOK BUTTON =================
        Button bookBtn = null;

        if (hasGuide) {
            bookBtn = new Button("Book");

            String normalStyle = """
                -fx-background-color: #223f91;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-padding: 10 18;
                -fx-cursor: hand;
            """;

            String fullStyle = """
                -fx-background-color: #9ca3af;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-padding: 10 18;
                -fx-opacity: 0.85;
                -fx-cursor: hand;
            """;

            if (isLimited && isFull) bookBtn.setStyle(fullStyle);
            else bookBtn.setStyle(normalStyle);

            Button finalBookBtn = bookBtn;
            finalBookBtn.setOnAction(e -> {

                // ✅ if full => show alert + do nothing
                if (isLimited) {
                    int b = inscriptionService.countConfirmedByActiviteId(a.getId());
                    int l = Math.max(0, max - b);
                    if (l == 0) {
                        showWarn("Sold out", "This activity is fully booked.");
                        reloadFromDB();
                        return;
                    }
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm booking");
                confirm.setHeaderText("Are you sure you want to book?");
                confirm.setContentText(
                        "Activity: " + safe(a.getNom()) + "\n" +
                                "Price: " + String.format("%.2f TND", a.getPrix()) + "\n\n" +
                                "Confirm your booking?"
                );

                ButtonType confirmBtn = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirm.getButtonTypes().setAll(confirmBtn, cancelBtn);

                confirm.showAndWait().ifPresent(response -> {
                    if (response == confirmBtn) {
                        try {
                            // safety re-check before insert
                            if (isLimited) {
                                int b2 = inscriptionService.countConfirmedByActiviteId(a.getId());
                                if (b2 >= max) {
                                    showWarn("Sold out", "This activity is fully booked.");
                                    reloadFromDB();
                                    return;
                                }
                            }

                            // ✅ DB booking
                            inscriptionService.book(CURRENT_USER_ID, a.getId(), a.getPrix());

                            // ✅ SEND EMAIL (real user email from DB)
                            String userEmail = personneService.getEmailById(CURRENT_USER_ID);
                            String userName = personneService.getFullNameById(CURRENT_USER_ID);

                            if (userEmail != null && !userEmail.isBlank()) {
                                try {
                                    emailService.sendBookingConfirmation(
                                            userEmail,
                                            userName,
                                            safe(a.getNom()),
                                            a.getPrix()
                                    );
                                } catch (MessagingException | UnsupportedEncodingException mailEx) {
                                    mailEx.printStackTrace();
                                    // don’t block booking if mail fails
                                    showWarn("Email not sent", "Booking done, but confirmation email failed.");
                                }
                            }

                            showInfo("Success", "Your booking has been confirmed.");
                            reloadFromDB();

                        } catch (SQLException ex) {
                            showWarn("Booking error", ex.getMessage());
                        }
                    }
                });
            });
        }

        // ================= ACTIONS LAYOUT =================
        VBox actionsBox = new VBox(4);
        actionsBox.setFillWidth(true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRow.getChildren().add(detailsBtn);
        btnRow.getChildren().add(spacer);
        if (bookBtn != null) btnRow.getChildren().add(bookBtn);

        // places text under the book button (right)
        HBox placesRow = new HBox();
        placesRow.setAlignment(Pos.CENTER_RIGHT);
        placesRow.setPadding(new Insets(0, 6, 0, 0));
        placesRow.getChildren().add(placesText);

        if (placesText.isManaged()) actionsBox.getChildren().addAll(btnRow, placesRow);
        else actionsBox.getChildren().add(btnRow);

        // ================= ADD EVERYTHING =================
        card.getChildren().addAll(img, title, destination, date, infoRow, actionsBox);

        // ================= HOVER =================
        card.setOnMouseEntered(e ->
                card.setStyle("""
                    -fx-background-color: #f7fbff;
                    -fx-background-radius: 15;
                    -fx-border-radius: 15;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 10, 0, 0, 3);
                    -fx-padding: 16;
                """)
        );

        card.setOnMouseExited(e ->
                card.setStyle("""
                    -fx-background-color: white;
                    -fx-background-radius: 15;
                    -fx-border-radius: 15;
                    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 10, 0, 0, 3);
                    -fx-padding: 16;
                """)
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

    private Stage getStageFromEvent(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Node n) return (Stage) n.getScene().getWindow();
        if (src instanceof MenuItem mi) return (Stage) mi.getParentPopup().getOwnerWindow();
        throw new IllegalArgumentException("Unknown event source: " + src);
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
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

    @FXML public void goToHome(ActionEvent event) { switchScene(event, "/Frontoffice/HomePage.fxml"); }
    @FXML public void goToDestinations(ActionEvent event) { /* TODO */ }
    @FXML public void goToPosts(ActionEvent event) { switchScene(event, "/Frontoffice/PostsPage.fxml"); }
    @FXML public void goToactivities(ActionEvent event) { /* already here */ }
    @FXML public void goToMyProfile(ActionEvent event) { /* TODO */ }
    @FXML public void goToMyPosts(ActionEvent event) { /* TODO */ }

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

    @FXML public void goToMyActivities(ActionEvent event) { switchScene(event, "/Frontoffice/MyActivitiesPage.fxml"); }

    @FXML
    public void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        alert.showAndWait();
    }

    @FXML public void closewindow(ActionEvent event) { getStageFromEvent(event).close(); }
    @FXML public void minwindow(ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
