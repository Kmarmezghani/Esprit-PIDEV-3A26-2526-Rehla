package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Activite;
import models.Personne;
import models.Review;
import services.ActiviteService;
import services.GeminiTipsService;
import services.NotificationService;
import services.ReviewService;
import util.Session;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityDetailsController {

    // =========================
    // SESSION
    // =========================
    Personne CURRENT_USER = Session.getCurrentUser();
    int CURRENT_USER_ID = (CURRENT_USER != null) ? CURRENT_USER.getId() : -1;

    // =========================
    // TOPBAR (NOTIF + PROFILE)
    // =========================
    @FXML private Button btnProfile;
    @FXML private Button btnNotif;
    @FXML private ContextMenu profileMenu;
    @FXML private Label lblNotifCount;

    private final NotificationService notificationService = new NotificationService();

    // =========================
    // HEADER / DETAILS
    // =========================
    @FXML private Label LBLtitle;
    @FXML private Label LBLdestination;
    @FXML private Label LBLdates;
    @FXML private Label LBLprice;
    @FXML private Label LBLrating;
    @FXML private Label LBLtype;

    @FXML private StackPane headerPane;
    @FXML private Rectangle overlayRect;
    @FXML private ImageView IMGactivity;

    @FXML private Label LBLdescription;

    // =========================
    // REVIEWS SUMMARY
    // =========================
    @FXML private Label LBLavgBig;
    @FXML private Label LBLstarsText;
    @FXML private Label LBLcountText;

    // IA Tips
    @FXML private VBox tipsBox;
    @FXML private VBox tipsItems;
    @FXML private Label tipsMeta;
    @FXML private MenuItem menuMyActivities;

    private final GeminiTipsService tipsService = new GeminiTipsService();
    private final Map<Integer, List<String>> tipsCache = new HashMap<>();
    private final Map<Integer, Integer> fpCache = new HashMap<>();

    // Distribution
    @FXML private ProgressBar PB5, PB4, PB3, PB2, PB1;
    @FXML private Label LBLc5, LBLc4, LBLc3, LBLc2, LBLc1;

    @FXML private ListView<Review> reviewsList;

    @FXML private HBox starBox;
    @FXML private TextArea TAreview;
    @FXML private Button BTNdeleteMyReview;

    private final ActiviteService activiteService = new ActiviteService();
    private final ReviewService reviewService = new ReviewService();

    private Activite activity;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");

    private Review editingReview = null;
    private int selectedRating = 5;
    private int hoverRating = 0;


    private final Map<Integer, Image> avatarCache = new ConcurrentHashMap<>();

    private Map<Integer, String> reviewerPhotoPath = new HashMap<>();

    // =========================
    // INIT
    // =========================
    @FXML
    public void initialize() {

        // Bind header image sizing
        Platform.runLater(() -> {
            if (headerPane != null && IMGactivity != null) {
                IMGactivity.fitWidthProperty().bind(headerPane.widthProperty());
                IMGactivity.fitHeightProperty().bind(headerPane.heightProperty());
                IMGactivity.setPreserveRatio(false);
                IMGactivity.setSmooth(true);
            }
            if (headerPane != null && overlayRect != null) {
                overlayRect.widthProperty().bind(headerPane.widthProperty());
                overlayRect.heightProperty().bind(headerPane.heightProperty());
            }
        });

        Personne u = Session.getCurrentUser();
        boolean isGuide = (u != null) && "GUIDE".equalsIgnoreCase(u.getRole());
        if (!isGuide && profileMenu != null && menuMyActivities != null) {
            profileMenu.getItems().remove(menuMyActivities); // pas d’espace vide
        }

        // Notif count (badge)
        refreshNotifCount();

        if (BTNdeleteMyReview != null) BTNdeleteMyReview.setDisable(true);

        initStarRating();

        if (reviewsList != null) {
            reviewsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                boolean canDelete = newV != null && newV.getPersonneId() == CURRENT_USER_ID;
                if (BTNdeleteMyReview != null) BTNdeleteMyReview.setDisable(!canDelete);
            });

            reviewsList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> reviewsList.refresh());

            reviewsList.setStyle("""
                -fx-background-color: transparent;
                -fx-control-inner-background: transparent;
            """);

            reviewsList.setCellFactory(list -> new ReviewCardCell());
        }

        hideTips();
    }

    @FXML public void goToMyProfile(ActionEvent event) { }
    @FXML public void goToMyPosts(ActionEvent event) { }

    @FXML
    void goToMyReservations(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(),
                "/Frontoffice/MyReservation.fxml");
    }

    @FXML
    public void goToMyActivities(ActionEvent event) {
        switchScene(event, "/Frontoffice/MyActivitiesPage.fxml");
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

    // =========================
    // SET DATA
    // =========================
    public void setActivity(Activite a) {
        this.activity = a;
        renderActivity();
        loadReviewsAndRating();
    }

    private void renderActivity() {
        if (activity == null) return;

        if (LBLtitle != null) LBLtitle.setText(activity.getNom());

        String dest = activiteService.getDestinationDisplayById(activity.getDestinationId());
        if (LBLdestination != null) {
            LBLdestination.setText("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));
        }

        String start = (activity.getDateDebut() != null) ? activity.getDateDebut().format(dtf) : "—";
        String end   = (activity.getDateFin()   != null) ? activity.getDateFin().format(dtf)   : "—";
        if (LBLdates != null) LBLdates.setText("🕒 " + start + "  →  " + end);

        if (LBLprice != null) LBLprice.setText(String.format("💰 %.2f TND", activity.getPrix()));
        if (LBLtype != null) LBLtype.setText("Type: " + (activity.getTypeActivite() == null ? "" : activity.getTypeActivite()));

        String desc = activity.getDescription();
        if (LBLdescription != null) {
            LBLdescription.setText((desc == null || desc.isBlank()) ? "No description provided." : desc.trim());
        }

        Image real = loadActivityImage(activity.getImage());
        if (IMGactivity != null) {
            if (real != null) IMGactivity.setImage(real);
            else {
                Image ph = loadPlaceholder();
                if (ph != null) IMGactivity.setImage(ph);
            }
        }
    }

    // =========================
    // REVIEWS LOAD
    // =========================
    private void loadReviewsAndRating() {
        if (activity == null) return;

        List<Review> list = reviewService.getReviewsByActiviteId(activity.getId());
        Set<Integer> ids = new HashSet<>();
        for (Review r : list) {
            if (r != null && r.getPersonneId() > 0) ids.add(r.getPersonneId());
        }
        reviewerPhotoPath = reviewService.getProfilePhotosForUsers(ids);
        if (reviewsList != null) reviewsList.setItems(FXCollections.observableArrayList(list));
        if (BTNdeleteMyReview != null) BTNdeleteMyReview.setDisable(true);

        int total = list.size();
        int[] count = new int[6];
        int sum = 0;

        for (Review r : list) {
            int n = Math.max(1, Math.min(5, r.getNote()));
            count[n]++;
            sum += n;
        }

        double avg = (total == 0) ? 0.0 : (sum / (double) total);

        activity.setNoteMoyenne(avg);
        if (LBLrating != null) LBLrating.setText("⭐ " + String.format("%.1f", avg));

        if (LBLavgBig != null) LBLavgBig.setText(String.format("%.1f", avg));
        if (LBLstarsText != null) LBLstarsText.setText(starsFromAverage(avg));
        if (LBLcountText != null) LBLcountText.setText(total + " ratings");

        double denom = (total == 0) ? 1.0 : total;

        if (PB5 != null) PB5.setProgress(count[5] / denom);
        if (PB4 != null) PB4.setProgress(count[4] / denom);
        if (PB3 != null) PB3.setProgress(count[3] / denom);
        if (PB2 != null) PB2.setProgress(count[2] / denom);
        if (PB1 != null) PB1.setProgress(count[1] / denom);

        if (LBLc5 != null) LBLc5.setText("5.0  " + count[5] + " reviews");
        if (LBLc4 != null) LBLc4.setText("4.0  " + count[4] + " reviews");
        if (LBLc3 != null) LBLc3.setText("3.0  " + count[3] + " reviews");
        if (LBLc2 != null) LBLc2.setText("2.0  " + count[2] + " reviews");
        if (LBLc1 != null) LBLc1.setText("1.0  " + count[1] + " reviews");

        generateTipsSmart(list);
    }

    // =========================
    // IA Tips
    // =========================
    private void hideTips() {
        if (tipsItems != null) tipsItems.getChildren().clear();
        if (tipsMeta != null) tipsMeta.setText("");
        if (tipsBox != null) {
            tipsBox.setVisible(false);
            tipsBox.setManaged(false);
        }
    }

    private void showTips(List<String> tips, String meta) {
        if (tipsBox == null || tipsItems == null) return;

        tipsItems.getChildren().clear();

        if (tips == null) tips = List.of();
        List<String> limited = tips.stream().filter(s -> s != null && !s.isBlank()).limit(4).toList();

        if (limited.isEmpty()) {
            hideTips();
            return;
        }

        tipsBox.setVisible(true);
        tipsBox.setManaged(true);

        if (tipsMeta != null) tipsMeta.setText(meta == null ? "" : meta);

        FlowPane flow = new FlowPane();
        flow.setHgap(10);
        flow.setVgap(10);
        flow.setPrefWrapLength(520);

        for (String tip : limited) {
            Label chip = new Label("💡 " + tip.trim());
            chip.getStyleClass().add("tipChip");
            chip.setWrapText(true);
            flow.getChildren().add(chip);
        }

        tipsItems.getChildren().add(flow);
    }

    private void generateTipsSmart(List<Review> list) {
        if (activity == null) return;
        if (tipsBox == null || tipsItems == null || tipsMeta == null) return;

        List<String> texts = list.stream()
                .map(r -> r.getCommentaire() == null ? "" : r.getCommentaire().trim())
                .filter(s -> !s.isEmpty())
                .toList();

        if (texts.isEmpty()) {
            hideTips();
            return;
        }

        int fp = Objects.hash(texts.size(), String.join("|", texts).hashCode());
        Integer oldFp = fpCache.get(activity.getId());

        if (oldFp != null && oldFp == fp) {
            List<String> cached = tipsCache.get(activity.getId());
            if (cached != null && !cached.isEmpty()) {
                showTips(cached, "Based on travelers’ reviews (cached)");
                return;
            }
        }

        tipsItems.getChildren().clear();
        tipsBox.setVisible(true);
        tipsBox.setManaged(true);
        tipsMeta.setText("Generating tips from reviews...");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return tipsService.extractTips(texts);
            }
        };

        task.setOnSucceeded(e -> {
            List<String> tips = task.getValue();

            if (tips == null || tips.isEmpty()) {
                hideTips();
                return;
            }

            tipsCache.put(activity.getId(), tips);
            fpCache.put(activity.getId(), fp);

            showTips(tips, "Based on travelers’ reviews");
        });

        task.setOnFailed(e -> {
            List<String> cached = tipsCache.get(activity.getId());
            if (cached != null && !cached.isEmpty()) {
                showTips(cached, "Based on travelers’ reviews (cached)");
            } else {
                hideTips();
            }
        });

        Thread th = new Thread(task, "gemini-tips");
        th.setDaemon(true);
        th.start();
    }

    // =========================
    // Stars rating UI
    // =========================
    private void initStarRating() {
        if (starBox == null) return;

        starBox.getChildren().clear();

        for (int i = 1; i <= 5; i++) {
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 26; -fx-text-fill: #223f91; -fx-cursor: hand;");

            final int value = i;

            star.setOnMouseEntered(e -> {
                hoverRating = value;
                updateStars(hoverRating);
            });

            star.setOnMouseExited(e -> {
                hoverRating = 0;
                updateStars(selectedRating);
            });

            star.setOnMouseClicked(e -> {
                selectedRating = value;
                updateStars(selectedRating);
            });

            starBox.getChildren().add(star);
        }

        updateStars(selectedRating);
    }

    private void updateStars(int rating) {
        for (int i = 0; i < starBox.getChildren().size(); i++) {
            Label star = (Label) starBox.getChildren().get(i);
            star.setText(i < rating ? "★" : "☆");
        }
    }

    private String starsFromAverage(double avg) {
        avg = Math.max(0, Math.min(5, avg));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            double diff = avg - (i - 1);
            if (diff >= 1) sb.append("★");
            else if (diff >= 0.75) sb.append("★");
            else if (diff >= 0.5) sb.append("⯨");
            else if (diff >= 0.25) sb.append("⯪");
            else sb.append("☆");
        }
        return sb.toString();
    }

    // =========================
    // CRUD Reviews
    // =========================
    private void startEdit(Review r) {
        if (r == null) return;
        if (r.getPersonneId() != CURRENT_USER_ID) return;

        editingReview = r;

        if (TAreview != null) {
            TAreview.setText(r.getCommentaire() == null ? "" : r.getCommentaire());
            TAreview.requestFocus();
            TAreview.positionCaret(TAreview.getText().length());
        }

        selectedRating = Math.max(1, Math.min(5, r.getNote()));
        updateStars(selectedRating);
    }

    private void exitEditMode() {
        editingReview = null;
        if (TAreview != null) TAreview.clear();
        selectedRating = 5;
        updateStars(selectedRating);
    }

    @FXML
    void addReview() {
        if (activity == null) return;

        String comment = (TAreview == null || TAreview.getText() == null) ? "" : TAreview.getText().trim();
        if (comment.isEmpty()) {
            showWarn("Missing review", "Please write a comment before submitting.");
            return;
        }

        int rating = selectedRating;

        if (editingReview != null) {
            editingReview.setNote(rating);
            editingReview.setCommentaire(comment);
            editingReview.setDateAvis(LocalDateTime.now());

            reviewService.update(editingReview);
            exitEditMode();
        } else {
            Review r = new Review();
            r.setActiviteId(activity.getId());
            r.setPersonneId(CURRENT_USER_ID);
            r.setNote(rating);
            r.setCommentaire(comment);
            r.setDateAvis(LocalDateTime.now());

            reviewService.add(r);
            if (TAreview != null) TAreview.clear();
        }

        activiteService.updateNoteMoyenne(activity.getId());

        fpCache.remove(activity.getId());
        loadReviewsAndRating();
    }

    private boolean confirmDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete review");
        confirm.setHeaderText("Are you sure you want to delete your review?");
        confirm.setContentText("This action cannot be undone.");

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBtn, cancelBtn);

        Optional<ButtonType> res = confirm.showAndWait();
        return res.isPresent() && res.get() == deleteBtn;
    }

    @FXML
    void deleteMyReview() {
        if (activity == null) return;

        Review selected = (reviewsList == null) ? null : reviewsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getPersonneId() != CURRENT_USER_ID) {
            showWarn("Not allowed", "You can delete only your own review.");
            return;
        }

        if (!confirmDelete()) return;

        if (editingReview != null && editingReview.getId() == selected.getId()) {
            exitEditMode();
        }

        reviewService.deleteByIdAndUser(selected.getId(), CURRENT_USER_ID);
        activiteService.updateNoteMoyenne(activity.getId());

        fpCache.remove(activity.getId());
        loadReviewsAndRating();
    }

    // =========================
    // NAVIGATION
    // =========================
    @FXML
    public void backToActivities(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(), "/Frontoffice/ActivitiesPage.fxml");
    }

    @FXML
    public void goToHome(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(), "/Frontoffice/HomePage.fxml");
    }

    @FXML public void goToDestinations(ActionEvent event) { /* TODO */ }

    @FXML
    public void goToPosts(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(), "/Frontoffice/PostsPage.fxml");
    }

    @FXML
    public void goToactivities(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(), "/Frontoffice/ActivitiesPage.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        alert.showAndWait();
    }

    private void switchSceneKeepSize(Node anyNodeOnScene, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) anyNodeOnScene.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWarn(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // =========================
    // ✅ PROFILE MENU + NOTIFS
    // =========================
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

            // positionner sous le bouton notif
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

    // =========================
    // IMAGE LOADER (activité)
    // =========================
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
            InputStream is = getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png");
            return is != null ? new Image(is) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // ✅ Avatar loader (profilePhoto de Personne)
    // =========================
    private Image loadProfilePhoto(String rawPath) {
        if (rawPath == null) return null;
        String path = rawPath.trim();
        if (path.isEmpty()) return null;

        try {
            // URL
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return new Image(path, false);
            }

            // déjà file:/...
            if (path.startsWith("file:/")) {
                return new Image(path, false);
            }

            // resource "/..."
            if (path.startsWith("/")) {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) return new Image(is);
            }

            // fichier local
            File file = new File(path);
            if (!file.exists()) file = new File(System.getProperty("user.dir"), path);
            if (file.exists()) return new Image(file.toURI().toString(), false);

        } catch (Exception ignored) {}

        return null;
    }

    // =========================
    // CUSTOM REVIEW CELL
    // =========================
    private class ReviewCardCell extends ListCell<Review> {
        @Override
        protected void updateItem(Review r, boolean empty) {
            super.updateItem(r, empty);

            if (empty || r == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            VBox card = new VBox(8);
            card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 14;
                -fx-padding: 14;
                -fx-border-color: #eef2ff;
                -fx-border-radius: 14;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 3);
            """);

            HBox header = new HBox(12);
            header.setFillHeight(true);

            StackPane avatar = buildAvatar(r);

            VBox nameBox = new VBox(2);

            String username = (r.getUserName() != null && !r.getUserName().isBlank())
                    ? r.getUserName()
                    : ("User#" + r.getPersonneId());

            boolean isYou = r.getPersonneId() == CURRENT_USER_ID;

            Label name = new Label(username + (isYou ? " (You)" : ""));
            name.setFont(Font.font("System", FontWeight.BOLD, 14));
            name.setTextFill(Color.web("#1f2a44"));

            Label timeAgo = new Label(buildTimeAgo(r.getDateAvis()));
            timeAgo.setStyle("-fx-text-fill: #7b8798; -fx-font-size: 12;");

            nameBox.getChildren().addAll(name, timeAgo);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button editBtn = new Button("✎");
            editBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #223f91;
                -fx-font-size: 16;
                -fx-cursor: hand;
            """);

            boolean showEdit = isYou && isSelected();
            editBtn.setVisible(showEdit);
            editBtn.setManaged(showEdit);
            editBtn.setOnAction(e -> ActivityDetailsController.this.startEdit(r));

            VBox ratingBox = new VBox(2);
            ratingBox.setMinWidth(110);
            ratingBox.setMaxWidth(110);

            Label note = new Label(String.format("%.1f", (double) r.getNote()));
            note.setFont(Font.font("System", FontWeight.BOLD, 14));
            note.setTextFill(Color.web("#1f2a44"));

            Label stars = new Label(starsText(r.getNote()));
            stars.setStyle("-fx-text-fill: #223f91; -fx-font-size: 14;");
            ratingBox.getChildren().addAll(note, stars);

            header.getChildren().addAll(avatar, nameBox, spacer, editBtn, ratingBox);

            Label comment = new Label(r.getCommentaire() == null ? "" : r.getCommentaire());
            comment.setWrapText(true);
            comment.setStyle("-fx-text-fill: #3c4b66; -fx-font-size: 13;");

            Separator sep = new Separator();
            sep.setStyle("-fx-opacity: 0.35;");

            card.getChildren().addAll(header, sep, comment);

            setStyle("-fx-background-color: transparent; -fx-padding: 10 0 10 0;");
            setGraphic(card);
        }

        /**
         * ✅ Avatar avec image si disponible.
         * Ici on affiche l'image uniquement pour "You" (CURRENT_USER) car tu n’as pas donné PersonneService.
         * Si tu veux l’image pour TOUS les users, il faut récupérer Personne par id via un service.
         */
        private StackPane buildAvatar(Review r) {
            double size = 36;

            int uid = r.getPersonneId();
            String profilePhoto = reviewerPhotoPath.get(uid); // ✅ photo de ce reviewer (pas currentUser)

            Image img = null;
            if (profilePhoto != null && !profilePhoto.isBlank()) {
                img = avatarCache.computeIfAbsent(uid, k -> loadProfilePhoto(profilePhoto));
                if (img != null && img.isError()) img = null;
            }

            // border
            Circle border = new Circle(size / 2);
            border.setFill(Color.web("#EEF2FF"));
            border.setStroke(Color.web("#DDE3FF"));

            // ✅ image ronde si existe
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);

                Circle clip = new Circle(size / 2);
                clip.setCenterX(size / 2);
                clip.setCenterY(size / 2);
                iv.setClip(clip);


                border.setFill(Color.TRANSPARENT);

                StackPane wrap = new StackPane(iv, border); // image derrière, bordure au dessus (stroke seulement)
                wrap.setMinSize(size, size);
                wrap.setPrefSize(size, size);
                wrap.setMaxSize(size, size);
                return wrap;
            }
            // fallback: initiales
            String username = (r.getUserName() != null && !r.getUserName().isBlank())
                    ? r.getUserName()
                    : ("U" + uid);

            String initials = initialsOf(username);

            Label init = new Label(initials);
            init.setFont(Font.font("System", FontWeight.BOLD, 12));
            init.setTextFill(Color.web("#223f91"));

            StackPane avatar = new StackPane(border, init);
            avatar.setMinSize(size, size);
            avatar.setMaxSize(size, size);
            return avatar;
        }
    }

    private String starsText(int note) {
        int n = Math.max(0, Math.min(5, note));
        return "★".repeat(n) + "☆".repeat(5 - n);
    }

    private String initialsOf(String name) {
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "U";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String buildTimeAgo(LocalDateTime dateAvis) {
        if (dateAvis == null) return "";

        LocalDateTime now = LocalDateTime.now();
        if (dateAvis.isAfter(now)) return "Today";

        Period p = Period.between(dateAvis.toLocalDate(), now.toLocalDate());
        if (p.getYears() > 0) return p.getYears() + " year" + (p.getYears() > 1 ? "s" : "") + " ago";
        if (p.getMonths() > 0) return p.getMonths() + " month" + (p.getMonths() > 1 ? "s" : "") + " ago";

        Duration d = Duration.between(dateAvis, now);
        long days = d.toDays();
        if (days > 7) return (days / 7) + " week" + ((days / 7) > 1 ? "s" : "") + " ago";
        if (days > 1) return days + " days ago";
        if (days == 1) return "Yesterday";

        long hours = d.toHours();
        if (hours >= 1) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long mins = d.toMinutes();
        if (mins >= 1) return mins + " min ago";

        return "Just now";
    }

    @FXML void closewindow(ActionEvent e) { Stage s = getStageFromEvent(e); if (s != null) s.close(); }
    @FXML void minwindow(ActionEvent e) { Stage s = getStageFromEvent(e); if (s != null) s.setIconified(true); }
    @FXML void maxwindow(ActionEvent e) {
        Stage s = getStageFromEvent(e);
        if (s != null) s.setMaximized(!s.isMaximized());
    }

    private Stage getStageFromEvent(ActionEvent event) {
        try {
            Object src = event.getSource();
            if (src instanceof MenuItem mi) {
                return (Stage) mi.getParentPopup().getOwnerWindow();
            }
            if (src instanceof Node n) {
                return (Stage) n.getScene().getWindow();
            }
            return null;
        } catch (Exception ignored) {}
        return null;
    }
}