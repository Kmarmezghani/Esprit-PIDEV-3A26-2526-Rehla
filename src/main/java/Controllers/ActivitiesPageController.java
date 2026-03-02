package Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import models.Personne;
import models.Preference;
import models.Reservation;
import services.*;
import util.Session;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ActivitiesPageController {

    @FXML private FlowPane activitiesFlowPane;
    @FXML private TextField searchField;

    @FXML private Button btnProfile;
    @FXML private Button btnNotif;
    @FXML private ContextMenu profileMenu;
    @FXML private Label lblNotifCount;

    private final NotificationService notificationService = new NotificationService();

    private final ActiviteService activiteService = new ActiviteService();
    private final ReservationService reservationService = new ReservationService();
    private final PersonneService personneService = new PersonneService();
    private final EmailService emailService = new EmailService();

    private final PreferenceService preferenceService = new PreferenceService();
    private final GeminiRecommendationService geminiService = new GeminiRecommendationService();

    // ✅ WAITLIST
    private final WaitlistService waitlistService = new WaitlistService();

    // ===== Local state =====
    private Set<Integer> recommendedIds = new HashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");
    private List<Activite> allActivities = new ArrayList<>();

    private boolean flashMode = false;

    private final javafx.animation.Timeline flashTicker =
            new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> refreshFlashUI())
            );

    Personne CURRENT_USER = Session.getCurrentUser();
    int CURRENT_USER_ID=CURRENT_USER.getId();
    // ===== Booking context (vient du calendrier / BookingType) =====
    private boolean addMode = false;
    private Reservation reservationContext = null;
    private LocalDate selectedDate = null;

    public void setAddMode(boolean addMode) {
        this.addMode = addMode;
        maybeApplyContextReload();
    }

    public void setReservation(Reservation r) {
        this.reservationContext = r;
        maybeApplyContextReload();
    }

    public void setSelectedDate(LocalDate d) {
        this.selectedDate = d;
        maybeApplyContextReload();
    }

    private boolean hasContext() {
        return addMode && reservationContext != null && selectedDate != null;
    }

    private boolean isSelectedDateWithinReservation() {
        if (reservationContext == null || reservationContext.getDateDebut() == null || reservationContext.getDateFin() == null)
            return false;

        LocalDate start = reservationContext.getDateDebut().toLocalDate();
        LocalDate end   = reservationContext.getDateFin().toLocalDate();

        return !selectedDate.isBefore(start) && !selectedDate.isAfter(end);
    }

    private boolean doesActivityCoverSelectedDate(Activite a) {
        if (a == null || a.getDateDebut() == null || a.getDateFin() == null) return false;

        LocalDate aStart = a.getDateDebut().toLocalDate();
        LocalDate aEnd   = a.getDateFin().toLocalDate();

        return !selectedDate.isBefore(aStart) && !selectedDate.isAfter(aEnd);
    }

    /**
     * IMPORTANT :
     * initialize() s’exécute avant que BookingTypeController appelle tes setters.
     * Donc on refresh dès que le contexte est reçu.
     */
    private void maybeApplyContextReload() {
        if (!hasContext()) return;

        Platform.runLater(() -> {
            // si la page est déjà en mode flash, garde flash, sinon normal
            if (flashMode) showFlashSales(null);
            else reloadFromDB();
        });
    }

    /** Applique le filtre de contexte sur une liste donnée */
    private List<Activite> applyContextFilter(List<Activite> input) {
        if (input == null) return new ArrayList<>();

        if (!hasContext()) return input;

        if (!isSelectedDateWithinReservation()) {
            Platform.runLater(() -> toastWarn("Date invalide",
                    "La date choisie n’appartient pas à l’intervalle de la réservation."));
            return new ArrayList<>();
        }

        List<Activite> out = new ArrayList<>();
        for (Activite a : input) {
            if (doesActivityCoverSelectedDate(a)) out.add(a);
        }
        return out;
    }

    private static long lastAiFetchMs = 0;
    private static final long AI_CACHE_MS = 5 * 60 * 1000;

    private static Set<Integer> cachedRecommendedIds = new HashSet<>();
    private static String cachedProfileKey = "";
    private static String cachedActivitiesSignature = "";
    // =========================

    @FXML
    private MenuItem menuMyActivities;
    @FXML
    public void initialize() {
        reloadFromDB();        // charge liste normale
        refreshNotifCount();
        Personne u = Session.getCurrentUser();
        boolean isGuide = (u != null) && "GUIDE".equalsIgnoreCase(u.getRole());

        if (!isGuide) {
            profileMenu.getItems().remove(menuMyActivities); // pas d’espace vide
        }

        flashTicker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        flashTicker.play();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> applySearchFilter());
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        applySearchFilter();
    }

    private void applySearchFilter() {
        // ✅ toujours filtrer sur allActivities (qui contient soit FLASH soit NORMAL selon mode)
        List<Activite> base = (allActivities == null) ? List.of() : allActivities;

        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        if (q.isEmpty()) {
            renderActivities(base);
            return;
        }

        List<Activite> filtered = new ArrayList<>();
        for (Activite a : base) {
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

        final double CARD_W = 280;
        final double INNER_W = CARD_W - 32;

        VBox card = new VBox(10);
        card.setPrefWidth(CARD_W);
        card.setMinWidth(CARD_W);
        card.setMaxWidth(CARD_W);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 10, 0, 0, 3);
            -fx-padding: 16;
        """);

        ImageView img = new ImageView();
        Image real = loadActivityImage(a.getImage());
        Image ph = loadPlaceholder();
        img.setImage(real != null ? real : ph);
        img.setFitWidth(248);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);

        final boolean isRecommended = recommendedIds != null && recommendedIds.contains(a.getId());
        final boolean isFlash = isFlashActivity(a);

        Label recommendedBadge = new Label("✅ Recommended");
        recommendedBadge.setStyle("""
            -fx-background-color: #dcfce7;
            -fx-text-fill: #166534;
            -fx-font-weight: 900;
            -fx-font-size: 11;
            -fx-padding: 4 10;
            -fx-background-radius: 999;
        """);

        Label flashRibbon = new Label("🔥 FLASH");
        flashRibbon.setVisible(isFlash);
        flashRibbon.setManaged(isFlash);
        flashRibbon.setStyle("""
            -fx-background-color: #ea580c;
            -fx-text-fill: white;
            -fx-font-weight: 900;
            -fx-font-size: 11;
            -fx-padding: 6 12;
            -fx-background-radius: 999;
        """);

        StackPane imagePane = new StackPane(img);
        imagePane.setPrefSize(248, 140);
        imagePane.setMinSize(248, 140);
        imagePane.setMaxSize(248, 140);

        if (isRecommended) {
            StackPane.setAlignment(recommendedBadge, Pos.TOP_LEFT);
            StackPane.setMargin(recommendedBadge, new Insets(10, 0, 0, 10));
            imagePane.getChildren().add(recommendedBadge);
        }
        if (isFlash) {
            StackPane.setAlignment(flashRibbon, Pos.TOP_RIGHT);
            StackPane.setMargin(flashRibbon, new Insets(10, 10, 0, 0));
            imagePane.getChildren().add(flashRibbon);
        }

        Label title = new Label(safe(a.getNom()));
        title.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #1e3a8a;");
        title.setMaxWidth(INNER_W);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);

        String dest = activiteService.getDestinationDisplayById(a.getDestinationId());
        Label destination = new Label("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));
        destination.setStyle("-fx-font-size: 13; -fx-text-fill: #475569; -fx-font-weight: 700;");
        destination.setMaxWidth(INNER_W);
        destination.setTextOverrun(OverrunStyle.ELLIPSIS);

        String start = (a.getDateDebut() != null) ? a.getDateDebut().format(dtf) : "—";
        String end = (a.getDateFin() != null) ? a.getDateFin().format(dtf) : "—";
        Label date = new Label("🕒 " + start + "  →  " + end);
        date.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b; -fx-font-weight: 600;");
        date.setMaxWidth(INNER_W);
        date.setTextOverrun(OverrunStyle.ELLIPSIS);

        final double normalPrice = a.getPrix();
        final double flashPrice = (isFlash && a.getFlashPrice() != null) ? a.getFlashPrice() : normalPrice;
        final double unitPrice = flashPrice;

        Label priceNormalLbl = new Label(String.format("%.2f TND", normalPrice));
        priceNormalLbl.setWrapText(false);
        priceNormalLbl.setMinWidth(Region.USE_PREF_SIZE);
        priceNormalLbl.setStyle("""
            -fx-font-size: 14;
            -fx-font-weight: 900;
            -fx-text-fill: #1d4ed8;
        """);

        Label saleBadge = new Label("🔥 SALE");
        saleBadge.setVisible(isFlash);
        saleBadge.setManaged(isFlash);
        saleBadge.setWrapText(false);
        saleBadge.setMinWidth(Region.USE_PREF_SIZE);
        saleBadge.setStyle("""
            -fx-background-color: #fff7ed;
            -fx-text-fill: #c2410c;
            -fx-font-weight: 900;
            -fx-font-size: 12;
            -fx-padding: 4 12;
            -fx-background-radius: 999;
            -fx-border-color: #fdba74;
            -fx-border-radius: 999;
        """);

        Label priceFlashLbl = new Label(String.format("%.2f TND", flashPrice));
        priceFlashLbl.setVisible(isFlash);
        priceFlashLbl.setManaged(isFlash);
        priceFlashLbl.setWrapText(false);
        priceFlashLbl.setMinWidth(Region.USE_PREF_SIZE);
        priceFlashLbl.setStyle("""
            -fx-font-size: 18;
            -fx-font-weight: 900;
            -fx-text-fill: #ea580c;
        """);

        Label flashExpireLbl = new Label();
        flashExpireLbl.setVisible(isFlash);
        flashExpireLbl.setManaged(isFlash);
        flashExpireLbl.setWrapText(false);
        flashExpireLbl.setMinWidth(Region.USE_PREF_SIZE);
        flashExpireLbl.setStyle("""
            -fx-font-size: 12;
            -fx-font-weight: 900;
            -fx-text-fill: #b45309;
        """);

        if (isFlash) {
            priceNormalLbl.setStyle("""
                -fx-font-size: 13;
                -fx-font-weight: 900;
                -fx-text-fill: #94a3b8;
                -fx-strikethrough: true;
            """);
            startFlashCountdown(flashExpireLbl, a.getFlashExpiresAt(), card);
        }

        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setMaxWidth(INNER_W - 70);

        if (isFlash) {
            HBox flashRow = new HBox(10, saleBadge, priceFlashLbl);
            flashRow.setAlignment(Pos.CENTER_LEFT);
            priceBox.getChildren().addAll(priceNormalLbl, flashRow, flashExpireLbl);
        } else {
            priceBox.getChildren().add(priceNormalLbl);
        }

        Label ratingBadge = new Label("★ " + String.format("%.1f", a.getNoteMoyenne()));
        ratingBadge.setWrapText(false);
        ratingBadge.setMinWidth(58);
        ratingBadge.setPrefWidth(58);
        ratingBadge.setMaxWidth(58);
        ratingBadge.setAlignment(Pos.CENTER);
        ratingBadge.setStyle("""
            -fx-background-color: #eef2ff;
            -fx-text-fill: #1e3a8a;
            -fx-font-weight: 900;
            -fx-font-size: 12;
            -fx-padding: 4 10;
            -fx-background-radius: 999;
            -fx-border-color: #c7d2fe;
            -fx-border-radius: 999;
        """);

        Region spacerInfo = new Region();
        HBox.setHgrow(spacerInfo, Priority.ALWAYS);

        HBox infoRow = new HBox(12, priceBox, spacerInfo, ratingBadge);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setPrefWidth(INNER_W);
        infoRow.setMaxWidth(INNER_W);
        infoRow.setMinWidth(INNER_W);

        Button detailsBtn = new Button("View details");
        detailsBtn.setStyle("""
            -fx-background-color: #3A5BC7;
            -fx-text-fill: white;
            -fx-font-weight: 900;
            -fx-background-radius: 12;
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
        placesText.setStyle("-fx-font-size: 12; -fx-font-weight: 900;");

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
                else placesText.setTextFill(Color.web("#065f46"));
            }
        }

        Button bookBtn = null;
        Button waitBtn = null;

        if (hasGuide) {

            if (isLimited && isFull) {

                waitBtn = new Button("Join waitlist");
                waitBtn.setStyle("""
                    -fx-background-color: #f59e0b;
                    -fx-text-fill: white;
                    -fx-font-weight: 900;
                    -fx-background-radius: 12;
                    -fx-padding: 10 18;
                    -fx-cursor: hand;
                """);

                Button finalWaitBtn = waitBtn;
                finalWaitBtn.setOnAction(ev -> {
                    finalWaitBtn.setDisable(true);
                    try {
                        if (waitlistService.isUserWaiting(CURRENT_USER_ID, a.getId())) {
                            toastWarn("Info", "You are already on the waitlist.");
                            return;
                        }

                        waitlistService.joinWaitlist(CURRENT_USER_ID, a.getId());

                        toastSuccessWithAction(
                                "Added!",
                                "You are now on the waitlist.",
                                "View my bookings",
                                this::goToMyReservationsFromToast
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        finalWaitBtn.setDisable(false);
                        toastError("Error", "Unable to join the waitlist.");
                    }
                });

            } else {

                bookBtn = new Button(isFlash ? "Book (Sale)" : "Book");
                bookBtn.setStyle(isFlash ? """
                    -fx-background-color: #ea580c;
                    -fx-text-fill: white;
                    -fx-font-weight: 900;
                    -fx-background-radius: 12;
                    -fx-padding: 10 18;
                    -fx-cursor: hand;
                """ : """
                    -fx-background-color: #223f91;
                    -fx-text-fill: white;
                    -fx-font-weight: 900;
                    -fx-background-radius: 12;
                    -fx-padding: 10 18;
                    -fx-cursor: hand;
                """);

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

                    Label priceLbl2 = new Label(isFlash ? "Sale price" : "Price");
                    priceLbl2.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");
                    Label priceVal2 = new Label(String.format("%.2f TND", unitPrice));
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
                        int q2 = sp.getValue();
                        totalLbl.setText("Total: " + String.format("%.2f TND", unitPrice * q2));
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

                            Reservation created = null; // ✅ déclaré ici pour être visible après le if/else

                            if (hasContext()) {

                                if (!isSelectedDateWithinReservation()) {
                                    toastWarn("Date invalide", "La date choisie n’appartient pas à l’intervalle de la réservation.");
                                    return;
                                }
                                if (!doesActivityCoverSelectedDate(a)) {
                                    toastWarn("Hors date", "Cette activité n’est pas disponible pour la date sélectionnée.");
                                    return;
                                }

                                reservationService.addActivityTicketsToExistingReservation(
                                        reservationContext.getId(),
                                        CURRENT_USER_ID,
                                        a.getId(),
                                        qty,
                                        unitPrice,
                                        a.getDestinationId(),
                                        selectedDate
                                );

                                toastSuccessWithAction(
                                        "Ajouté !",
                                        qty + " ticket(s) ajoutés à votre réservation.",
                                        "Voir mes réservations",
                                        this::goToMyReservationsFromToast
                                );

                            } else {

                                created = reservationService.bookWithQtyReturnReservation(
                                        CURRENT_USER_ID,
                                        a.getId(),
                                        qty,
                                        unitPrice,
                                        a.getDestinationId()
                                );

                                toastSuccessWithAction(
                                        "Booked!",
                                        "Reservation created for " + qty + " ticket(s).",
                                        "View my bookings",
                                        this::goToMyReservationsFromToast
                                );
                            }

                            if (flashMode) showFlashSales(null);
                            else reloadFromDB();

                            // ✅ email seulement pour le cas "nouvelle réservation"
                            if (!hasContext()) {
                                String userEmail = personneService.getEmailById(CURRENT_USER_ID);
                                String userName  = personneService.getFullNameById(CURRENT_USER_ID);

                                if (userEmail != null && !userEmail.isBlank() && created != null) {

                                    Reservation finalCreated = created; // ✅ variable "effectively final" pour le Thread

                                    new Thread(() -> {
                                        try {
                                            emailService.sendBookingConfirmationWithPdf(
                                                    userEmail,
                                                    userName,
                                                    safe(a.getNom()),
                                                    unitPrice * qty,
                                                    finalCreated
                                            );
                                        } catch (Exception mailEx) {
                                            mailEx.printStackTrace();
                                            Platform.runLater(() ->
                                                    toastWarn("Email not sent", "Booking succeeded, but email failed.")
                                            );
                                        }
                                    }).start();
                                }
                            }

                        } catch (SQLException ex) {
                            toastError("Booking error", ex.getMessage());
                        }
                    });
                });
            }
        }

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRow.getChildren().addAll(detailsBtn, spacer);
        if (waitBtn != null) btnRow.getChildren().add(waitBtn);
        else if (bookBtn != null) btnRow.getChildren().add(bookBtn);

        HBox placesRow = new HBox(placesText);
        placesRow.setAlignment(Pos.CENTER_RIGHT);

        VBox actionsBox = new VBox(4, btnRow);
        if (placesText.isManaged()) actionsBox.getChildren().add(placesRow);

        card.getChildren().addAll(
                imagePane,
                title,
                destination,
                date,
                infoRow,
                actionsBox
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
            toastError("Erreur", "Impossible d'ouvrir le détail de l'activité.");
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
        try {
            activiteService.refreshFlashSales();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // ✅ normal mode list
        List<Activite> base = activiteService.getDisponibles();

        // ✅ filtre si contexte
        allActivities = applyContextFilter(base);

        if (hasContext() && allActivities.isEmpty()) {
            Platform.runLater(() -> toastWarn("Aucune activité", "Aucune activité disponible pour cette date."));
        }

        if (cachedRecommendedIds != null && !cachedRecommendedIds.isEmpty()) {
            recommendedIds = new HashSet<>(cachedRecommendedIds);
        }

        applySearchFilter();

        // ... le reste de ton code AI cache inchangé ...
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
        if (event == null) return getStage();

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
    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogAllPosts.fxml"));
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

    @FXML public void goToactivities(ActionEvent event) { }
    @FXML
    private void goToMyProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogProfileView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    getClass().getResource("/Frontoffice/css/blog_styles.css").toExternalForm()
            );

            Stage stage;

            if (event.getSource() instanceof javafx.scene.control.MenuItem menuItem) {
                stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
            } else {
                stage = (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();
            }

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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

    @FXML
    public void openProfileMenu(ActionEvent event) {
        if (profileMenu == null || btnProfile == null) return;

        if (profileMenu.isShowing()) {
            profileMenu.hide();
            return;
        }

        profileMenu.show(btnProfile, Side.BOTTOM, 0, 6);
    }

    @FXML
    public void openNotifications(ActionEvent event) {
        try {
            refreshNotifCount();

            List<NotificationService.NotifRow> notifs =
                    notificationService.getLatestUnread(CURRENT_USER_ID, 5);

            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-radius: 14; -fx-padding: 10; -fx-background-color: #f8fafc;");
            menu.getStyleClass().add("notifMenu");

            final double MENU_W = 320;

            if (notifs.isEmpty()) {
                Label lbl = new Label("Aucune notification");
                lbl.setWrapText(true);
                lbl.setPrefWidth(MENU_W);
                lbl.setMaxWidth(MENU_W);
                lbl.setAlignment(Pos.CENTER);
                lbl.setStyle("""
                    -fx-padding: 14 12;
                    -fx-text-fill: #6b7280;
                    -fx-font-size: 13px;
                """);
                menu.getItems().add(new CustomMenuItem(lbl, false));

            } else {
                for (var n : notifs) {

                    Label title = new Label(("WAITLIST_HOLD".equalsIgnoreCase(n.type) ? "⏳ Waitlist" : "🔔 Notification"));
                    title.setStyle("-fx-font-size: 12; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

                    Label msg = new Label(n.message);
                    msg.setWrapText(true);
                    msg.setMaxWidth(300);
                    msg.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");

                    Label time = new Label(n.createdAt != null ? n.createdAt.toString() : "");
                    time.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");

                    VBox card = new VBox(6, title, msg, time);
                    card.setStyle("""
                        -fx-background-color: white;
                        -fx-background-radius: 12;
                        -fx-padding: 12 12;
                        -fx-border-color: #e5e7eb;
                        -fx-border-radius: 12;
                    """);

                    CustomMenuItem it = new CustomMenuItem(card, true);

                    it.setOnAction(ev -> {
                        try {
                            notificationService.markRead(n.id);
                            refreshNotifCount();

                            if ("WAITLIST_HOLD".equalsIgnoreCase(n.type) && n.activiteId != null) {
                                openWaitlistHoldPopup(n.activiteId);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    menu.getItems().add(it);
                }

                menu.getItems().add(new SeparatorMenuItem());

                MenuItem mark = new MenuItem("Tout marquer comme lu");
                mark.setOnAction(e2 -> {
                    try {
                        notificationService.markAllRead(CURRENT_USER_ID);
                        refreshNotifCount();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                menu.getItems().add(mark);
            }

            var b = btnNotif.localToScreen(btnNotif.getBoundsInLocal());
            double x = b.getMaxX() - MENU_W;
            double y = b.getMaxY() + 8;

            x = Math.max(8, x);
            y = Math.max(8, y);

            menu.show(btnNotif, x, y);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openWaitlistHoldPopup(int activiteId) {
        // inchangé (tu peux garder ton code)
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Place disponible");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(420);
        pane.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-padding: 18;
            -fx-font-family: "Segoe UI";
        """);

        String css = """
            .dialog-pane .button-bar .button {
                -fx-background-radius: 12;
                -fx-padding: 10 16;
                -fx-font-weight: 800;
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

        Activite a = null;
        try { a = activiteService.getById(activiteId); } catch (Exception ignored) {}
        String name = (a != null && a.getNom() != null) ? a.getNom() : ("Activité #" + activiteId);

        Label title = new Label("Une place s'est libérée 🎉");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label sub = new Label("Activité : " + name + "\nTu as 20 minutes pour réserver.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #475569;");

        VBox content = new VBox(10, title, sub);
        content.setPadding(new Insets(4, 0, 6, 0));
        pane.setContent(content);

        ButtonType book = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
        ButtonType skip = new ButtonType("Skip", ButtonBar.ButtonData.NO);
        ButtonType close = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(book, skip, close);

        dialog.setResultConverter(bt -> {
            if (bt == book) return "BOOK";
            if (bt == skip) return "SKIP";
            return null;
        });

        dialog.showAndWait().ifPresent(action -> {
            if ("BOOK".equals(action)) {
                try {
                    waitlistService.confirmHold(CURRENT_USER_ID, activiteId);
                    toastSuccessWithAction("Réservé !", "Votre réservation est confirmée.", "Voir mes réservations", this::goToMyReservationsFromToast);

                    if (flashMode) showFlashSales(null);
                    else reloadFromDB();

                } catch (Exception e) {
                    e.printStackTrace();
                    toastError("Erreur", e.getMessage());
                }
            } else if ("SKIP".equals(action)) {
                try {
                    waitlistService.leaveWaitlist(CURRENT_USER_ID, activiteId);
                    waitlistService.promoteNextIfSeatAvailable(activiteId);
                    toastWarn("OK", "On passe au prochain utilisateur.");

                    if (flashMode) showFlashSales(null);
                    else reloadFromDB();

                } catch (Exception e) {
                    e.printStackTrace();
                    toastError("Erreur", e.getMessage());
                }
            }
        });
    }

    private void refreshNotifCount() {
        try {
            int n = notificationService.countUnread(CURRENT_USER_ID);

            if (lblNotifCount != null) {
                lblNotifCount.setText(String.valueOf(n));
                boolean show = n > 0;
                lblNotifCount.setVisible(show);
                lblNotifCount.setManaged(show);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void showFlashSales(ActionEvent event) {
        flashMode = true;

        new Thread(() -> {
            try {
                activiteService.refreshFlashSales();
                List<Activite> flash = activiteService.getFlashSales();

                // ✅ filtre si contexte
                List<Activite> finalList = applyContextFilter(flash);

                Platform.runLater(() -> {
                    allActivities = finalList;
                    applySearchFilter();

                    if (finalList.isEmpty()) {
                        toastWarn("Flash sales", hasContext()
                                ? "Aucune activité flash disponible pour cette date."
                                : "Aucune activité en vente flash pour le moment.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> toastError("Erreur", "Impossible de charger les flash sales."));
            }
        }).start();
    }
    @FXML
    private void showAllActivities(ActionEvent event) {
        flashMode = false;
        reloadFromDB();
    }

    private boolean isFlashActivity(Activite a) {
        if (a == null) return false;

        if (!a.isFlashSale()) return false;
        if (a.getFlashExpiresAt() == null) return false;
        if (!a.getFlashExpiresAt().isAfter(LocalDateTime.now())) return false;

        return a.getFlashPrice() != null && a.getFlashPrice() > 0;
    }

    // ✅ refresh auto: recharge la bonne liste selon mode + MAJ allActivities
    private void refreshFlashUI() {
        new Thread(() -> {
            try {
                activiteService.refreshFlashSales();

                List<Activite> freshList = flashMode
                        ? activiteService.getFlashSales()
                        : activiteService.getDisponibles();

                // ✅ filtre si contexte
                List<Activite> finalList = applyContextFilter(freshList);

                Platform.runLater(() -> {
                    allActivities = finalList;
                    applySearchFilter();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(this::applySearchFilter);
            }
        }).start();
    }
    private void startFlashCountdown(Label lbl, LocalDateTime expiresAt, VBox card) {

        javafx.animation.Timeline tl = new javafx.animation.Timeline();
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);

        Runnable tick = () -> {
            if (expiresAt == null) {
                lbl.setText("");
                tl.stop();
                return;
            }

            long totalSec = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();

            if (totalSec <= 0) {
                lbl.setText("Expired");
                tl.stop();

                // ✅ recharge selon mode
                if (flashMode) showFlashSales(null);
                else reloadFromDB();

                return;
            }

            long days = totalSec / 86400;
            long rem = totalSec % 86400;
            long hours = rem / 3600;
            rem = rem % 3600;
            long mins = rem / 60;
            long secs = rem % 60;

            String text;
            if (days > 0) {
                text = String.format("Expires in %dd %02d:%02d:%02d", days, hours, mins, secs);
            } else if (hours > 0) {
                text = String.format("Expires in %02d:%02d:%02d", hours, mins, secs);
            } else {
                text = String.format("Expires in %02d:%02d", mins, secs);
            }

            lbl.setText(text);
        };

        tick.run();

        tl.getKeyFrames().setAll(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> tick.run())
        );

        card.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS == null) tl.stop();
        });

        tl.play();
    }
}