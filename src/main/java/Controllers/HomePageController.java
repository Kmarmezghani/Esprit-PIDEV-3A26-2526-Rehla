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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * HOMEPAGE CONTROLLER - FRONTOFFICE TEMPLATE
 *
 * This controller manages the main landing page of the application.
 *
 * IMPORTANT FOR TEAMMATES:
 * - Destination module code is COMMENTED OUT below
 * - If you have the Destination module (Ville, Pays models), UNCOMMENT those sections
 * - If you DON'T have Destination module, add your own content in initialize() method
 * - The FlowPane containers (countriesFlowPane, popularCitiesFlowPane) are available for any module to use
 */
public class HomePageController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane countriesFlowPane;  // Empty for now - Destination team will populate

    @FXML
    private FlowPane popularCitiesFlowPane;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /* =====================================================
           DESTINATION MODULE: Uncomment to load countries and cities

           loadCountries();
           loadPopularCities();
           ===================================================== */

        // TODO: Add your own module content here
        // Example for Reservation module:
        // loadRecentReservations();

        // Example for Posts module:
        // loadRecentPosts();
    }





    @FXML
    void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            System.out.println("Search: " + searchText);
            // TODO: Implement search for your module
            showInfo("Search", "Search feature - implement for your module");
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        System.out.println("Already on home");
    }

    @FXML
    void goToDestinations(ActionEvent event) {
        System.out.println("Destinations - to be implemented");
        showInfo("Destinations", "Implement navigation to your module page");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PostsPage.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Posts", "Posts page loading...");
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
        showInfo("Profile", "My Profile page - implement in your module");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("📝 My Posts - to be implemented");
        showInfo("My Posts", "My Posts page - implement in your module");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /* ========================================
       TEMPORARY: Module Testing Methods
       DELETE THESE AFTER INTEGRATION!
       ======================================== */

    @FXML
    void frontofficeReservation(ActionEvent event) {
        /* RESERVATION TEAM: Uncomment this code when ReservationForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Réservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        showInfo("Module Réservation",
                "Équipe Réservation:\n\n" +
                        "1. Créer ReservationForm.fxml\n" +
                        "2. Créer ReservationController.java\n" +
                        "3. Décommenter le code dans frontofficeReservation()\n" +
                        "4. Cliquer ce bouton pour tester!\n\n" +
                        "Plus tard: Sera déclenché par le bouton 'Book Now' sur les villes");
    }

    @FXML
    private void handleAddReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajout.fxml"));
            Parent root = loader.load();

            AjouterReservationController controller = loader.getController();

            // Création d'un Stage modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Reservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Après fermeture du popup, naviguer vers MyReservations
            FXMLLoader myResLoader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent myResRoot = myResLoader.load();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(myResRoot));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void frontofficeActivite(ActionEvent event) {
        /* ACTIVITIES TEAM: Uncomment this code when ActivityForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ActivityForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Activité");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        showInfo("Module Activités",
                "Équipe Activités:\n\n" +
                        "1. Créer ActivityForm.fxml\n" +
                        "2. Créer ActivityController.java\n" +
                        "3. Décommenter le code dans frontofficeActivite()\n" +
                        "4. Cliquer ce bouton pour tester!\n\n" +
                        "Plus tard: Montrera les activités pour chaque ville");
    }

}