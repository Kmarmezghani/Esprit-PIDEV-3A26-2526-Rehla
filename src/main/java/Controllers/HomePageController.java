package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.*;
import services.*;
import util.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class HomePageController implements Initializable {

    // Header
    @FXML private Button btnNotif, btnProfile;
    @FXML private ContextMenu profileMenu;
    @FXML private Label lblFlashTitle;
    @FXML private Label lblReviewsCount;
    @FXML private HBox reviewsBox;

    // Hero
    @FXML private TextField tfWhere;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;


    // Flash
    @FXML private HBox flashDealsBox;
    @FXML private Label lblFlashSubtitle;

    // Top destinations
    @FXML private FlowPane popularDestinationsFlow;
    private final ActiviteService activiteService = new ActiviteService();
    private final ReviewService reviewService = new ReviewService();
    // Blog
    @FXML private FlowPane blogFlow;

    // Trust
    @FXML private Label lblOverallRating;
    @FXML private HBox testimonialsBox;

    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();

    private final TicketService ticketService = new TicketService();
    private final ReservationService reservationService = new ReservationService();
    private final PreferenceService preferenceService = new PreferenceService();
    private final PostService postService = new PostService();
    Personne CURRENT_USER = Session.getCurrentUser();
    int CURRENT_USER_ID=CURRENT_USER.getId();

    @FXML private MenuItem menuMyActivities;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        setupDefaultDates();

        loadFlashSalesActivities();
        loadTopDestinationsFromDb(); // ✅ compatible avec nouveau model Ville
        loadBlogDemo();
        loadTestimonialsDemo();
        loadReviewsReal();

        Personne u = Session.getCurrentUser();
        boolean isGuide = (u != null) && "GUIDE".equalsIgnoreCase(u.getRole());

        if (!isGuide) {
            profileMenu.getItems().remove(menuMyActivities); // pas d’espace vide
        }

    }


    private void setupDefaultDates() {
        if (dpStart != null) dpStart.setValue(LocalDate.now().plusDays(3));
        if (dpEnd != null) dpEnd.setValue(LocalDate.now().plusDays(6));
    }

    // =========================
    // SEARCH
    // =========================
    @FXML
    private void handleGeneratePackFromHome(ActionEvent event) {

        if (tfWhere.getText().isEmpty()
                || dpStart.getValue() == null
                || dpEnd.getValue() == null) {

            System.out.println("Please fill destination and dates");
            return;
        }

        String destinationSearch = tfWhere.getText().toLowerCase();

        Preference pref = preferenceService.getByPersonneId(CURRENT_USER_ID);

        if (pref == null) {
            System.out.println("No preferences found");
            return;
        }

        double budgetMax = pref.getBudgetMax();

        List<Ticket> all = ticketService.getAvailableTickets();

        // 🔎 FILTER BY CITY + TYPE
        List<Ticket> flights = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("flight"))
                .filter(t -> t.getDestinationNom().toLowerCase().contains(destinationSearch))
                .toList();

        List<Ticket> hotels = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("hotel"))
                .filter(t -> t.getDestinationNom().toLowerCase().contains(destinationSearch))
                .toList();

        List<Ticket> transports = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("transport"))
                .filter(t -> t.getDestinationNom().toLowerCase().contains(destinationSearch))
                .toList();

        Ticket bestFlight = null;
        Ticket bestHotel = null;
        Ticket bestTransport = null;

        double bestTotal = 0;

        for (Ticket f : flights) {
            for (Ticket h : hotels) {
                for (Ticket tr : transports) {

                    double total = f.getPrix() + h.getPrix() + tr.getPrix();

                    if (total <= budgetMax- 300 && total > bestTotal) {
                        bestTotal = total;
                        bestFlight = f;
                        bestHotel = h;
                        bestTransport = tr;
                    }
                }
            }
        }

        if (bestFlight == null) {
            System.out.println("No valid pack found for this destination.");
            return;
        }

        try {

            // ================= CREATE RESERVATION =================

            Reservation reservation = new Reservation();
            reservation.setPersonneId(CURRENT_USER_ID);
            reservation.setDestinationId(bestFlight.getDestinationId());
            reservation.setDateReservation(java.sql.Date.valueOf(java.time.LocalDate.now()));
            reservation.setDateDebut(java.sql.Date.valueOf(dpStart.getValue()));
            reservation.setDateFin(java.sql.Date.valueOf(dpEnd.getValue()));
            reservation.setStatut("Reserved");
            reservation.setCoutTotal(bestTotal);

            reservationService.add(reservation);

            int reservationId = reservationService.getLastInsertedId();

            // ================= LINK TICKETS =================

            List<Ticket> pack = List.of(bestFlight, bestHotel, bestTransport);

            for (Ticket t : pack) {
                t.setReservationId(reservationId);
                t.setStatut("Reserved");
                ticketService.update(t);
            }

            System.out.println("🎁 Smart Pack Created Automatically! Total = " + bestTotal);

            // ================= REDIRECT =================

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            util.NavigationUtil.switchScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // TOP DESTINATIONS (DB)
    // =========================
    private void loadTopDestinationsFromDb() {
        if (popularDestinationsFlow == null) return;
        popularDestinationsFlow.getChildren().clear();

        List<Ville> villes = villeService.getAll();
        List<Pays> paysList = paysService.getAll();

        Map<Integer, String> paysNameById = paysList.stream()
                .collect(Collectors.toMap(Pays::getId, Pays::getNom, (a, b) -> a));

        // ✅ tri “top” : visit count desc
        List<Ville> top = villes.stream()
                .sorted(Comparator
                        .comparingInt(Ville::getVisitCount).reversed()
                )
                .limit(4)
                .collect(Collectors.toList());

        for (Ville v : top) {
            String paysNom = paysNameById.getOrDefault(v.getPaysId(), "Unknown");
            popularDestinationsFlow.getChildren().add(createTopDestinationCard(v, paysNom));
        }
    }

    private VBox createTopDestinationCard(Ville ville, String paysNom) {

        double cardW = 280;
        double imgH = 180;

        VBox card = new VBox();
        card.getStyleClass().add("destCard");
        card.setPrefWidth(cardW);
        card.setMaxWidth(cardW);

        // Use actual city image from database if available
        ImageView iv = createCityImage(ville, cardW, imgH, 18);

        VBox body = new VBox(6);
        body.getStyleClass().add("destBody");

        Label name = new Label(ville.getNom() + ", " + paysNom);
        name.getStyleClass().add("destName");

        String descTxt = (ville.getTypeTourisme() != null && !ville.getTypeTourisme().isBlank())
                ? ville.getTypeTourisme()
                : "Discover amazing places";
        Label desc = new Label(descTxt);
        desc.getStyleClass().add("destDesc");
        desc.setWrapText(true);

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #eef2f7;");

        int days = guessDaysFromSeason(ville.getSaison());


        Label daysLbl = new Label("⏱ " + days + " Days");
        daysLbl.getStyleClass().add("metaText");

        Label priceLbl = new Label("👁 " + ville.getVisitCount() + " visits");
        priceLbl.getStyleClass().add("priceText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button explore = new Button("Explore →");
        explore.getStyleClass().add("exploreBtn");
        explore.setOnAction(e -> navigateToCity(ville));

        HBox bottom = new HBox(10, daysLbl, priceLbl, spacer, explore);
        bottom.getStyleClass().add("metaRow");
        bottom.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(name, desc, sep, bottom);

        card.getChildren().addAll(iv, body);

        card.setOnMouseClicked(e -> navigateToCity(ville));
        return card;
    }

    private int guessDaysFromSeason(String saison) {
        if (saison == null) return 5;
        return switch (saison.toLowerCase(Locale.ROOT)) {
            case "summer" -> 8;
            case "winter" -> 6;
            case "spring" -> 7;
            case "autumn", "fall" -> 5;
            default -> 5;
        };
    }

    // =========================
    // BLOG (DEMO)
    // =========================
    private record UserPost(String username, String caption, String imagePath, int likes, int comments) {}

    private void loadBlogDemo() {
        if (blogFlow == null) return;
        blogFlow.getChildren().clear();

        List<UserPost> posts = List.of(
                new UserPost("Rayen", "Best sunrise spot in Zaghouan 🔥", "/Frontoffice/images/blog/blog-default.jpg", 128, 14),
                new UserPost("Lina", "Medina walk was insane, don’t miss it!", "/Frontoffice/images/blog/blog-default.jpg", 92, 9),
                new UserPost("Ahmed", "Budget tip: eat like a local, save a lot 💡", "/Frontoffice/images/blog/blog-default.jpg", 61, 7)
        );

//        for (UserPost p : posts) blogFlow.getChildren().add(createUserPostCard(p));
        loadTopPosts();
    }

    private VBox createUserPostCard(Post p) {
        double cardW = 300;
        double imgH = 160;

        VBox card = new VBox(10);
        card.getStyleClass().add("postCard");
        card.setPrefWidth(cardW);
        card.setMaxWidth(cardW);

        Image img = loadActivityImage(p.getImage());
        if (img == null) img = loadActivityImage("/Frontoffice/images/blog/blog-default.jpg");

        ImageView iv = new ImageView(img);
        iv.setFitWidth(cardW);
        iv.setFitHeight(imgH);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        Rectangle clip = new Rectangle(cardW, imgH);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        iv.setClip(clip);

        Label user = new Label("@" + p.getAuteur().getNom() + " " + p.getAuteur().getPrenom());
        user.setStyle("-fx-font-weight:900; -fx-text-fill:#0f172a;");

        Label titre = new Label(p.getTitre());
        titre.setWrapText(true);
        titre.setStyle("-fx-font-weight:700; -fx-text-fill:#1e293b; -fx-font-size:14;");

        Label caption = new Label(p.getContenu());
        caption.setWrapText(true);
        caption.setStyle("-fx-text-fill:#334155; -fx-font-size:12;");

        Label stats = new Label("❤ Popularité: " + p.getPopularite());
        stats.setStyle("-fx-text-fill:#64748b; -fx-font-weight:800; -fx-font-size:12;");

        Button open = new Button("Open post →");
        open.getStyleClass().add("flashBtn");
        open.setOnAction(e -> goToPosts(new ActionEvent(open, null)));

        VBox body = new VBox(6, user, titre, caption, stats, open);
        body.setPadding(new Insets(10, 12, 12, 12));

        card.getChildren().addAll(iv, body);
        return card;
    }
    private void loadTopPosts() {
        if (blogFlow == null) return;
        blogFlow.getChildren().clear();
        List<Post> topPosts = postService.getTop5PostsByPopularite();

        for (Post p : topPosts) {
            blogFlow.getChildren().add(createUserPostCard(p));
        }
    }
    @FXML
    private void goToCreatePost(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogCreatePost.fxml"));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();

            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Create Post", "Impossible de charger la page de création du post.");
        }
    }
    // =========================
    // TRUST (DEMO)
    // =========================
    private record Testimonial(String name, String text, String stars) {}

    private void loadTestimonialsDemo() {
        if (testimonialsBox == null) return;
        testimonialsBox.getChildren().clear();

        if (lblOverallRating != null) lblOverallRating.setText("4.6 / 5");

        List<Testimonial> list = List.of(
                new Testimonial("Sarah M.", "Smooth booking and great deals. Super easy to use.", "★★★★★"),
                new Testimonial("Ahmed K.", "Loved the destination suggestions. Clean and fast.", "★★★★☆"),
                new Testimonial("Lina R.", "Flash deals are a game-changer!", "★★★★★")
        );

        for (Testimonial t : list) testimonialsBox.getChildren().add(createTestimonialCard(t));
    }

    private VBox createTestimonialCard(Testimonial t) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefSize(260, 130);
        card.setMaxSize(260, 130);

        Label name = new Label(t.name);
        name.setStyle("-fx-text-fill:#101828; -fx-font-weight:900; -fx-font-size:13;");

        Label stars = new Label(t.stars);
        stars.setStyle("-fx-text-fill:#7c3aed; -fx-font-weight:900; -fx-font-size:12;");

        Label text = new Label("“" + t.text + "”");
        text.setWrapText(true);
        text.setStyle("-fx-text-fill:#344054; -fx-font-size:12;");

        card.getChildren().addAll(name, stars, text);
        return card;
    }
    private record ReviewCard(String name, String text, int stars, String target, String when) {}

    private void loadReviewsReal() {
        if (reviewsBox == null) return;
        reviewsBox.getChildren().clear();

        List<Review> all = reviewService.getAll();

        int total = all.size();
        double avg = 0.0;

        if (total > 0) {
            int sum = 0;
            for (Review r : all) sum += r.getNote();
            avg = (double) sum / total;
        }

        if (lblOverallRating != null) {
            lblOverallRating.setText(String.format(Locale.US, "%.1f / 5", avg));
        }
        if (lblReviewsCount != null) {
            lblReviewsCount.setText("Based on " + String.format("%,d", total) + " reviews");
        }

        if (total == 0) {
            reviewsBox.getChildren().add(emptyReviewsCard());
            return;
        }


        for (Review r : all.stream().limit(6).toList()) {
            reviewsBox.getChildren().add(createReviewCardFromDb(r));
        }
    }
    private VBox createReviewCardFromDb(Review r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reviewCard");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        String user = (r.getUserName() == null || r.getUserName().isBlank()) ? "Traveler" : r.getUserName().trim();

        Label name = new Label(user);
        name.setStyle("-fx-font-weight: 900; -fx-text-fill:#0f172a; -fx-font-size: 13;");

        Label stars = new Label(starsText(r.getNote()));
        stars.setStyle("-fx-text-fill:#f59e0b; -fx-font-weight: 900;");

        String when = timeAgo(r.getDateAvis());
        Label target = new Label("Activity • " + when);
        target.setStyle("-fx-text-fill:#64748b; -fx-font-size: 12; -fx-font-weight: 800;");

        String txt = (r.getCommentaire() == null || r.getCommentaire().isBlank()) ? "—" : r.getCommentaire().trim();
        if (txt.length() > 130) txt = txt.substring(0, 130) + "...";

        Label text = new Label("“" + txt + "”");
        text.setWrapText(true);
        text.setStyle("-fx-text-fill:#334155; -fx-font-size: 12;");

        card.getChildren().addAll(name, stars, target, text);


        int actId = r.getActiviteId();
        card.setOnMouseClicked(e -> openActivityDetails(actId));

        return card;
    }
    private String starsText(int note) {
        int s = note;
        if (s < 0) s = 0;
        if (s > 5) s = 5;
        return "★".repeat(s) + "☆".repeat(5 - s);
    }

    private String timeAgo(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        long seconds = java.time.Duration.between(dt, java.time.LocalDateTime.now()).getSeconds();

        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        long weeks = days / 7;
        return weeks + " weeks ago";
    }

    private VBox emptyReviewsCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("reviewCard");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        Label t = new Label("No reviews yet");
        t.setStyle("-fx-font-weight: 900; -fx-text-fill:#0f172a; -fx-font-size: 13;");

        Label s = new Label("Be the first to review an activity.");
        s.setStyle("-fx-text-fill:#64748b; -fx-font-weight: 800; -fx-font-size: 12;");

        card.getChildren().addAll(t, s);
        return card;
    }
    // =========================
    // HEADER ACTIONS
    // =========================
    @FXML public void openNotifications(ActionEvent event) { /* TODO */ }

    @FXML
    public void openProfileMenu(ActionEvent e) {
        if (profileMenu == null || btnProfile == null) return;
        if (profileMenu.isShowing()) profileMenu.hide();
        else profileMenu.show(btnProfile, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    @FXML void goToHome(ActionEvent event) { reloadPage(); }

    @FXML void goToDestinations(ActionEvent event) { navigateToDestinations(); }

    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogAllPosts.fxml"));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToactivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivitiesPage.fxml"));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // window controls
    @FXML void closewindow(ActionEvent e) { Stage s = getStageFromEvent(e); if (s != null) s.close(); }
    @FXML void minwindow(ActionEvent e) { Stage s = getStageFromEvent(e); if (s != null) s.setIconified(true); }
    @FXML void maxwindow(ActionEvent e) {
        Stage s = getStageFromEvent(e);
        if (s != null) s.setMaximized(!s.isMaximized());
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                util.Session.clear();
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/loginPage.fxml"));
                    Stage stage = getStageFromEvent(event);
                    if (stage == null) stage = getAnyStage();
                    if (stage != null) util.NavigationUtil.switchScene(stage, root);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Stage getAnyStage() {
        try {
            if (tfWhere != null && tfWhere.getScene() != null) return (Stage) tfWhere.getScene().getWindow();
            if (popularDestinationsFlow != null && popularDestinationsFlow.getScene() != null) return (Stage) popularDestinationsFlow.getScene().getWindow();
            if (flashDealsBox != null && flashDealsBox.getScene() != null) return (Stage) flashDealsBox.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
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
            return getAnyStage();
        } catch (Exception ignored) {}
        return null;
    }

    // =========================
    // NAVIGATION
    // =========================
    private void navigateToDestinations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryBrowsePage.fxml"));
            Parent root = loader.load();

            Stage stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToDestinationsWithSearch(String searchText) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryBrowsePage.fxml"));
            Parent root = loader.load();

            CountryBrowseController controller = loader.getController();
            controller.setSearchText(searchText);

            Stage stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToCity(Ville ville) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CityDetailPage.fxml"));
            Parent root = loader.load();

            CityDetailController controller = loader.getController();
            controller.setVille(ville);

            Stage stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Parent root = loader.load();

            Stage stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // IMAGE HELPERS
    // =========================
    private ImageView createCityImage(Ville ville, double w, double h, double arc) {
        String imageFilename = ville.getImage();
        if (imageFilename == null || imageFilename.trim().isEmpty()) {
            String defaultPath = "/Frontoffice/images/cities/city-default.jpg";
            return createGenericImage(defaultPath, w, h, arc);
        }
        
        String imagePath = "/Frontoffice/images/countries/" + imageFilename;
        return createGenericImage(imagePath, w, h, arc);
    }

    private ImageView createGenericImage(String path, double w, double h, double arc) {
        Image img = null;
        try {
            URL u = getClass().getResource(path);
            if (u == null) u = getClass().getResource("/Frontoffice/images/cities/city-default.jpg");
            if (u != null) img = new Image(u.toExternalForm(), w, h, false, true);
        } catch (Exception ignored) {}

        ImageView iv = new ImageView();
        if (img != null) iv.setImage(img);

        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        iv.setClip(clip);

        return iv;
    }

    private String safeKey(String s) {
        if (s == null) return "city-default";
        return s.toLowerCase(Locale.ROOT)
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("â", "a")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ù", "u")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // =========================
    // OPTIONAL : Profile routes
    // =========================
    @FXML
    private void goToMyProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogProfileView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/Frontoffice/css/blog_styles.css")).toExternalForm()
            );

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML void goToMyPosts(ActionEvent e) { showInfo("My Posts", "My Posts page - implement in your module"); }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToMyActivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyActivitiesPage.fxml"));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage == null) stage = getAnyStage();
            if (stage != null) {
                if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
                else stage.getScene().setRoot(root);
            }

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadFlashSalesActivities() {
        if (flashDealsBox == null) return;

        flashDealsBox.getChildren().clear();

        try {

            activiteService.refreshFlashSales();
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<Activite> list = activiteService.getFlashSales();

        if (list.isEmpty()) {
            if (lblFlashSubtitle != null) lblFlashSubtitle.setText("No flash deals available right now.");
            if (lblFlashTitle != null) lblFlashTitle.setText("Flash Deals");
            return;
        }

// calcule le max % réel
        int maxPercent = list.stream()
                .mapToInt(a -> computePercent(a.getPrix(), a.getFlashPrice()))
                .max()
                .orElse(0);

// update titre
        if (lblFlashTitle != null) {
            if (maxPercent > 0) lblFlashTitle.setText("Flash Deals — up to -" + maxPercent + "%");
            else lblFlashTitle.setText("Flash Deals");
        }

        if (lblFlashSubtitle != null) lblFlashSubtitle.setText("Limited-time offers you don’t want to miss");
        for (Activite a : list.stream().limit(8).toList()) {
            flashDealsBox.getChildren().add(createFlashActivityCard(a));
        }
    }

    private VBox createFlashActivityCard(Activite a) {

        double cardW = 280;
        double imgH  = 140;

        VBox card = new VBox();
        card.getStyleClass().add("flashCard");
        card.setPrefWidth(cardW);
        card.setMaxWidth(cardW);

        // =========================
        // IMAGE (Activite.image)
        // =========================
        Image img = loadActivityImage(a.getImage());

        // fallback si null
        if (img == null) {
            img = loadActivityImage("/Frontoffice/images/activities/activity-default.jpg");
        }
        if (img == null) {
            img = loadActivityImage("/Frontoffice/images/cities/city-default.jpg");
        }

        ImageView iv = new ImageView(img);
        iv.setFitWidth(cardW);
        iv.setFitHeight(imgH);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        Rectangle clip = new Rectangle(cardW, imgH);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        iv.setClip(clip);

        // Badge -xx%
        int percent = computePercent(a.getPrix(), a.getFlashPrice());
        Label badge = new Label("-" + percent + "%");
        badge.getStyleClass().add("flashBadge");

        StackPane imgWrap = new StackPane(iv);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        imgWrap.getChildren().add(badge);

        // =========================
        // BODY
        // =========================
        VBox body = new VBox(6);
        body.getStyleClass().add("flashBody");

        Label title = new Label(a.getNom() != null ? a.getNom() : "Activity");
        title.getStyleClass().add("flashTitle");

        String dest = "";
        try {
            dest = activiteService.getDestinationDisplayById(a.getDestinationId());
        } catch (Exception ignored) {}

        String days = guessDaysFromDates(a);

        Label sub = new Label("📍 " + ((dest == null || dest.isBlank()) ? "Destination" : dest)
                + " • ⏱ " + days);
        sub.getStyleClass().add("flashSub");

        Region sep = new Region();
        sep.getStyleClass().add("flashSep");

        // Prix TND
        Label oldP = new Label(formatTND(a.getPrix()));
        oldP.getStyleClass().add("flashOld");

        Double flash = a.getFlashPrice();
        Label newP = new Label(formatTND(flash != null ? flash : a.getPrix()));
        newP.getStyleClass().add("flashNew");

        HBox prices = new HBox(10, oldP, newP);
        prices.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btn = new Button("View activity →");
        btn.getStyleClass().add("flashBtn");
        btn.setOnAction(e -> openActivityDetails(a.getId()));

        body.getChildren().addAll(title, sub, sep, prices, spacer, btn);

        card.getChildren().addAll(imgWrap, body);

        card.setOnMouseClicked(e -> openActivityDetails(a.getId()));
        return card;
    }

    private int computePercent(double normal, Double flash) {
        if (flash == null || normal <= 0) return 0;

        double ratio = flash / normal;


        if (ratio <= 0.71) return 30;
        if (ratio <= 0.81) return 20;


        int pr = (int) Math.round((1 - ratio) * 100.0);
        if (pr < 0) pr = 0;
        if (pr > 90) pr = 90;
        return pr;
    }

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
    private Image loadFromFileSmart(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return null;

        try {
            String p = rawPath.trim();

            // Chemin Windows possible
            p = p.replace("\\", "/");

            File f = new File(p);
            if (!f.isAbsolute()) {
                // si tu stockes juste "zaghouan.jpg", on cherche dans user.dir/images ou uploads (à adapter)
                File f1 = new File(System.getProperty("user.dir"), p);
                if (f1.exists()) return new Image(f1.toURI().toString(), true);

                // option: dossier uploads
                File f2 = new File(System.getProperty("user.dir") + File.separator + "uploads", p);
                if (f2.exists()) return new Image(f2.toURI().toString(), true);

                return null;
            }

            if (f.exists()) {
                return new Image(f.toURI().toString(), true);
            }
        } catch (Exception ignored) {}

        return null;
    }
    private String formatTND(Double value) {
        if (value == null) return "—";
        return String.format(Locale.US, "%,.0f TND", value); // 45 TND / 1,250 TND
    }
    private String guessDaysFromDates(Activite a) {
        try {
            if (a.getDateDebut() != null && a.getDateFin() != null) {
                long d = java.time.Duration.between(a.getDateDebut(), a.getDateFin()).toDays();
                if (d <= 0) d = 1;
                return d + " days";
            }
        } catch (Exception ignored) {}
        return "—";
    }

    private void openActivityDetails(int activiteId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivityDetailsPage.fxml"));
            Parent root = loader.load();

            ActivityDetailsController controller = loader.getController();


            Activite act = activiteService.getById(activiteId);
            if (act == null) {
                showInfo("Activity", "Activity not found (id=" + activiteId + ")");
                return;
            }


            controller.setActivity(act);

            Stage stage = getAnyStage();
            if (stage == null) stage = (Stage) ((Node) flashDealsBox).getScene().getWindow();

            if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Activity", "Error opening activity details.");
        }
    }
}
