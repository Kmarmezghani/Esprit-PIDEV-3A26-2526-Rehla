package Controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Activite;
import models.Review;
import services.ActiviteService;
import services.ReviewService;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityDetailsController {

    private static final int CURRENT_USER_ID = 1;

    @FXML private Label LBLtitle;
    @FXML private Label LBLdestination;
    @FXML private Label LBLdates;
    @FXML private Label LBLprice;
    @FXML private Label LBLrating;
    @FXML private Label LBLtype;

    // ===== Reviews summary (left) =====
    @FXML private Label LBLavgBig;
    @FXML private Label LBLstarsText;
    @FXML private Label LBLcountText;

    // ===== Distribution (right) =====
    @FXML private ProgressBar PB5, PB4, PB3, PB2, PB1;
    @FXML private Label LBLc5, LBLc4, LBLc3, LBLc2, LBLc1;

    @FXML private javafx.scene.image.ImageView IMGactivity;

    @FXML private ListView<Review> reviewsList;
    @FXML private Spinner<Integer> SPreviewRating;
    @FXML private TextArea TAreview;
    @FXML private Button BTNdeleteMyReview;

    private final ActiviteService activiteService = new ActiviteService();
    private final ReviewService reviewService = new ReviewService();

    private Activite activity;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");

    // ===== Editing state =====
    private Review editingReview = null;

    @FXML
    public void initialize() {

        SPreviewRating.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));
        SPreviewRating.setEditable(true);
        BTNdeleteMyReview.setDisable(true);

        reviewsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean canDelete = newV != null && newV.getPersonneId() == CURRENT_USER_ID;
            BTNdeleteMyReview.setDisable(!canDelete);
        });

        // IMPORTANT: refresh to show/hide edit icon on selection
        reviewsList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            reviewsList.refresh();
        });

        reviewsList.setStyle("""
            -fx-background-color: transparent;
            -fx-control-inner-background: transparent;
        """);

        reviewsList.setCellFactory(list -> new ReviewCardCell());
    }

    public void setActivity(Activite a) {
        this.activity = a;
        renderActivity();
        loadReviewsAndRating();
    }

    private void renderActivity() {
        if (activity == null) return;

        LBLtitle.setText(activity.getNom());

        String dest = activiteService.getDestinationDisplayById(activity.getDestinationId());
        LBLdestination.setText("📍 " + (dest == null || dest.isBlank() ? "Unknown" : dest));

        String start = (activity.getDateDebut() != null) ? activity.getDateDebut().format(dtf) : "—";
        String end = (activity.getDateFin() != null) ? activity.getDateFin().format(dtf) : "—";
        LBLdates.setText("🕒 " + start + "  →  " + end);

        LBLprice.setText(String.format("💰 %.2f TND", activity.getPrix()));
        LBLtype.setText("Type: " + (activity.getTypeActivite() == null ? "" : activity.getTypeActivite()));

        try {
            IMGactivity.setImage(new Image(getClass().getResourceAsStream("/icons/activity_placeholder.png")));
        } catch (Exception ignored) {}
    }

    private void loadReviewsAndRating() {
        if (activity == null) return;

        List<Review> list = reviewService.getReviewsByActiviteId(activity.getId());
        reviewsList.setItems(FXCollections.observableArrayList(list));
        BTNdeleteMyReview.setDisable(true);

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
        LBLrating.setText("⭐ " + String.format("%.1f", avg));

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

    // ===== Start editing =====
    private void startEdit(Review r) {
        if (r == null) return;
        if (r.getPersonneId() != CURRENT_USER_ID) return;

        editingReview = r;

        TAreview.setText(r.getCommentaire() == null ? "" : r.getCommentaire());
        if (SPreviewRating.getValueFactory() != null) {
            SPreviewRating.getValueFactory().setValue(r.getNote());
        }

        TAreview.requestFocus();
        TAreview.positionCaret(TAreview.getText().length());
    }

    private void exitEditMode() {
        editingReview = null;
        TAreview.clear();
        if (SPreviewRating.getValueFactory() != null) {
            SPreviewRating.getValueFactory().setValue(5);
        }
    }

    @FXML
    void addReview() {
        if (activity == null) return;

        String comment = (TAreview.getText() == null) ? "" : TAreview.getText().trim();
        if (comment.isEmpty()) {
            showWarn("Missing review", "Please write a comment before submitting.");
            return;
        }

        int rating = SPreviewRating.getValue();

        if (editingReview != null) {
            editingReview.setNote(rating);
            editingReview.setCommentaire(comment);
            editingReview.setDateAvis(LocalDate.now());

            reviewService.update(editingReview);

            exitEditMode();
        } else {
            Review r = new Review();
            r.setActiviteId(activity.getId());
            r.setPersonneId(CURRENT_USER_ID);
            r.setNote(rating);
            r.setCommentaire(comment);
            r.setDateAvis(LocalDate.now());

            reviewService.add(r);

            TAreview.clear();
        }

        activiteService.updateNoteMoyenne(activity.getId());
        loadReviewsAndRating();
    }

    @FXML
    void deleteMyReview() {
        if (activity == null) return;

        Review selected = reviewsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getPersonneId() != CURRENT_USER_ID) {
            showWarn("Not allowed", "You can delete only your own review.");
            return;
        }

        if (editingReview != null && editingReview.getId() == selected.getId()) {
            exitEditMode();
        }

        reviewService.deleteByIdAndUser(selected.getId(), CURRENT_USER_ID);
        activiteService.updateNoteMoyenne(activity.getId());
        loadReviewsAndRating();
    }

    @FXML
    public void backToActivities(javafx.event.ActionEvent event) {
        switchScene((Node) event.getSource(), "/Frontoffice/ActivitiesPage.fxml");
    }


    private void switchScene(Node anyNodeOnScene, String fxmlPath) {
        try {
            Stage stage = (Stage) anyNodeOnScene.getScene().getWindow();

            boolean wasMax = stage.isMaximized();
            boolean wasFull = stage.isFullScreen();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene newScene = new Scene(root, w, h);

            stage.setScene(newScene);

            stage.setMaximized(wasMax);
            stage.setFullScreen(wasFull);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void goToHome(javafx.event.ActionEvent event) { switchScene((Node) event.getSource(), "/Frontoffice/HomePage.fxml"); }
    @FXML public void goToDestinations(javafx.event.ActionEvent event) { }
    @FXML public void goToPosts(javafx.event.ActionEvent event) { switchScene((Node) event.getSource(), "/Frontoffice/PostsPage.fxml"); }
    @FXML public void goToactivities(javafx.event.ActionEvent event) { switchScene((Node) event.getSource(), "/Frontoffice/ActivitiesPage.fxml"); }

    private void showWarn(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // =========================
    //  CUSTOM REVIEW CARD CELL
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

        private StackPane buildAvatar(Review r) {
            String username = (r.getUserName() != null && !r.getUserName().isBlank())
                    ? r.getUserName()
                    : ("U" + r.getPersonneId());

            String initials = initialsOf(username);

            Circle circle = new Circle(18);
            circle.setFill(Color.web("#EEF2FF"));
            circle.setStroke(Color.web("#DDE3FF"));

            Label init = new Label(initials);
            init.setFont(Font.font("System", FontWeight.BOLD, 12));
            init.setTextFill(Color.web("#223f91"));

            StackPane avatar = new StackPane(circle, init);
            avatar.setMinSize(36, 36);
            avatar.setMaxSize(36, 36);
            return avatar;
        }
    }

    private String starsText(int note) {
        int n = Math.max(0, Math.min(5, note));
        return "★".repeat(n) + "☆".repeat(5 - n);
    }

    private String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "U";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String buildTimeAgo(LocalDate dateAvis) {
        if (dateAvis == null) return "";
        LocalDate now = LocalDate.now();
        if (dateAvis.isAfter(now)) return "Today";

        Period p = Period.between(dateAvis, now);
        if (p.getYears() > 0) return p.getYears() + " year" + (p.getYears() > 1 ? "s" : "") + " ago";
        if (p.getMonths() > 0) return p.getMonths() + " month" + (p.getMonths() > 1 ? "s" : "") + " ago";
        if (p.getDays() > 7) return (p.getDays() / 7) + " week" + ((p.getDays() / 7) > 1 ? "s" : "") + " ago";
        if (p.getDays() > 1) return p.getDays() + " days ago";
        if (p.getDays() == 1) return "Yesterday";
        return "Today";
    }
}
