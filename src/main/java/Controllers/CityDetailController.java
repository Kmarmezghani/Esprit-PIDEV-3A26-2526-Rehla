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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Attraction;
import models.Pays;
import models.Ville;
import services.AttractionService;
import services.PaysService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CityDetailController {

    @FXML
    private Label cityNameLabel;

    @FXML
    private Label countryNameLabel;

    @FXML
    private Label regionLabel;

    @FXML
    private Label tourismTypeLabel;

    @FXML
    private Label seasonLabel;

    @FXML
    private Label popularityLabel;

    @FXML
    private Label attractionsCountLabel;

    @FXML
    private Label noAttractionsLabel;

    @FXML
    private FlowPane attractionsFlowPane;

    private Ville currentVille;
    private Pays currentPays;
    private final AttractionService attractionService = new AttractionService();
    private final PaysService paysService = new PaysService();

    public void setVille(Ville ville) {
        this.currentVille = ville;
        
        // Get country info
        currentPays = paysService.getById(ville.getPaysId());
        
        displayCityInfo();
        loadAttractions();
    }

    private void displayCityInfo() {
        cityNameLabel.setText(currentVille.getNom());
        countryNameLabel.setText(currentPays != null ? currentPays.getNom() : "Unknown");
        regionLabel.setText(currentVille.getRegion());
        tourismTypeLabel.setText(currentVille.getTypeTourisme());
        seasonLabel.setText(currentVille.getSaison());
        popularityLabel.setText(currentVille.getPopularite() + "/100");
    }

    private void loadAttractions() {
        List<Attraction> attractions = attractionService.getAll().stream()
                .filter(a -> a.getVilleId() == currentVille.getId())
                .collect(Collectors.toList());

        if (attractions.isEmpty()) {
            noAttractionsLabel.setVisible(true);
            attractionsCountLabel.setText("No attractions available yet");
        } else {
            noAttractionsLabel.setVisible(false);
            attractionsCountLabel.setText("Explore " + attractions.size() + " amazing " +
                    (attractions.size() == 1 ? "attraction" : "attractions"));

            for (Attraction attraction : attractions) {
                VBox attractionCard = createAttractionCard(attraction);
                attractionsFlowPane.getChildren().add(attractionCard);
            }
        }
    }

    private VBox createAttractionCard(Attraction attraction) {
        VBox card = new VBox(12);
        card.setPrefSize(310, 340);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 20; " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 6);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28));

        // Attraction icon/emoji based on type
        String emoji = getAttractionEmoji(attraction.getType());
        Label iconLabel = new Label(emoji);
        iconLabel.setFont(Font.font("System", 60));

        Label nameLabel = new Label(attraction.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#223f91"));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(260);

        Label typeLabel = new Label(attraction.getType());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        typeLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 6 12; -fx-background-radius: 12;");
        typeLabel.setTextFill(Color.web("#1976D2"));

        Label descLabel = new Label(attraction.getDescription());
        descLabel.setWrapText(true);
        descLabel.setFont(Font.font("System", 13));
        descLabel.setTextFill(Color.GRAY);
        descLabel.setMaxWidth(250);
        descLabel.setMaxHeight(50);

        Label priceLabel = new Label("💰 " + String.format("%.2f", attraction.getPrix()) + " TND");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceLabel.setTextFill(Color.web("#1976D2"));

        Label hoursLabel = new Label("🕒 " + attraction.getHoraires());
        hoursLabel.setFont(Font.font("System", 12));
        hoursLabel.setTextFill(Color.web("#5a6c9a"));

        Button bookButton = new Button("Book Now");
        bookButton.setStyle("-fx-background-color: #3A5BC7; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(58,91,199,0.4), 10, 0, 0, 3);");
        bookButton.setPadding(new Insets(12, 30, 12, 30));
        bookButton.setOnAction(e -> handleBooking(attraction));

        card.getChildren().addAll(iconLabel, nameLabel, typeLabel, descLabel, 
                priceLabel, hoursLabel, bookButton);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, #e8f5e9, white); " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.3), 25, 0, 0, 8);");
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 6);");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private String getAttractionEmoji(String type) {
        if (type == null) return "🎯";
        
        String lowerType = type.toLowerCase();
        if (lowerType.contains("museum") || lowerType.contains("musée")) return "🏛️";
        if (lowerType.contains("park") || lowerType.contains("parc")) return "🌳";
        if (lowerType.contains("beach") || lowerType.contains("plage")) return "🏖️";
        if (lowerType.contains("restaurant")) return "🍽️";
        if (lowerType.contains("monument")) return "🗿";
        if (lowerType.contains("tower") || lowerType.contains("tour")) return "🗼";
        if (lowerType.contains("castle") || lowerType.contains("château")) return "🏰";
        if (lowerType.contains("temple")) return "⛩️";
        if (lowerType.contains("church") || lowerType.contains("église")) return "⛪";
        if (lowerType.contains("market") || lowerType.contains("marché")) return "🛍️";
        if (lowerType.contains("mountain") || lowerType.contains("montagne")) return "⛰️";
        if (lowerType.contains("water") || lowerType.contains("eau")) return "💧";
        if (lowerType.contains("sport")) return "⚽";
        if (lowerType.contains("shopping")) return "🛒";
        return "🎯";
    }

    private void handleBooking(Attraction attraction) {
        // TODO: Implement booking functionality
        System.out.println("Booking attraction: " + attraction.getNom());
        // This could open a booking form or navigate to reservation page
    }

    @FXML
    void goBack(ActionEvent event) {
        // Go back to country page
        if (currentPays != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/CountryDetailPage.fxml"));
                Parent root = loader.load();
                
                CountryDetailController controller = loader.getController();
                controller.setPays(currentPays);
                
                Stage stage = (Stage) attractionsFlowPane.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) attractionsFlowPane.getScene().getWindow();
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

    @FXML
    void handleProfile(ActionEvent event) {
        System.out.println("Profile clicked");
    }
    
    // ⭐ RESERVATION FEATURE - INTEGRATION POINT
    @FXML
    void handleBookCity(ActionEvent event) {
        if (currentVille == null) {
            System.out.println("No city selected");
            return;
        }
        
        // ════════════════════════════════════════════════════════════════
        // TODO: RESERVATION TEAM - UNCOMMENT THIS WHEN READY!
        // ════════════════════════════════════════════════════════════════
        // Prerequisites:
        // 1. Create: src/main/resources/ReservationForm.fxml
        // 2. Create: src/main/java/Controllers/ReservationFormController.java
        // 3. Uncomment the code below
        // ════════════════════════════════════════════════════════════════
        
        /*
        try {
            // Load the reservation form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();
            
            // Pass the selected city to the form
            ReservationFormController controller = loader.getController();
            controller.setVille(currentVille);  // ← Passes city data!
            
            // Show as modal popup
            Stage popupStage = new Stage();
            popupStage.setTitle("Book " + currentVille.getNom());
            popupStage.setScene(new Scene(root));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.showAndWait();
            
        } catch (IOException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setContentText("Could not load reservation form: " + e.getMessage());
            errorAlert.showAndWait();
        }
        */
        
        // ════════════════════════════════════════════════════════════════
        // TEMPORARY PLACEHOLDER (Remove when reservation form is ready)
        // ════════════════════════════════════════════════════════════════
        
        System.out.println("📅 Booking: " + currentVille.getNom() + " (ID: " + currentVille.getId() + ")");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Reservation Feature");
        alert.setContentText("You are booking: " + currentVille.getNom() + "\n\n" +
                           "This feature will be completed by the Reservation team.\n" +
                           "Once they create ReservationForm.fxml, uncomment the code above!");
        alert.showAndWait();
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
}
