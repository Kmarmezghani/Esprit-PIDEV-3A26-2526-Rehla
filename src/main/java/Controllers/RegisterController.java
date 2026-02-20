package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Personne;
import services.PersonneService;
import util.Session;
import util.ValidationUtil;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private TextField searchField;    // nom
    @FXML private TextField searchField1;   // prenom
    @FXML private TextField searchField2;   // email
    @FXML private TextField searchField21;  // password
    @FXML private TextField searchField211; // confirm password
    @FXML private ComboBox<String> roleCombo;

    private final PersonneService personneService = new PersonneService();
    private final PreferenceService preferenceService = new PreferenceService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (roleCombo != null) {
            roleCombo.getItems().setAll("User", "Guide");
            roleCombo.getSelectionModel().selectFirst(); // default "User"
        }
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String nom = getText(searchField);
        String prenom = getText(searchField1);
        String email = getText(searchField2);
        String password = searchField21.getText();
        String confirm = searchField211.getText();

        if (nom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Please enter your last name (Nom).");
            return;
        }
        if (prenom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Please enter your first name (Prénom).");
            return;
        }
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Please enter your email.");
            return;
        }
        if (password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Please enter a password.");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Passwords do not match.");
            return;
        }

        if (!ValidationUtil.nameDoesNotStartWithNumber(nom)) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Nom: " + ValidationUtil.nameErrorMessage());
            return;
        }
        if (!ValidationUtil.nameDoesNotStartWithNumber(prenom)) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Prénom: " + ValidationUtil.nameErrorMessage());
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Registration", ValidationUtil.emailErrorMessage());
            return;
        }
        if (!ValidationUtil.isStrongPassword(password)) {
            showAlert(Alert.AlertType.WARNING, "Registration", ValidationUtil.strongPasswordErrorMessage());
            return;
        }

        if (personneService.findByEmail(email) != null) {
            showAlert(Alert.AlertType.ERROR, "Registration", "An account with this email already exists.");
            return;
        }

        String role = "user";
        if (roleCombo != null && roleCombo.getSelectionModel().getSelectedItem() != null) {
            String selected = roleCombo.getSelectionModel().getSelectedItem().trim();
            if ("Guide".equalsIgnoreCase(selected)) role = "guide";
        }

        Personne p = new Personne(nom, prenom, email, password,
                LocalDateTime.now(), role, "actif");
        try {
            personneService.add(p);
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not create account. Check console for details.\n\n" + e.getMessage());
            return;
        }

        Session.setCurrentUser(p);
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/HomePage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Account created but could not open home page.");
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
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

    private String getText(TextField f) {
        return f != null && f.getText() != null ? f.getText().trim() : "";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
