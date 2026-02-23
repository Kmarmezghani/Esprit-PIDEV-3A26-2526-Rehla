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

import java.io.File;
import java.io.InputStream;
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

    private final PreferenceService preferenceService = new PreferenceService();
    private final GeminiRecommendationService geminiService = new GeminiRecommendationService();

    // =========================
    // ✅ WAITLIST: add service
    // =========================
    private final WaitlistService waitlistService = new WaitlistService();
    // =========================

    // ===== Local state =====
    private Set<Integer> recommendedIds = new HashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");
    private List<Activite> allActivities = new ArrayList<>();
    private static final int CURRENT_USER_ID = 1;

    // =========================
    // ✅✅✅ GLOBAL AI CACHE (static) + LIST SIGNATURE
    // =========================
    private static long lastAiFetchMs = 0;
    private static final long AI_CACHE_MS = 5 * 60 * 1000;

    private static Set<Integer> cachedRecommendedIds = new HashSet<>();
    private static String cachedProfileKey = "";

    private static String cachedActivitiesSignature = "";
    // =========================

    @FXML
    public void initialize() {
        reloadFromDB();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applySearchFilter());
        }
    }

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
        Image real = loadActivityImage(a.getImage());
        if (real != null) img.setImage(real);
        else {
            Image ph = loadPlaceholder();
            if (ph != null) img.setImage(ph);
        }
        img.setFitWidth(248);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        Label title = new Label(safe(a.getNom()));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #223f91;");

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

        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(248, 140);
        imagePane.setMinSize(248, 140);
        imagePane.setMaxSize(248, 140);

        imagePane.getChildren().add(img);

        if (isRecommended) {
            StackPane.setAlignment(recommendedBadge, Pos.TOP_LEFT);
            StackPane.setMargin(recommendedBadge, new Insets(10, 0, 0, 10));
            imagePane.getChildren().add(recommendedBadge);
        }

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
        infoRow.setAlignment(Pos.CENTER_LEFT);

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

        // =========================
        // ✅ WAITLIST: if full -> show "Join waitlist" instead of booking dialog
        // =========================
        Button bookBtn = null;
        Button waitBtn = null;

        if (hasGuide) {

            // If FULL => join waitlist
            if (isLimited && isFull) {
                waitBtn = new Button("Join waitlist");
                waitBtn.setStyle("""
                    -fx-background-color: #f59e0b;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-background-radius: 10;
                    -fx-padding: 10 18;
                    -fx-cursor: hand;
                """);

                // Optional: disable if already waiting
                boolean alreadyWaiting = false;
                try {
                    alreadyWaiting = waitlistService.isUserWaiting(CURRENT_USER_ID, a.getId());
                } catch (Exception ignored) {}

                if (alreadyWaiting) {
                    waitBtn.setText("On waitlist");
                    waitBtn.setDisable(true);
                    waitBtn.setStyle("""
                        -fx-background-color: #9ca3af;
                        -fx-text-fill: white;
                        -fx-font-weight: bold;
                        -fx-background-radius: 10;
                        -fx-padding: 10 18;
                        -fx-opacity: 0.85;
                    """);
                } else {
                    waitBtn.setOnAction(ev -> {
                        try {
                            waitlistService.joinWaitlist(CURRENT_USER_ID, a.getId());
                            toastSuccessWithAction(
                                    "Ajouté !",
                                    "Vous êtes sur la liste d'attente.",
                                    "Voir mes réservations",
                                    this::goToMyReservationsFromToast
                            );
                            reloadFromDB();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            toastError("Erreur", "Impossible de rejoindre la liste d'attente.");
                        }
                    });
                }

            } else {
                // Not full => keep your Book behavior unchanged
                bookBtn = new Button("Book");

                String normalStyle = """
                    -fx-background-color: #223f91;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-background-radius: 10;
                    -fx-padding: 10 18;
                    -fx-cursor: hand;
                """;
                bookBtn.setStyle(normalStyle);

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
        }
        // =========================

        VBox actionsBox = new VBox(4);
        actionsBox.setFillWidth(true);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRow.getChildren().add(detailsBtn);
        btnRow.getChildren().add(spacer);

        // ✅ WAITLIST: show waitBtn if exists else bookBtn
        if (waitBtn != null) btnRow.getChildren().add(waitBtn);
        else if (bookBtn != null) btnRow.getChildren().add(bookBtn);

        HBox placesRow = new HBox();
        placesRow.setAlignment(Pos.CENTER_RIGHT);
        placesRow.setPadding(new Insets(0, 6, 0, 0));
        placesRow.getChildren().add(placesText);

        if (placesText.isManaged()) actionsBox.getChildren().addAll(btnRow, placesRow);
        else actionsBox.getChildren().add(btnRow);

        card.getChildren().addAll(
                imagePane,
                title,
                destination,
                date,
                infoRow,
                actionsBox
        );

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

    private String buildActivitiesSignature(List<Activite> list) {
        if (list == null || list.isEmpty()) return "empty";

        List<Activite> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingInt(Activite::getId));

        StringBuilder sb = new StringBuilder();
        for (Activite a : copy) {
            sb.append(a.getId()).append("|");
            sb.append(safe(a.getNom())).append("|");
            sb.append(a.getPrix()).append("|");
            sb.append(a.getDateDebut() != null ? a.getDateDebut().toString() : "").append("|");
            sb.append(a.getDateFin() != null ? a.getDateFin().toString() : "").append("|");
            sb.append(safe(a.getStatus())).append(";");
        }
        return sb.toString();
    }

    public void reloadFromDB() {
        allActivities = activiteService.getDisponibles();

        if (cachedRecommendedIds != null && !cachedRecommendedIds.isEmpty()) {
            recommendedIds = new HashSet<>(cachedRecommendedIds);
        }

        applySearchFilter();

        long now = System.currentTimeMillis();

        String currentActivitiesSig = buildActivitiesSignature(allActivities);
        boolean listChanged = !currentActivitiesSig.equals(cachedActivitiesSignature);

        boolean cacheFresh = (now - lastAiFetchMs) < AI_CACHE_MS;
        if (cacheFresh && !listChanged && cachedRecommendedIds != null && !cachedRecommendedIds.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                Preference pref = preferenceService.getByPersonneId(CURRENT_USER_ID);
                if (pref == null) {
                    cachedRecommendedIds = new HashSet<>();
                    recommendedIds = new HashSet<>();
                    Platform.runLater(this::applySearchFilter);
                    return;
                }

                String profileKey = buildProfileText(pref).trim();

                long now2 = System.currentTimeMillis();
                boolean cacheFresh2 = (now2 - lastAiFetchMs) < AI_CACHE_MS;
                boolean samePrefs = profileKey.equals(cachedProfileKey);

                if (cacheFresh2 && samePrefs && !listChanged && cachedRecommendedIds != null && !cachedRecommendedIds.isEmpty()) {
                    recommendedIds = new HashSet<>(cachedRecommendedIds);
                    Platform.runLater(this::applySearchFilter);
                    return;
                }

                List<Activite> candidates = filterCandidatesByPreference(allActivities, pref);

                List<Integer> ids = geminiService.rankActivityIdsMax3(candidates, profileKey);
                recommendedIds = new HashSet<>(ids);

                cachedRecommendedIds = new HashSet<>(recommendedIds);
                cachedProfileKey = profileKey;
                cachedActivitiesSignature = currentActivitiesSig;
                lastAiFetchMs = System.currentTimeMillis();

                Platform.runLater(this::applySearchFilter);

            } catch (Exception e) {
                e.printStackTrace();
                if (cachedRecommendedIds != null) recommendedIds = new HashSet<>(cachedRecommendedIds);
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
            if (out.size() >= 18) break;
        }
        return out;
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
    @FXML public void goToDestinations(ActionEvent event) { }
    @FXML public void goToPosts(ActionEvent event) { switchScene(event, "/Frontoffice/PostsPage.fxml"); }
    @FXML public void goToactivities(ActionEvent event) { }
    @FXML public void goToMyProfile(ActionEvent event) { }
    @FXML public void goToMyPosts(ActionEvent event) { }

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

    @FXML public void closewindow(ActionEvent event) { getStageFromEvent(event).close(); }
    @FXML public void minwindow(ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());
    }

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

    private Image loadActivityImage(String path) {
        if (path == null || path.isBlank()) return null;

        try {
            String p = path.trim();

            if (p.startsWith("http://") || p.startsWith("https://")) {
                return new Image(p, true);
            }

            if (p.startsWith("file:/")) {
                return new Image(p, true);
            }

            if (p.startsWith("/")) {
                InputStream is = getClass().getResourceAsStream(p);
                if (is != null) return new Image(is);
                Image fs = loadFromFileSmart(p.substring(1));
                if (fs != null) return fs;
                return null;
            }

            Image fs = loadFromFileSmart(p);
            if (fs != null) return fs;

        } catch (Exception ignored) {}

        return null;
    }

    private Image loadFromFileSmart(String p) {
        try {
            File file = new File(p);

            if (!file.exists()) {
                file = new File(System.getProperty("user.dir"), p);
            }

            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Image loadPlaceholder() {
        try {
            var is = getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png");
            return is != null ? new Image(is) : null;
        } catch (Exception e) {
            return null;
        }
    }
}