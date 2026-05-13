package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URL;
import models.Pays;
import models.Ville;
import services.AttractionService;
import services.VilleService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CountryDetailController {

    @FXML
    private Label countryNameLabel;
    @FXML
    private Label countryDescriptionLabel;
    @FXML
    private ImageView countryImageView;
    @FXML
    private Label countryFlagLabel;
    @FXML
    private Label citiesSectionLabel;
    @FXML
    private Label citiesCountLabel;
    @FXML
    private FlowPane citiesFlowPane;

    private Pays currentPays;
    private final VilleService villeService = new VilleService();
    private final AttractionService attractionService = new AttractionService();
    private final services.PaysService paysService = new services.PaysService();

    public void setPays(Pays pays) {
        this.currentPays = pays;
        displayCountryInfo();
        loadCities();
    }

    private void displayCountryInfo() {
        countryFlagLabel.setText(getCountryEmoji(currentPays.getNom()));
        countryNameLabel.setText(currentPays.getNom());
        countryDescriptionLabel.setText(currentPays.getDescription());
        
        // Load country image
        loadCountryImage();
        
        // Increment country visit count
        paysService.incrementVisitCount(currentPays.getId());
    }

    private void loadCities() {
        List<Ville> cities = villeService.getAll().stream()
                .filter(v -> v.getPaysId() == currentPays.getId())
                .collect(Collectors.toList());

        citiesCountLabel.setText("Choose from " + cities.size() + " amazing " +
                (cities.size() == 1 ? "city" : "cities"));

        for (Ville ville : cities) {
            // Increment city visit count when displaying card
            villeService.incrementVisitCount(ville.getId());
            
            VBox cityCard = createCityCard(ville);
            citiesFlowPane.getChildren().add(cityCard);
        }
    }

    private VBox createCityCard(Ville ville) {
        VBox card = new VBox(15);
        card.setPrefSize(290, 280);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 20; " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0, 0, 5);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28));

        // City Image
        ImageView cityImageView = createCityImage(ville);

        Label nameLabel = new Label(ville.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        nameLabel.setTextFill(Color.web("#1a237e"));

        Label typeLabel = new Label(ville.getTypeTourisme());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        typeLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 8 15; -fx-background-radius: 15;");
        typeLabel.setTextFill(Color.web("#1976D2"));

        Label seasonLabel = new Label("Best: " + ville.getSaison());
        seasonLabel.setFont(Font.font("System", 12));
        seasonLabel.setTextFill(Color.web("#3A5BC7"));

        // Count attractions
        long attractionCount = attractionService.getAll().stream()
                .filter(a -> a.getVilleId() == ville.getId())
                .count();

        Label attractionsLabel = new Label("Attractions: " + attractionCount);
        attractionsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        attractionsLabel.setTextFill(Color.web("#3A5BC7"));

        Button bookButton = new Button("Book Now");

        bookButton.setStyle(
                "-fx-background-color: #3A5BC7;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 25;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(58,91,199,0.3), 8, 0, 0, 2);"
        );

        bookButton.setOnMouseEntered(e ->
                bookButton.setStyle(
                        "-fx-background-color: #2d4a9e;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13;" +
                                "-fx-padding: 10 25;" +
                                "-fx-background-radius: 8;"
                )
        );

        bookButton.setOnMouseExited(e ->
                bookButton.setStyle(
                        "-fx-background-color: #3A5BC7;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13;" +
                                "-fx-padding: 10 25;" +
                                "-fx-background-radius: 8;"
                )
        );

        // OPEN BOOKING PAGE
        bookButton.setOnAction(e -> {
            e.consume();
            handleAddReservation(e, ville);
        });


        card.getChildren().addAll(cityImageView, nameLabel, typeLabel,
                seasonLabel, attractionsLabel,bookButton);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff9c4, white); " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(255,193,7,0.35), 22, 0, 0, 7); " +
                    "-fx-cursor: hand;");
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0, 0, 5);");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Navigate to city detail
        card.setOnMouseClicked(e -> {
            navigateToCity(ville);
        });

        return card;
    }

    private ImageView createCityImage(Ville ville) {
        // Check if image filename is null or empty
        String imageFilename = ville.getImage();
        if (imageFilename == null || imageFilename.trim().isEmpty()) {
            System.out.println("DEBUG: No city image filename in database, using fallback");
            return createDefaultCityImage();
        }
        
        String imagePath = "/Frontoffice/images/countries/" + imageFilename;
        
        System.out.println("DEBUG: Trying to load city image: " + imagePath);
        
        try {
            URL url = getClass().getResource(imagePath);
            System.out.println("DEBUG: City image URL resolved: " + (url != null ? url.toString() : "NULL"));
            
            if (url != null) {
                ImageView imageView = new ImageView(new Image(url.toExternalForm()));
                imageView.setFitWidth(290);
                imageView.setFitHeight(140);
                imageView.setPreserveRatio(false);
                System.out.println("DEBUG: City image loaded successfully");
                return imageView;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception loading city image: " + e.getMessage());
            // Fall back to default
        }
        
        System.out.println("DEBUG: Using fallback city image");
        // Fallback to default image
        return createDefaultCityImage();
    }

    private ImageView createDefaultCityImage() {
        String defaultPath = "/Frontoffice/images/cities/city-default.jpg";
        URL url = getClass().getResource(defaultPath);
        ImageView imageView = new ImageView(new Image(url.toExternalForm()));
        imageView.setFitWidth(290);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(false);
        return imageView;
    }

    private void loadCountryImage() {
        try {
            // Try to load country image based on country name
            String countryName = currentPays.getNom().toLowerCase().replace(" ", "_");
            String imagePath = "/Frontoffice/images/countries/" + countryName + ".jpg";
            
            System.out.println("DEBUG: Trying to load country image: " + imagePath);
            URL url = getClass().getResource(imagePath);
            if (url != null) {
                countryImageView.setImage(new Image(url.toExternalForm()));
                countryImageView.setVisible(true);
                System.out.println("DEBUG: Country image loaded successfully: " + imagePath);
                return;
            }
            
            // Try alternative names
            loadAlternativeCountryImages();
        } catch (Exception e) {
            System.out.println("DEBUG: Error loading country image: " + e.getMessage());
            // Fallback to default or hide image
            countryImageView.setVisible(false);
        }
    }
    
    private void loadAlternativeCountryImages() {
        // Try to find any image that starts with the country name
        String countryName = currentPays.getNom().toLowerCase();
        String[] alternatives = {
            countryName.replace(" ", "_"),
            countryName.replace(" ", ""),
            countryName.substring(0, 3).toLowerCase()
        };
        
        for (String alt : alternatives) {
            String imagePath = "/Frontoffice/images/countries/" + alt + ".jpg";
            URL url = getClass().getResource(imagePath);
            if (url != null) {
                countryImageView.setImage(new Image(url.toExternalForm()));
                countryImageView.setVisible(true);
                System.out.println("DEBUG: Alternative country image loaded: " + imagePath);
                return;
            }
        }
        
        // Try to find any image that contains the country name
        try {
            String[] imageFiles = {
                "france_69f1abc8474ed.jpg", "italy_69f2dacbbe77d.jpg", 
                "algeria_69f1ae563d4a6.jpg", "tunisia", "spain", "germany",
                "china_69f2cba778fd0.jpg", "australia_69f2901959bfc.jpg",
                "argentina_69f2925d7faae.jpg", "belgium_69f291419d7cf.jpg"
            };
            
            for (String imageFile : imageFiles) {
                if (imageFile.toLowerCase().contains(countryName)) {
                    String imagePath = "/Frontoffice/images/countries/" + imageFile;
                    URL url = getClass().getResource(imagePath);
                    if (url != null) {
                        countryImageView.setImage(new Image(url.toExternalForm()));
                        countryImageView.setVisible(true);
                        System.out.println("DEBUG: Country image found by partial match: " + imagePath);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error in alternative image search: " + e.getMessage());
        }
        
        // Hide image if no suitable image found
        countryImageView.setVisible(false);
        System.out.println("DEBUG: No country image found for " + currentPays.getNom() + ", hiding ImageView");
    }

    private String getCountryEmoji(String countryName) {
        switch (countryName.toLowerCase()) {
            case "france": return "FR";
            case "tunisia": case "tunisie": return "TN";
            case "japan": case "japon": return "JP";
            case "italy": case "italie": return "IT";
            case "spain": case "espagne": return "ES";
            case "usa": case "united states": case "etats-unis": return "US";
            case "uk": case "united kingdom": case "royaume-uni": return "GB";
            case "germany": case "allemagne": return "DE";
            case "morocco": case "maroc": return "MA";
            case "egypt": case "egypte": return "EG";
            default: return "World";
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) citiesFlowPane.getScene().getWindow();
            if (stage.getScene() == null) {
                util.NavigationUtil.switchScene(stage, root);
            } else {
                stage.getScene().setRoot(root);
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

            Stage stage = (Stage) citiesFlowPane.getScene().getWindow();
            if (stage.getScene() == null) {
                util.NavigationUtil.switchScene(stage, root);
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Window Controls
    @FXML
    void closewindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void minwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    void maxwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    // Profile Menu Actions
    @FXML
    void goToMyProfile(ActionEvent event) {
        System.out.println("My Profile - to be implemented");
        showInfo("Profile", "My Profile page will show user information,\nprofile picture, and settings.");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("My Posts - to be implemented");
        showInfo("My Posts", "This will show all blog posts\ncreated by the current user.");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        System.out.println("My Reservations - to be implemented");
        showInfo("My Reservations", "This will show all reservations\nmade by the current user.");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        System.out.println("Logging out...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User logged out");
                showInfo("Logged Out", "You have been successfully logged out!");
            }
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleAddReservation(ActionEvent event, Ville ville) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/BookingType.fxml"));

            Parent root = loader.load();

            // 🔥 GET CONTROLLER
            BookingTypeController controller = loader.getController();

            // 🔥 PASS VILLE ID
            controller.setVilleId(ville.getId());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

