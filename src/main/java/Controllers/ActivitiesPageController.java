package Controllers;

import javafx.application.Platform;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.Activite;
import models.Preference;
import services.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ActivitiesPageController {

    @FXML private FlowPane activitiesFlowPane;
    @FXML private TextField searchField;

    private final ActiviteService activiteService = new ActiviteService();
    private final ReservationService reservationService = new ReservationService();
    private final PersonneService personneService = new PersonneService();
    private final EmailService emailService = new EmailService();

    // ===== AI =====
    private final PreferenceService preferenceService = new PreferenceService();
    private final GeminiRecommendationService geminiService = new GeminiRecommendationService();
    private Set<Integer> recommendedIds = new HashSet<>();

    // Cache IA (évite lenteur)
    private long lastAiFetchMs = 0;
    private static final long AI_CACHE_MS = 5 * 60 * 1000; // 5 minutes
    // ============

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
    // RENDER
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

        // IMAGE
        ImageView img = new ImageView();
        try { img.setImage(new Image(getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png"))); }
        catch (Exception ignored) {}
        img.setFitWidth(248);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        // TITLE
        Label title = new Label(safe(a.getNom()));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #223f91;");

        // ===== AI BADGE =====
        boolean isRecommended = recommendedIds != null && recommendedIds.contains(a.getId());

        Label recommendedBadge = new Label("✅ Recommended");
        recommendedBadge.setStyle("""
            -fx-background-color: #dcfce7;
            -fx-text-fill: #166534;
            -fx-font-weight: 900;
            -fx-font-size: 11;
            -fx-padding: 4 10;
            -fx-background-radius: 999;
        """);

        HBox badgeRow = new HBox(recommendedBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        badgeRow.setVisible(isRecommended);
        badgeRow.setManaged(isRecommended);
        // ====================

        // DESTINATION
        String dest = activiteService.getDestinationDisplayById(a.getDestinationId());
        Label destination = new Label("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));
        destination.setStyle("-fx-font-size: 13; -fx-text-fill: #4a5f88;");

        // DATES
        String start = (a.getDateDebut() != null) ? a.getDateDebut().format(dtf) : "—";
        String end = (a.getDateFin() != null) ? a.getDateFin().format(dtf) : "—";
        Label date = new Label("🕒 " + start + "  →  " + end);
        date.setStyle("-fx-font-size: 12; -fx-text-fill: #5a6b8a;");

        // PRICE + RATING
        Label price = new Label(String.format("💰 %.2f TND", a.getPrix()));
        price.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3A5BC7;");

        Label rating = new Label("⭐ " + String.format("%.1f", a.getNoteMoyenne()));
        rating.setStyle("-fx-font-size: 13; -fx-text-fill: #223f91;");

        HBox infoRow = new HBox(12, price, rating);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        // DETAILS
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

        // LIMIT/PLACES
        Integer guideId = a.getGuideId();
        boolean hasGuide = (guideId != null && guideId > 0);

        Integer max = a.getMaxPlaces();
        boolean isLimited = (max != null);

        int left = -1;
        boolean isFull = false;

        if (isLimited) {
            try {
                int booked = reservationService.sumTicketsConfirmedByActiviteId(a.getId());
                left = Math.max(0, max - booked);
                isFull = (left == 0);
            } catch (SQLException ex) {
                ex.printStackTrace();
                left = max;
                isFull = false;
            }
        }

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
                if (left <= 1) placesText.setTextFill(Color.web("#ef4444"));
                else if (left == 2) placesText.setTextFill(Color.web("#f59e0b"));
                else placesText.setTextFill(Color.web("#002b11"));
            }
        }

        // BOOK (ton code inchangé)
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
                int available = Integer.MAX_VALUE;

                if (isLimited) {
                    try {
                        int taken = reservationService.sumTicketsConfirmedByActiviteId(a.getId());
                        available = Math.max(0, max - taken);
                        if (available == 0) {
                            toastWarn("Sold out", "This activity is fully booked.");
                            reloadFromDB();
                            return;
                        }
                    } catch (SQLException ex) {
                        toastError("Error", ex.getMessage());
                        return;
                    }
                }

                // (reste inchangé)...
                Dialog<Integer> dialog = new Dialog<>();
                dialog.setTitle("Book tickets");
                dialog.setHeaderText(null);

                DialogPane pane = dialog.getDialogPane();
                pane.setPrefWidth(420);
                pane.setStyle("""
                    -fx-background-color: white;
                    -fx-padding: 18;
                    -fx-font-family: "Segoe UI";
                """);

                String css = """
                .dialog-pane .button-bar .button {
                    -fx-background-radius: 10;
                    -fx-padding: 10 16;
                    -fx-font-weight: 700;
                    -fx-cursor: hand;
                }
                .dialog-pane .button-bar .button:default {
                    -fx-background-color: #223f91;
                    -fx-text-fill: white;
                }
                .dialog-pane .button-bar .button:cancel {
                    -fx-background-color: #eef2ff;
                    -fx-text-fill: #223f91;
                }
                """;
                pane.getStylesheets().add("data:text/css," + css.replace("\n", "%0A").replace(" ", "%20"));

                Label hTitle = new Label("Book tickets");
                hTitle.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #111827;");
                Label hSub = new Label("Choose how many tickets you want");
                hSub.setStyle("-fx-font-size: 12.5; -fx-text-fill: #6b7280;");

                VBox header = new VBox(4, hTitle, hSub);
                header.setPadding(new Insets(0, 0, 10, 0));

                Label actName = new Label(safe(a.getNom()));
                actName.setStyle("-fx-font-size: 15; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

                Label priceLbl2 = new Label("Price");
                priceLbl2.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");
                Label priceVal2 = new Label(String.format("%.2f TND", a.getPrix()));
                priceVal2.setStyle("-fx-font-size: 13; -fx-font-weight: 900; -fx-text-fill: #111827;");

                Label availLbl2 = new Label("Available");
                availLbl2.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");
                Label availVal2 = new Label(isLimited ? String.valueOf(available) : "Unlimited");

                String badgeStyle = """
                    -fx-font-size: 12;
                    -fx-font-weight: 900;
                    -fx-padding: 4 10;
                    -fx-background-radius: 999;
                """;
                if (isLimited && available <= 2) {
                    availVal2.setStyle(badgeStyle + "-fx-text-fill: #92400e; -fx-background-color: #ffedd5;");
                } else {
                    availVal2.setStyle(badgeStyle + "-fx-text-fill: #065f46; -fx-background-color: #d1fae5;");
                }

                HBox row1 = new HBox(10, priceLbl2, new Region(), availLbl2);
                HBox.setHgrow(row1.getChildren().get(1), Priority.ALWAYS);

                HBox row2 = new HBox(10, priceVal2, new Region(), availVal2);
                HBox.setHgrow(row2.getChildren().get(1), Priority.ALWAYS);

                VBox infoCard = new VBox(8, actName, row1, row2);
                infoCard.setStyle("""
                    -fx-background-color: #f8fafc;
                    -fx-padding: 14;
                    -fx-background-radius: 14;
                    -fx-border-radius: 14;
                    -fx-border-color: #e5e7eb;
                """);

                Label qtyLbl = new Label("Quantity");
                qtyLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #111827; -fx-font-weight: 900;");

                int maxSpinner = isLimited ? Math.min(available, 20) : 20;
                Spinner<Integer> sp = new Spinner<>(1, Math.max(1, maxSpinner), 1);
                sp.setEditable(true);
                sp.setPrefWidth(130);

                Label totalLbl = new Label();
                totalLbl.setStyle("-fx-font-size: 13; -fx-font-weight: 900; -fx-text-fill: #111827;");

                Runnable updateTotal = () -> {
                    int q = sp.getValue();
                    totalLbl.setText("Total: " + String.format("%.2f TND", a.getPrix() * q));
                };
                updateTotal.run();
                sp.valueProperty().addListener((obs, ov, nv) -> updateTotal.run());

                HBox qtyRow = new HBox(12, qtyLbl, new Region(), sp);
                HBox.setHgrow(qtyRow.getChildren().get(1), Priority.ALWAYS);
                qtyRow.setAlignment(Pos.CENTER_LEFT);

                HBox totalRow = new HBox(totalLbl);
                totalRow.setAlignment(Pos.CENTER_RIGHT);

                VBox content = new VBox(12, header, infoCard, qtyRow, totalRow);
                content.setPadding(new Insets(0, 0, 6, 0));

                ButtonType bookType = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
                pane.getButtonTypes().setAll(bookType, ButtonType.CANCEL);
                pane.setContent(content);

                dialog.setResultConverter(btn -> btn == bookType ? sp.getValue() : null);

                dialog.showAndWait().ifPresent(qty -> {
                    if (qty == null || qty <= 0) return;

                    try {
                        reservationService.bookWithQty(
                                CURRENT_USER_ID,
                                a.getId(),
                                qty,
                                a.getPrix(),
                                a.getDestinationId()
                        );

                        toastSuccessWithAction(
                                "Booked!",
                                "Reservation created for " + qty + " ticket(s).",
                                "View my bookings",
                                this::goToMyReservationsFromToast
                        );

                        reloadFromDB();

                        String userEmail = personneService.getEmailById(CURRENT_USER_ID);
                        String userName  = personneService.getFullNameById(CURRENT_USER_ID);

                        if (userEmail != null && !userEmail.isBlank()) {
                            new Thread(() -> {
                                try {
                                    emailService.sendBookingConfirmation(
                                            userEmail,
                                            userName,
                                            safe(a.getNom()),
                                            a.getPrix() * qty
                                    );
                                } catch (Exception mailEx) {
                                    mailEx.printStackTrace();
                                    Platform.runLater(() ->
                                            toastWarn("Email not sent", "Booking done, but email failed.")
                                    );
                                }
                            }).start();
                        }

                    } catch (SQLException ex) {
                        toastError("Booking error", ex.getMessage());
                    }
                });
            });
        }

        // ACTIONS LAYOUT
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

        HBox placesRow = new HBox();
        placesRow.setAlignment(Pos.CENTER_RIGHT);
        placesRow.setPadding(new Insets(0, 6, 0, 0));
        placesRow.getChildren().add(placesText);

        if (placesText.isManaged()) actionsBox.getChildren().addAll(btnRow, placesRow);
        else actionsBox.getChildren().add(btnRow);

        // ADD ALL (badgeRow ajouté)
        card.getChildren().addAll(
                badgeRow,
                img,
                title,
                destination,
                date,
                infoRow,
                actionsBox
        );

        // HOVER
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
            toastError("Error", "Cannot open activity details.");
        }
    }

    // ===== DATA (IA rapide + cache) =====
    public void reloadFromDB() {
        allActivities = activiteService.getDisponibles();

        // Affichage immédiat
        applySearchFilter();

        // Cache: si déjà calculé récemment, on ne relance pas l'IA
        long now = System.currentTimeMillis();
        if (now - lastAiFetchMs < AI_CACHE_MS) {
            return;
        }
        lastAiFetchMs = now;

        // IA en background
        new Thread(() -> {
            try {
                Preference pref = preferenceService.getByPersonneId(CURRENT_USER_ID);
                if (pref == null) {
                    recommendedIds = new HashSet<>();
                    return;
                }

                String profileText = buildProfileText(pref);

                // ✅ Préfiltre = plus rapide + plus logique
                List<Activite> candidates = filterCandidatesByPreference(allActivities, pref);

                List<Integer> ids = geminiService.rankActivityIdsMax3(candidates, profileText);
                recommendedIds = new HashSet<>(ids);

                Platform.runLater(this::applySearchFilter);

            } catch (Exception e) {
                e.printStackTrace();
                recommendedIds = new HashSet<>();
                Platform.runLater(this::applySearchFilter);
            }
        }).start();
    }

    private String buildProfileText(Preference p) {
        return """
            budgetMin: %s
            budgetMax: %s
            typesVoyage: %s
            centresInteret: %s
            """.formatted(
                String.valueOf(p.getBudgetMin()),
                String.valueOf(p.getBudgetMax()),
                safe(p.getTypesVoyage()),
                safe(p.getCentresInteret())
        );
    }

    // ✅ garde seulement des candidats cohérents (rapide)
    private List<Activite> filterCandidatesByPreference(List<Activite> list, Preference pref) {
        if (list == null) return List.of();

        Double min = pref.getBudgetMin();
        Double max = pref.getBudgetMax();

        List<Activite> out = new ArrayList<>();
        for (Activite a : list) {
            if (!"DISPONIBLE".equalsIgnoreCase(safe(a.getStatus()))) continue;

            double price = a.getPrix();
            if (min != null && price < min) continue;
            if (max != null && price > max) continue;

            out.add(a);
            if (out.size() >= 18) break; // ✅ même limite que Gemini
        }
        return out;
    }
    // ====================================

    // NAVIGATION (inchangé)
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
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToMyReservationsFromToast() {
        try {
            Stage stage = (Stage) activitiesFlowPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception ex) {
            ex.printStackTrace();
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

    // WINDOW BUTTONS
    @FXML public void closewindow(ActionEvent event) { getStageFromEvent(event).close(); }
    @FXML public void minwindow(ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }

    // TOAST
    private Stage getStage() {
        if (activitiesFlowPane == null || activitiesFlowPane.getScene() == null) return null;
        return (Stage) activitiesFlowPane.getScene().getWindow();
    }

    private void toastWarn(String title, String msg) { Toast.show(getStage(), Toast.Type.WARNING, title, msg); }
    private void toastError(String title, String msg) { Toast.show(getStage(), Toast.Type.ERROR, title, msg); }
    private void toastSuccessWithAction(String title, String msg, String actionText, Runnable action) {
        Toast.show(getStage(), Toast.Type.SUCCESS, title, msg, actionText, action);
    }

    private String safe(String s) { return s == null ? "" : s; }
}