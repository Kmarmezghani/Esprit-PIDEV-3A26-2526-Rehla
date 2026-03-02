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
        // Implement loading attractions logic here
        // List<Attraction> attractions = attractionService.getByVilleId(currentVille.getId());
        // ... display logic ...
        noAttractionsLabel.setVisible(true); // Default for now
    }

    @FXML
    void goBack(ActionEvent event) {
        // Navigate back logic
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryDetailPage.fxml"));
            Parent root = loader.load();
            
            // We need to pass the country back. This requires storing the country in this controller or fetching it.
            // For now, let's just go back to Home or Country Browse if context is lost.
            // Ideally: CountryDetailController controller = loader.getController();
            // controller.setPays(currentVille.getPays()); // Need to fetch Pays object
            
            Stage stage = (Stage) cityNameLabel.getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
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
                stage.setScene(new Scene(root));
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
