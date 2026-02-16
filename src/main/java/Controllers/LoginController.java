package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Personne;
import services.PersonneService;
import util.Session;

import java.io.IOException;

public class LoginController {

    @FXML private TextField searchField;   // email
    @FXML private TextField searchField1;  // password

    private final PersonneService personneService = new PersonneService();

    @FXML
    void handleLogin(ActionEvent event) {
        String email = searchField.getText() != null ? searchField.getText().trim() : "";
        String password = searchField1.getText() != null ? searchField1.getText() : "";

        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login", "Please enter your email.");
            return;
        }
        if (password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login", "Please enter your password.");
            return;
        }

        // Admin / backend: root / root → Dashboard
        if ("root".equalsIgnoreCase(email) && "root".equals(password)) {
            Session.setCurrentUser(createRootAdmin());
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Could not open dashboard.");
            }
            return;
        }

        Personne user = personneService.findByEmail(email);
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Login failed", "No account found with this email.");
            return;
        }
        if (!password.equals(user.getMotDePasse())) {
            showAlert(Alert.AlertType.ERROR, "Login failed", "Incorrect password.");
            return;
        }
        if ("inactif".equalsIgnoreCase(user.getStatutCompte())) {
            showAlert(Alert.AlertType.ERROR, "Account disabled", "Your account is inactive. Contact support.");
            return;
        }

        Session.setCurrentUser(user);
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/HomePage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open home page.");
        }
    }

    /** Fake admin user for root login (not stored in DB). */
    private static Personne createRootAdmin() {
        Personne admin = new Personne();
        admin.setId(0);
        admin.setNom("Admin");
        admin.setPrenom("Root");
        admin.setEmail("root");
        admin.setMotDePasse("root");
        admin.setRole("admin");
        admin.setStatutCompte("actif");
        return admin;
    }

    @FXML
    void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RegisterPage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Window controls (minimize, maximize, close)
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
