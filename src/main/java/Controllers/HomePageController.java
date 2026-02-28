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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Pays;
import models.Ville;
import services.PaysService;
import services.VilleService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class HomePageController implements Initializable {

    // Header
    @FXML private Button btnNotif, btnProfile;
    @FXML private ContextMenu profileMenu;

    // Hero
    @FXML private TextField tfWhere;
    @FXML private DatePicker dpStart;
    @FXML private DatePicker dpEnd;
    @FXML private Spinner<Integer> spTravelers;

    // Flash
    @FXML private HBox flashDealsBox;
    @FXML private Label lblFlashSubtitle;

    // Popular
    @FXML private FlowPane popularDestinationsFlow;

    // Blog
    @FXML private FlowPane blogFlow;

    // Trust
    @FXML private Label lblOverallRating;
    @FXML private HBox testimonialsBox;

    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();
    @FXML
    private TextField searchField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTravelersSpinner();
        setupDefaultDates();

        loadFlashDealsDemo();
        loadPopularDestinationsFromDb();
        loadBlogDemo();
        loadTestimonialsDemo();
    }

    private void setupTravelersSpinner() {
        SpinnerValueFactory<Integer> vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        spTravelers.setValueFactory(vf);
        spTravelers.setEditable(true);
    }

    private void setupDefaultDates() {
        dpStart.setValue(LocalDate.now().plusDays(3));
        dpEnd.setValue(LocalDate.now().plusDays(6));
    }

    // =========================
    // SEARCH
    // =========================
    @FXML
    void handleSearch(ActionEvent event) {
        String where = tfWhere.getText() == null ? "" : tfWhere.getText().trim();
        LocalDate start = dpStart.getValue();
        LocalDate end = dpEnd.getValue();
        Integer travelers = spTravelers.getValue();

        if (where.isEmpty()) { showInfo("Search", "Please enter a city or country."); return; }
        if (start == null || end == null) { showInfo("Search", "Please select your dates."); return; }
        if (end.isBefore(start)) { showInfo("Search", "Check-out date must be after check-in date."); return; }
        if (travelers == null || travelers < 1) { showInfo("Search", "Please select number of travelers."); return; }

        navigateToDestinationsWithSearch(where);
    }

    // =========================
    // FLASH DEALS (DEMO)
    // =========================
    private record Deal(String city, String title, String subtitle, double oldPrice, double newPrice, int percent) {}

    private void loadFlashDealsDemo() {
        if (flashDealsBox == null) return;
        flashDealsBox.getChildren().clear();

        List<Deal> deals = List.of(
                new Deal("paris", "Paris Getaway", "City break · 3 nights", 240, 168, 30),
                new Deal("rome", "Rome Weekend", "Culture · 2 nights", 190, 152, 20),
                new Deal("dubai", "Dubai Escape", "Luxury · 4 nights", 420, 294, 30),
                new Deal("tunis", "Tunis Discovery", "Local vibes · 2 nights", 120, 96, 20)
        );

        for (Deal d : deals) {
            flashDealsBox.getChildren().add(createDealCard(d));
        }

        if (lblFlashSubtitle != null) {
            lblFlashSubtitle.setText("Limited-time offers you don’t want to miss");
        }
    }

    private VBox createDealCard(Deal d) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefSize(260, 230);
        card.setMaxSize(260, 230);

        ImageView img = createCityImage(d.city, 230, 110);
        img.setSmooth(true);

        Label badge = new Label("-" + d.percent + "%");
        badge.getStyleClass().add("badgeDeal");

        HBox top = new HBox(badge);
        top.setAlignment(Pos.TOP_LEFT);

        Label title = new Label(d.title);
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 16));

        Label sub = new Label(d.subtitle);
        sub.setStyle("-fx-text-fill:#6a7aa6; -fx-font-size:12;");

        Label oldP = new Label(String.format("$%.0f", d.oldPrice));
        oldP.getStyleClass().add("priceOld");

        Label newP = new Label(String.format("$%.0f", d.newPrice));
        newP.getStyleClass().add("priceNew");

        HBox prices = new HBox(10, oldP, newP);
        prices.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button("View deal");
        btn.getStyleClass().add("searchBtn");
        btn.setOnAction(e -> showInfo("Flash Deal", "Open deal: " + d.title));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(img, top, title, sub, spacer, prices, btn);
        return card;
    }

    // =========================
    // POPULAR DESTINATIONS (DB)
    // =========================
    private void loadPopularDestinationsFromDb() {
        if (popularDestinationsFlow == null) return;
        popularDestinationsFlow.getChildren().clear();

        List<Ville> villes = villeService.getAll();
        List<Pays> paysList = paysService.getAll();

        Map<Integer, String> paysNameById = paysList.stream()
                .collect(Collectors.toMap(Pays::getId, Pays::getNom, (a, b) -> a));

        List<Ville> top = villes.stream().limit(6).collect(Collectors.toList());

        for (Ville v : top) {
            String paysNom = paysNameById.getOrDefault(v.getPaysId(), "Unknown");
            popularDestinationsFlow.getChildren().add(createDestinationCard(v, paysNom));
        }
    }

    private VBox createDestinationCard(Ville ville, String paysNom) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefSize(300, 240);
        card.setMaxSize(300, 240);

        String key = safeKey(ville.getNom());
        ImageView img = createCityImage(key, 280, 115);

        Label tag = new Label("Very popular");
        tag.setStyle("""
            -fx-background-color:#eef2ff;
            -fx-text-fill:#223f91;
            -fx-font-weight:900;
            -fx-background-radius:999;
            -fx-padding:4 10;
            -fx-font-size:11;
        """);

        Label name = new Label(ville.getNom());
        name.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 18));

        Label meta = new Label("📍 " + paysNom);
        meta.setStyle("-fx-text-fill:#6a7aa6; -fx-font-size:12;");

        Label type = new Label(ville.getTypeTourisme());
        type.setStyle("""
            -fx-background-color:#f6f8ff;
            -fx-text-fill:#223f91;
            -fx-font-weight:800;
            -fx-background-radius:999;
            -fx-padding:4 10;
            -fx-font-size:11;
        """);

        Button btn = new Button("Explore");
        btn.setStyle("""
            -fx-background-color: transparent;
            -fx-border-color: #3A5BC7;
            -fx-text-fill: #3A5BC7;
            -fx-font-weight: 900;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-padding: 8 12;
            -fx-cursor: hand;
        """);
        btn.setOnAction(e -> navigateToCity(ville));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox(10, new Label("Explore now"), new Region(), btn);
        ((Label)bottom.getChildren().get(0)).setStyle("-fx-text-fill:#3A5BC7; -fx-font-weight:900;");
        HBox.setHgrow(bottom.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(img, tag, name, meta, type, spacer, bottom);

        card.setOnMouseClicked(e -> navigateToCity(ville));
        return card;
    }

    // =========================
    // BLOG (DEMO with images)
    // =========================
    private record BlogPost(String key, String title, String subtitle) {}

    private void loadBlogDemo() {
        if (blogFlow == null) return;
        blogFlow.getChildren().clear();

        List<BlogPost> posts = List.of(
                new BlogPost("rome", "Weekend in Rome", "2-day itinerary · best areas to stay"),
                new BlogPost("summer", "Top summer cities", "Where to go this season (budget-friendly)"),
                new BlogPost("budget", "Smart budget guide", "Save money without missing experiences")
        );

        for (BlogPost p : posts) {
            blogFlow.getChildren().add(createBlogCard(p));
        }
    }

    private VBox createBlogCard(BlogPost p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefSize(300, 220);
        card.setMaxSize(300, 220);

        ImageView img = createGenericImage("/Frontoffice/images/blog/blog-default.jpg", 280, 110);

        Label title = new Label(p.title);
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 16));

        Label sub = new Label(p.subtitle);
        sub.setWrapText(true);
        sub.setStyle("-fx-text-fill:#6a7aa6; -fx-font-size:12;");

        Button btn = new Button("Read more");
        btn.getStyleClass().add("searchBtn");
        btn.setOnAction(e -> goToPosts(new ActionEvent(btn, null)));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(img, title, sub, spacer, btn);
        return card;
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

    // =========================
    // CATEGORY FILTERS
    // =========================
    @FXML void filterBeach(ActionEvent e)      { navigateToDestinationsWithSearch("Beach"); }
    @FXML void filterCulture(ActionEvent e)    { navigateToDestinationsWithSearch("Culture"); }
    @FXML void filterMountains(ActionEvent e)  { navigateToDestinationsWithSearch("Mountains"); }
    @FXML void filterGastronomy(ActionEvent e) { navigateToDestinationsWithSearch("Gastronomy"); }
    @FXML void filterAdventure(ActionEvent e)  { navigateToDestinationsWithSearch("Adventure"); }

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

    // Profile menu items
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
    @FXML void goToMyPosts(ActionEvent e) { showInfo("My Posts", "My Posts page - implement in your module"); }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            MenuItem item = (MenuItem) event.getSource();
            Stage stage = (Stage) item.getParentPopup().getOwnerWindow();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToMyActivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyActivitiesPage.fxml"));
            Parent root = loader.load();

            MenuItem item = (MenuItem) event.getSource();
            Stage stage = (Stage) item.getParentPopup().getOwnerWindow();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    Stage stage = (Stage) searchField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // =========================
    // NAVIGATION (same idea as yours)
    // =========================
    @FXML void goToHome(ActionEvent event) { reloadPage(); }

    @FXML
    void goToDestinations(ActionEvent event) { navigateToDestinations(); }
    @FXML
    private void goToProfile(ActionEvent event) {
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





    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogAllPosts.fxml"));
            Parent root = loader.load();


            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/Frontoffice/css/blog_styles.css").toExternalForm()
            );


            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();


        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Posts", "Erreur lors du chargement de blogAllPosts.");
        }
    }

    @FXML
    void goToactivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivitiesPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // window controls
    @FXML void closewindow(ActionEvent e) { ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); }
    @FXML void minwindow(ActionEvent e) { ((Stage)((Node)e.getSource()).getScene().getWindow()).setIconified(true); }
    @FXML void maxwindow(ActionEvent e) {
        Stage s = (Stage)((Node)e.getSource()).getScene().getWindow();
        s.setMaximized(!s.isMaximized());
    }

    private void navigateToDestinations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryBrowsePage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfWhere.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);
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

            Stage stage = (Stage) tfWhere.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

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

            Stage stage = (Stage) popularDestinationsFlow.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfWhere.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // IMAGE HELPERS
    // =========================
    private ImageView createCityImage(String cityKey, double w, double h) {
        String path = "/Frontoffice/images/cities/" + cityKey + ".jpg";
        ImageView iv = createGenericImage(path, w, h);
        iv.setPreserveRatio(false);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setSmooth(true);
        iv.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        return iv;
    }

    private ImageView createGenericImage(String path, double w, double h) {
        Image img;
        try {
            URL u = getClass().getResource(path);
            if (u == null) u = getClass().getResource("/Frontoffice/images/cities/city-default.jpg");
            img = new Image(u.toExternalForm(), w, h, false, true);
        } catch (Exception ex) {
            // fallback empty
            img = null;
        }
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setClip(new javafx.scene.shape.Rectangle(w, h, null)); // simple clip (optional)
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
}