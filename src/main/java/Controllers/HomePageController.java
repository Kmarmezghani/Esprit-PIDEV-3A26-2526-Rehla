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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Pays;
import models.Ville;
import services.PaysService;
import services.VilleService;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HomePageController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane countriesFlowPane;

    @FXML
    private FlowPane popularCitiesFlowPane;

    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCountries();
        loadPopularCities();
    }

    private void loadCountries() {
        List<Pays> paysList = paysService.getAll();
        
        for (Pays pays : paysList) {
            VBox countryCard = createCountryCard(pays);
            countriesFlowPane.getChildren().add(countryCard);
        }
    }

    private VBox createCountryCard(Pays pays) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));

        // Country Flag Emoji or Icon
        Label flagLabel = new Label(getCountryEmoji(pays.getNom()));
        flagLabel.setFont(Font.font("System", 60));

        Label nameLabel = new Label(pays.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.web("#223f91"));

        Label continentLabel = new Label("Explore");
        continentLabel.setFont(Font.font("System", 14));
        continentLabel.setTextFill(Color.GRAY);

        // Count cities
        long cityCount = villeService.getAll().stream()
                .filter(v -> v.getPaysId() == pays.getId())
                .count();
        
        Label cityCountLabel = new Label(cityCount + " cities to explore");
        cityCountLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        cityCountLabel.setTextFill(Color.web("#3A5BC7"));

        card.getChildren().addAll(flagLabel, nameLabel, continentLabel, cityCountLabel);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-background-color: #f0f8ff; -fx-cursor: hand;");
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #f0f8ff;", "-fx-background-color: white;"));
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Navigate to country page
        card.setOnMouseClicked(e -> navigateToCountry(pays));

        return card;
    }

    private void loadPopularCities() {
        List<Ville> villes = villeService.getAll();
        
        // Take top 6 (random or first 6 since popularity is removed)
        List<Ville> topCities = villes.stream()
                .limit(6)
                .collect(Collectors.toList());

        for (Ville ville : topCities) {
            VBox cityCard = createCityCard(ville);
            popularCitiesFlowPane.getChildren().add(cityCard);
        }
    }

    private VBox createCityCard(Ville ville) {
        VBox card = new VBox(10);
        card.setPrefSize(220, 180);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));

        Label nameLabel = new Label(ville.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.web("#223f91"));

        // Get country name
        String paysNom = paysService.getAll().stream()
                .filter(p -> p.getId() == ville.getPaysId())
                .map(Pays::getNom)
                .findFirst()
                .orElse("Unknown");

        Label countryLabel = new Label("📍 " + paysNom);
        countryLabel.setFont(Font.font("System", 13));
        countryLabel.setTextFill(Color.GRAY);

        Label typeLabel = new Label(ville.getTypeTourisme());
        typeLabel.setFont(Font.font("System", 12));
        typeLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5 10; -fx-background-radius: 10;");

        // Book button
        Button bookButton = new Button("Book Now");
        bookButton.setStyle("-fx-background-color: #3A5BC7; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 10 25; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(58,91,199,0.3), 8, 0, 0, 2);");
        
        bookButton.setOnMouseEntered(e -> {
            bookButton.setStyle(bookButton.getStyle() + "-fx-background-color: #2d4a9e;");
        });
        
        bookButton.setOnMouseExited(e -> {
            bookButton.setStyle(bookButton.getStyle().replace("-fx-background-color: #2d4a9e;", "-fx-background-color: #3A5BC7;"));
        });
        
        bookButton.setOnAction(e -> {
            e.consume(); // Prevent card click
            handleBookCity(ville);
        });

        card.getChildren().addAll(nameLabel, countryLabel, typeLabel, bookButton);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-background-color: #fffacd; -fx-cursor: hand;");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #fffacd;", "-fx-background-color: white;"));
        });

        // Navigate to city detail (only on card click, not button)
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != bookButton) {
                navigateToCity(ville);
            }
        });

        return card;
    }

    private String getCountryEmoji(String countryName) {
        // Simple emoji mapping
        switch (countryName.toLowerCase()) {
            case "france": return "🇫🇷";
            case "tunisia": case "tunisie": return "🇹🇳";
            case "japan": case "japon": return "🇯🇵";
            case "italy": case "italie": return "🇮🇹";
            case "spain": case "espagne": return "🇪🇸";
            case "usa": case "united states": case "états-unis": return "🇺🇸";
            case "uk": case "united kingdom": case "royaume-uni": return "🇬🇧";
            case "germany": case "allemagne": return "🇩🇪";
            case "morocco": case "maroc": return "🇲🇦";
            case "egypt": case "égypte": return "🇪🇬";
            default: return "🌍";
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            // Navigate to destinations page with search
            navigateToDestinationsWithSearch(searchText);
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        // Already on home, just reload
        reloadPage();
    }

    @FXML
    void goToDestinations(ActionEvent event) {
        navigateToDestinations();
    }

    @FXML
    void goToPosts(ActionEvent event) {
        // TODO: Navigate to posts page
        System.out.println("Posts page - to be implemented");
    }

    private void navigateToCountry(Pays pays) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CountryDetailPage.fxml"));
            Parent root = loader.load();
            
            CountryDetailController controller = loader.getController();
            controller.setPays(pays);
            
            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToCity(Ville ville) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CityDetailPage.fxml"));
            Parent root = loader.load();
            
            CityDetailController controller = loader.getController();
            controller.setVille(ville);
            
            Stage stage = (Stage) popularCitiesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToDestinations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CountryBrowsePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToDestinationsWithSearch(String searchText) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CountryBrowsePage.fxml"));
            Parent root = loader.load();
            
            CountryBrowseController controller = loader.getController();
            controller.setSearchText(searchText);
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
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
        // TODO: Navigate to user profile page
        System.out.println("📱 My Profile - to be implemented");
        showInfo("Profile", "My Profile page will show user information,\nprofile picture, and settings.");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        // TODO: Navigate to user's posts/blogs
        System.out.println("📝 My Posts - to be implemented");
        showInfo("My Posts", "This will show all blog posts\ncreated by the current user.");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        // TODO: Navigate to user's reservations
        System.out.println("📅 My Reservations - to be implemented");
        showInfo("My Reservations", "This will show all reservations\nmade by the current user.");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        // TODO: Implement logout functionality
        System.out.println("🚪 Logging out...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("✅ User logged out");
                // TODO: Navigate to login page
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

    private void handleBookCity(Ville ville) {
        /* RESERVATION TEAM: Uncomment this code when ReservationForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();
            
            ReservationController controller = loader.getController();
            controller.setVille(ville);  // Pass the city object
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Book " + ville.getNom());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        
        // Temporary alert (remove after uncommenting above)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Reservation Feature");
        alert.setContentText("The reservation team will implement this soon!\n\n" +
                            "City: " + ville.getNom() + "\n\n" +
                            "They need to:\n" +
                            "1. Create ReservationForm.fxml\n" +
                            "2. Create ReservationController.java with setVille() method\n" +
                            "3. Uncomment the code in this method");
        alert.showAndWait();
    }
}
