package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * POSTS PAGE CONTROLLER - TEMPLATE FOR POSTS MODULE
 * 
 * This is a MINIMAL controller - just for navigation.
 * The Posts module team will add their content here.
 * 
 * TODO FOR POSTS TEAM:
 * 1. Create Post.java model
 * 2. Create PostService.java 
 * 3. Add loadRecentPosts() method
 * 4. Add loadPopularPosts() method
 * 5. Add createPostCard() method
 * 6. Uncomment initialize() and add your loading logic
 */
public class PostsPageController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane recentPostsFlowPane;  // Container for recent posts

    @FXML
    private FlowPane popularPostsFlowPane;  // Container for popular posts

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // TODO: Posts team - add your loading logic here
        // Example:
        // loadRecentPosts();
        // loadPopularPosts();
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            System.out.println("Search posts: " + searchText);
            // TODO: Implement post search
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        navigateToPage(event, "/Frontoffice/HomePage.fxml");
    }

    @FXML
    void goToDestinations(ActionEvent event) {
        navigateToPage(event, "/Frontoffice/CountryBrowsePage.fxml");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        // Already on posts page
        System.out.println("Already on Posts page");
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
        showInfo("Profile", "My Profile page - implement in User module");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("📝 My Posts - to be implemented");
        showInfo("My Posts", "Show posts created by current user");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        navigateToPage(event, "/Frontoffice/MyReservation.fxml");
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
                showInfo("Logged Out", "Successfully logged out!");
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

    private void navigateToPage(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // Use setRoot to keep window size
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error", "Could not load page: " + fxmlPath);
        }
    }
}
