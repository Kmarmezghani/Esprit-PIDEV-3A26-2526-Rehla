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
import javafx.stage.Modality;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Pays;
import models.Ville;
import services.AttractionService;
import services.VilleService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CountryDetailController {

    @FXML
    private Label countryFlagLabel;

    @FXML
    private Label countryNameLabel;

    @FXML
    private Label countryDescriptionLabel;

    @FXML
    private Label citiesCountLabel;

    @FXML
    private FlowPane citiesFlowPane;

    private Pays currentPays;
    private final VilleService villeService = new VilleService();
    private final AttractionService attractionService = new AttractionService();

    public void setPays(Pays pays) {
        this.currentPays = pays;
        displayCountryInfo();
        loadCities();
    }

    private void displayCountryInfo() {
        countryFlagLabel.setText(getCountryEmoji(currentPays.getNom()));
        countryNameLabel.setText(currentPays.getNom());
        countryDescriptionLabel.setText(currentPays.getDescription());
    }

    private void loadCities() {
        List<Ville> cities = villeService.getAll().stream()
                .filter(v -> v.getPaysId() == currentPays.getId())
                .collect(Collectors.toList());

        citiesCountLabel.setText("Choose from " + cities.size() + " amazing " + 
                (cities.size() == 1 ? "city" : "cities"));

        for (Ville ville : cities) {
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

        // City icon
        Label iconLabel = new Label("🏙️");
        iconLabel.setFont(Font.font("System", 50));

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

        Label attractionsLabel = new Label("🎯 " + attractionCount + " attractions");
        attractionsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        attractionsLabel.setTextFill(Color.web("#3A5BC7"));

        // Book button
        Button bookButton = new Button("Book Now");
        bookButton.setPrefWidth(220);
        bookButton.setStyle("-fx-background-color: #3A5BC7; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14; " +
                "-fx-padding: 12 30; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(58,91,199,0.4), 10, 0, 0, 3);");
        
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

        card.getChildren().addAll(iconLabel, nameLabel, typeLabel, 
                seasonLabel, attractionsLabel, bookButton);

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

        // Navigate to city detail (only on card click, not button)
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != bookButton) {
                navigateToCity(ville);
            }
        });

        return card;
    }

    private String getCountryEmoji(String countryName) {
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
    void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) citiesFlowPane.getScene().getWindow();
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
            
            Stage stage = (Stage) citiesFlowPane.getScene().getWindow();
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
        System.out.println("📱 My Profile - to be implemented");
        showInfo("Profile", "My Profile page will show user information,\nprofile picture, and settings.");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("📝 My Posts - to be implemented");
        showInfo("My Posts", "This will show all blog posts\ncreated by the current user.");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        System.out.println("📅 My Reservations - to be implemented");
        showInfo("My Reservations", "This will show all reservations\nmade by the current user.");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        System.out.println("🚪 Logging out...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("✅ User logged out");
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
