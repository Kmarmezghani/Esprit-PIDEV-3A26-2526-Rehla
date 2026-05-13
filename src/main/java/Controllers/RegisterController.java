package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Personne;
import services.FaceRecognitionService;
import services.PersonneService;
import services.PreferenceService;
import services.RateLimitService;
import services.SecurityAuditService;
import util.Session;
import util.ValidationUtil;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Controller for the registration page.
 * Handles user registration with validation and rate limiting.
 */
public class RegisterController implements Initializable {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;
    @FXML private Label captchaLabel;
    @FXML private TextField captchaField;
    @FXML private Button registerButton;
    @FXML private StackPane rootStack;
    @FXML private StackPane cardContainer;
    @FXML private StackPane cardRightPanel;
    @FXML private ImageView cardRightPhoto;

    @FXML private TextField searchField;
    @FXML private TextField searchField1;
    @FXML private TextField searchField2;
    @FXML private TextField searchField21;
    @FXML private TextField searchField211;

    private final PersonneService personneService = new PersonneService();
    private final PreferenceService preferenceService = new PreferenceService();
    private final RateLimitService rateLimitService = RateLimitService.getInstance();
    private final SecurityAuditService securityAuditService = SecurityAuditService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (roleCombo != null) {
            roleCombo.getItems().setAll("Traveler", "Guide");
            roleCombo.getSelectionModel().selectFirst();
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        refreshCaptcha(null);
        loadBlurredBackground();
        applyEqualRoundedCorners();
        setupRightImageFill();
    }

    private void setupRightImageFill() {
        if (cardRightPhoto != null) {
            cardRightPhoto.setPreserveRatio(false);
        }
        if (cardRightPanel != null && cardRightPhoto != null) {
            cardRightPhoto.fitWidthProperty().bind(cardRightPanel.widthProperty());
            cardRightPhoto.fitHeightProperty().bind(cardRightPanel.heightProperty());
        }
    }

    private void loadBlurredBackground() {
        try {
            java.net.URL url = getClass().getResource("/Frontoffice/images/hero.jpg");
            Image img = url != null ? new Image(url.toExternalForm(), true) : null;
            if (rootStack != null && img != null) {
                ImageView bg = new ImageView(img);
                bg.setFitWidth(1200);
                bg.setFitHeight(750);
                bg.setPreserveRatio(false);
                bg.setEffect(new GaussianBlur(35));
                rootStack.getChildren().add(1, bg);
            }
            if (cardRightPhoto != null && img != null) {
                cardRightPhoto.setImage(img);
                cardRightPhoto.setEffect(null);
            }
        } catch (Exception ignored) {}
    }

    private void applyEqualRoundedCorners() {
        double radius = 30;
        if (cardContainer != null) {
            Rectangle clip = new Rectangle(900, 540);
            clip.setArcWidth(radius * 2);
            clip.setArcHeight(radius * 2);
            cardContainer.layoutBoundsProperty().addListener((o, oldVal, newVal) -> {
                clip.setWidth(Math.max(900, newVal.getWidth()));
                clip.setHeight(Math.max(540, newVal.getHeight()));
            });
            cardContainer.setClip(clip);
        }
    }

    @FXML
    void refreshCaptcha(ActionEvent event) {
        if (captchaLabel != null && rateLimitService != null) {
            captchaLabel.setText(rateLimitService.generateRegistrationCaptcha());
            if (captchaField != null) captchaField.clear();
        }
    }

    private String getNom() {
        if (nomField != null && nomField.getText() != null) return nomField.getText().trim();
        if (searchField != null && searchField.getText() != null) return searchField.getText().trim();
        return "";
    }

    private String getPrenom() {
        if (prenomField != null && prenomField.getText() != null) return prenomField.getText().trim();
        if (searchField1 != null && searchField1.getText() != null) return searchField1.getText().trim();
        return "";
    }

    private String getEmail() {
        if (emailField != null && emailField.getText() != null) return emailField.getText().trim();
        if (searchField2 != null && searchField2.getText() != null) return searchField2.getText().trim();
        return "";
    }

    private String getPassword() {
        if (passwordField != null && passwordField.getText() != null) return passwordField.getText();
        if (searchField21 != null && searchField21.getText() != null) return searchField21.getText();
        return "";
    }

    private String getConfirmPassword() {
        if (confirmPasswordField != null && confirmPasswordField.getText() != null) return confirmPasswordField.getText();
        if (searchField211 != null && searchField211.getText() != null) return searchField211.getText();
        return "";
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String nom = getNom();
        String prenom = getPrenom();
        String email = getEmail();
        String password = getPassword();
        String confirm = getConfirmPassword();

        hideError();

        if (prenom.isEmpty()) { showError("Please enter your first name."); return; }
        if (nom.isEmpty()) { showError("Please enter your last name."); return; }
        if (email.isEmpty()) { showError("Please enter your email."); return; }
        if (password == null || password.isEmpty()) { showError("Please enter a password."); return; }
        if (!password.equals(confirm)) { showError("Passwords do not match."); return; }

        if (!ValidationUtil.nameDoesNotStartWithNumber(nom)) { showError("Last name: " + ValidationUtil.nameErrorMessage()); return; }
        if (!ValidationUtil.nameDoesNotStartWithNumber(prenom)) { showError("First name: " + ValidationUtil.nameErrorMessage()); return; }
        if (!ValidationUtil.isValidEmail(email)) { showError(ValidationUtil.emailErrorMessage()); return; }
        if (!ValidationUtil.isStrongPassword(password)) { showError(ValidationUtil.strongPasswordErrorMessage()); return; }

        if (!rateLimitService.verifyRegistrationCaptcha(captchaField != null ? captchaField.getText() : "")) {
            showError("Please answer the security question correctly.");
            refreshCaptcha(null);
            return;
        }

        if (rateLimitService.isBlocked(email)) {
            long minutes = rateLimitService.getBlockMinutesRemaining(email);
            showError("Too many attempts. Try again in " + minutes + " minutes.");
            return;
        }

        if (personneService.findByEmail(email) != null) {
            showError("An account with this email already exists.");
            rateLimitService.recordAttempt(email, false);
            return;
        }

        String role = "user";
        if (roleCombo != null && roleCombo.getSelectionModel().getSelectedItem() != null) {
            String selected = roleCombo.getSelectionModel().getSelectedItem().trim();
            if ("Guide".equalsIgnoreCase(selected)) role = "guide";
        }

        Personne p = new Personne();
        p.setNom(nom);
        p.setPrenom(prenom);
        p.setEmail(email);
        p.setMotDePasse(password);
        p.setDateInscription(LocalDateTime.now());
        p.setRole(role);
        p.setStatutCompte("actif");
        p.setHeureNotif(java.time.LocalTime.MIDNIGHT);
        p.setNotifSmsActive(true);

        try {
            personneService.add(p);

            // IMPORTANT: si add() ne remplit pas p.id, récupère l'user depuis DB
            if (p.getId() == 0) {
                Personne fromDb = personneService.findByEmail(email);
                if (fromDb != null) p = fromDb;
            }

        } catch (RuntimeException e) {
            showError("Could not create account. Please try again.");
            e.printStackTrace();
            return;
        }

        rateLimitService.clearAll(email);
        rateLimitService.clearRegistrationCaptcha();

        securityAuditService.logSecurityEvent(
                p.getId(),
                SecurityAuditService.EVENT_LOGIN_SUCCESS,
                "Account created and logged in"
        );

        Session.setCurrentUser(p);

        // ✅ NEW: open FaceEnrollment with controller injection + callback
        if (FaceRecognitionService.getInstance().isConfigured()) {
            FaceEnrollmentController.setComingFromRegistration(true);
            goToFaceEnrollment(event, p);
        } else {
            navigateTo("/Frontoffice/HomePage.fxml", event);
        }
    }

    // ✅ NEW METHOD: controller injection + callback
    private void goToFaceEnrollment(ActionEvent event, Personne user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/FaceEnrollment.fxml"));
            Parent root = loader.load();

            FaceEnrollmentController ctrl = loader.getController();
            ctrl.initAfterRegister(user, () -> {
                // quand enrollment terminé/skip -> Home
                Session.setCurrentUser(user);
                navigateToNoEvent("/Frontoffice/HomePage.fxml", root);
            });

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load face enrollment page.");
        }
    }

    private void navigateToNoEvent(String fxmlPath, Parent anyNodeOnScene) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) anyNodeOnScene.getScene().getWindow();
            stage.getScene().setRoot(root);
            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "Registration", message);
        }
    }

    private void hideError() {
        if (errorLabel != null) errorLabel.setVisible(false);
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            util.NavigationUtil.switchScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load page.");
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        navigateTo("/Frontoffice/loginPage.fxml", event);
    }

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
