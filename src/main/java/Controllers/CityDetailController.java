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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import java.sql.Time;
import java.time.LocalTime;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import models.Pays;
import models.Ville;
import services.AttractionService;
import models.Attraction;
import services.VilleService;
import java.util.Locale;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CityDetailController {

    @FXML
    private Label cityNameLabel;

    @FXML
    private Label countryNameLabel;

    @FXML
    private ImageView cityImageView;

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
    private FlowPane attractionsFlowPane;

    @FXML
    private Label noAttractionsLabel;

    private Ville currentVille;
    private final AttractionService attractionService = new AttractionService();
    private final VilleService villeService = new VilleService();

    public void setVille(Ville ville) {
        this.currentVille = ville;

        // increment DB counter each time city detail is opened
        villeService.incrementVisitCount(ville.getId());

        // keep in-memory value in sync for immediate UI display
        ville.setVisitCount(ville.getVisitCount() + 1);

        displayCityInfo();
        loadAttractions();
    }

    private void displayCityInfo() {
        cityNameLabel.setText(currentVille.getNom());
        
        // Load city image
        loadCityImage();
        
        // Fetch country name
        String countryName = "Unknown Country";
        // In a real app, you would fetch the country object. For now, placeholder or service call.
        // Assuming we can get it from a service or passed in.
        // For simplicity, let's just display a placeholder or fetch if possible.
        // countryNameLabel.setText(countryName); 
        
        regionLabel.setText("Region"); // Placeholder as Region is not in Ville model currently or needs to be fetched
        tourismTypeLabel.setText(currentVille.getTypeTourisme());
        seasonLabel.setText(currentVille.getSaison());
        popularityLabel.setText("Popular"); // Placeholder
    }

    private void loadAttractions() {
        // Get attractions for current city from database
        List<Attraction> attractions = attractionService.getByVilleId(currentVille.getId());
        
        if (attractions.isEmpty()) {
            noAttractionsLabel.setVisible(true);
            noAttractionsLabel.setText("No attractions available for this city");
            return;
        }
        
        // Hide "no attractions" label
        noAttractionsLabel.setVisible(false);
        
        // Display attractions in UI
        attractionsFlowPane.getChildren().clear();
        for (Attraction attraction : attractions) {
            VBox attractionCard = createAttractionCard(attraction);
            attractionsFlowPane.getChildren().add(attractionCard);
        }
    }
    
    private VBox createAttractionCard(Attraction attraction) {
        VBox card = new VBox(10);
        card.setPrefSize(320, 180);
        card.setStyle("-fx-background-color: #ffffff; " +
                "-fx-border-radius: 20; " +
                "-fx-background-radius: 20; " +
                "-fx-border-color: #e5e7eb; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 8); " +
                "-fx-cursor: hand; " +
                "-fx-padding: 20; " +
                "-fx-border-insets: 2;");
        
        // Header with icon and name
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        // Type icon
        Label typeIcon = new Label(getIconForType(attraction.getType()));
        typeIcon.setFont(Font.font("System", 24));
        typeIcon.setTextFill(Color.web("#6366f1"));
        
        // Attraction Name
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(attraction.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#1e293b"));
        
        nameBox.getChildren().add(nameLabel);
        headerBox.getChildren().addAll(typeIcon, nameBox);
        
        // Status badge based on current time
        boolean isOpen = checkIfAttractionIsOpen(attraction);
        Label statusBadge = new Label(isOpen ? "OPEN" : "CLOSED");
        statusBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
        statusBadge.setTextFill(Color.WHITE);
        statusBadge.setStyle(isOpen ? 
            "-fx-background-color: #10b981; " : "-fx-background-color: #ef4444;");
        statusBadge.setPadding(new Insets(4, 8, 4, 8));
        statusBadge.setStyle(statusBadge.getStyle() + " -fx-background-radius: 12;");
        
        // Description
        String desc = attraction.getDescription();
        if (desc != null && desc.length() > 80) {
            desc = desc.substring(0, 80) + "...";
        }
        Label descLabel = new Label(desc);
        descLabel.setFont(Font.font("System", 11));
        descLabel.setTextFill(Color.web("#64748b"));
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);
        
        // Opening hours
        String hoursText = formatHours(attraction.getHeureOuverture(), attraction.getHeureFermeture());
        Label hoursLabel = new Label(hoursText);
        hoursLabel.setFont(Font.font("System", 10));
        hoursLabel.setTextFill(Color.web("#6b7280"));
        
        // Price and rating row
        HBox infoRow = new HBox(15);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        // Price
        String priceText = (attraction.getPrix() > 0) ? 
            String.format(Locale.US, "%.0f TND", attraction.getPrix()) : "Free";
        Label priceLabel = new Label(priceText);
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        priceLabel.setTextFill(Color.web("#059669"));
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        infoRow.getChildren().addAll(priceLabel, spacer);
        
        card.getChildren().addAll(headerBox, statusBadge, descLabel, hoursLabel, infoRow);
        
        // Enhanced hover effects
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #f0f9ff; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-border-color: #3b82f6; " +
                    "-fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.15), 25, 0, 0, 10); " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 20; " +
                    "-fx-border-insets: 2;");
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #ffffff; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-border-color: #e5e7eb; " +
                    "-fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 8); " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 20; " +
                    "-fx-border-insets: 2;");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
        
        // Click handler for details
        card.setOnMouseClicked(e -> {
            showAttractionDetails(attraction);
        });
        
        return card;
    }
    
    private String getIconForType(String type) {
        if (type == null) return "•";
        switch (type.toLowerCase()) {
            case "museum": return "M";
            case "park": return "P";
            case "restaurant": return "R";
            case "shopping": return "S";
            case "beach": return "B";
            case "mountain": return "M";
            case "historical": return "H";
            case "entertainment": return "E";
            case "nature": return "N";
            case "sports": return "S";
            case "cultural": return "C";
            default: return "•";
        }
    }
    
    private boolean checkIfAttractionIsOpen(Attraction attraction) {
        try {
            Time openTime = attraction.getHeureOuverture();
            Time closeTime = attraction.getHeureFermeture();
            
            if (openTime == null || closeTime == null) {
                return !attraction.isEstFerme(); // Fallback to database flag
            }
            
            LocalTime now = LocalTime.now();
            LocalTime open = openTime.toLocalTime();
            LocalTime close = closeTime.toLocalTime();
            
            // Check if current time is within opening hours
            return !now.isBefore(open) && !now.isAfter(close);
        } catch (Exception e) {
            return !attraction.isEstFerme(); // Fallback to database flag on error
        }
    }
    
    private String formatHours(Time open, Time close) {
        if (open == null || close == null) return "Hours not available";
        return String.format("%s - %s", 
            open.toString().substring(0, 5), close.toString().substring(0, 5));
    }
    
    private void loadCityImage() {
        try {
            // Try to load city image from database field
            String imageFilename = currentVille.getImage();
            if (imageFilename != null && !imageFilename.trim().isEmpty()) {
                String imagePath = "/Frontoffice/images/countries/" + imageFilename;
                URL url = getClass().getResource(imagePath);
                if (url != null) {
                    cityImageView.setImage(new Image(url.toExternalForm()));
                    System.out.println("DEBUG: City image loaded from database: " + imagePath);
                    return;
                }
            }
            
            // Try city name-based image
            String cityName = currentVille.getNom().toLowerCase().replace(" ", "_");
            String imagePath = "/Frontoffice/images/countries/" + cityName + ".jpg";
            URL url = getClass().getResource(imagePath);
            if (url != null) {
                cityImageView.setImage(new Image(url.toExternalForm()));
                System.out.println("DEBUG: City image loaded by name: " + imagePath);
                return;
            }
            
            // Hide image if no suitable image found
            cityImageView.setVisible(false);
            System.out.println("DEBUG: No city image found, hiding ImageView");
        } catch (Exception e) {
            System.out.println("DEBUG: Error loading city image: " + e.getMessage());
            cityImageView.setVisible(false);
        }
    }

    private void showAttractionDetails(Attraction attraction) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(attraction.getNom());
        alert.setHeaderText(null);
        alert.setContentText(String.format(
            "Type: %s\nPrice: %.0f TND\nStatus: %s\n\n%s", 
            attraction.getType(), attraction.getPrix(), 
            attraction.isEstFerme() ? "Currently Closed" : "Currently Open",
            formatHours(attraction.getHeureOuverture(), attraction.getHeureFermeture())
        ));
        alert.showAndWait();
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryDetailPage.fxml"));
            Parent root = loader.load();
            
            // Get the country detail controller and pass the Pays object
            CountryDetailController controller = loader.getController();
            services.PaysService paysService = new services.PaysService();
            Pays country = paysService.getById(currentVille.getPaysId());
            controller.setPays(country);
            
            Stage stage = (Stage) cityNameLabel.getScene().getWindow();
            if (stage.getScene() == null) {
                util.NavigationUtil.switchScene(stage, root);
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) cityNameLabel.getScene().getWindow();
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
}

