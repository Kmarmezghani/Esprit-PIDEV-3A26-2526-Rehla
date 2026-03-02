package Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.Personne;
import services.*;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the login page.
 * Supports traditional password login and passwordless OTP login.
 * Integrates security detection for suspicious logins.
 */
public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField searchField;
    @FXML private TextField searchField1;
    @FXML private Button loginButton;
    @FXML private Button otpLoginButton;
    @FXML private Button faceLoginButton;
    @FXML private Label errorLabel;
    @FXML private Label captchaLabel;
    @FXML private TextField captchaField;
    @FXML private HBox captchaBox;
    @FXML private Label attemptsLabel;
    @FXML private StackPane rootStack;
    @FXML private StackPane cardContainer;
    @FXML private StackPane cardRightPanel;
    @FXML private ImageView cardRightPhoto;

    private final PersonneService personneService = new PersonneService();
    private final OTPService otpService = OTPService.getInstance();
    private final EmailUserService emailService = EmailUserService.getInstance();
    private final GeoIPService geoIPService = GeoIPService.getInstance();
    private final RateLimitService rateLimitService = RateLimitService.getInstance();
    private final SecurityAuditService securityAuditService = SecurityAuditService.getInstance();
    private final FaceRecognitionService faceService = FaceRecognitionService.getInstance();
    //private final AccountInactivityService accountInactivityService = AccountInactivityService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (captchaBox != null) {
            captchaBox.setVisible(false);
            captchaBox.setManaged(false);
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (attemptsLabel != null) {
            attemptsLabel.setVisible(false);
        }
        // Periodically mark long-inactive accounts as INACTIF
        try {
            //accountInactivityService.runDeactivationJob();
        } catch (Exception ignored) {}
        loadBlurredBackground();
        applyEqualRoundedCorners();
        setupRightImageFill();
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

    private void setupRightImageFill() {
        if (cardRightPhoto != null) {
            cardRightPhoto.setPreserveRatio(false);
        }
        if (cardRightPanel != null && cardRightPhoto != null) {
            cardRightPhoto.fitWidthProperty().bind(cardRightPanel.widthProperty());
            cardRightPhoto.fitHeightProperty().bind(cardRightPanel.heightProperty());
        }
    }

    private void applyEqualRoundedCorners() {
        double radius = 30;
        if (cardContainer != null) {
            Rectangle clip = new Rectangle(900, 520);
            clip.setArcWidth(radius * 2);
            clip.setArcHeight(radius * 2);
            cardContainer.layoutBoundsProperty().addListener((o, oldVal, newVal) -> {
                clip.setWidth(Math.max(900, newVal.getWidth()));
                clip.setHeight(Math.max(520, newVal.getHeight()));
            });
            cardContainer.setClip(clip);
        }
    }

    /**
     * Gets the email from the appropriate field (supports both old and new FXML).
     */
    private String getEmail() {
        if (emailField != null && emailField.getText() != null) {
            return emailField.getText().trim();
        }
        if (searchField != null && searchField.getText() != null) {
            return searchField.getText().trim();
        }
        return "";
    }

    /**
     * Gets the password from the appropriate field.
     */
    private String getPassword() {
        if (passwordField != null && passwordField.getText() != null) {
            return passwordField.getText();
        }
        if (searchField1 != null && searchField1.getText() != null) {
            return searchField1.getText();
        }
        return "";
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = getEmail();
        String password = getPassword();

        hideError();

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        if (rateLimitService.isBlocked(email)) {
            long minutes = rateLimitService.getBlockMinutesRemaining(email);
            showError("Too many failed attempts. Try again in " + minutes + " minutes.");
            return;
        }

        if (rateLimitService.requiresCaptcha(email)) {
            if (!handleCaptchaVerification(email)) {
                return;
            }
        }

        if ("root".equalsIgnoreCase(email) && "root".equals(password)) {
            rateLimitService.clearAll(email);
            Session.setCurrentUser(createRootAdmin());
            navigateTo("/Backoffice/Dashboard.fxml", event);
            return;
        }

        Personne user = personneService.findByEmail(email);
        if (user == null) {
            handleFailedLogin(email, null, "No account found with this email.");
            return;
        }

        if (!password.equals(user.getMotDePasse())) {
            handleFailedLogin(email, user, "Incorrect password.");
            return;
        }

        if ("suspendu".equalsIgnoreCase(user.getStatutCompte())) {
            showError("Your account is suspended. Contact support.");
            return;
        }

        if ("inactif".equalsIgnoreCase(user.getStatutCompte())) {
            showError("Your account is inactive. Contact support.");
            return;
        }

        GeoIPService.LocationInfo location = geoIPService.getCurrentLocation();
        boolean isSuspicious = securityAuditService.isSuspiciousLogin(
                user.getId(), location.city, location.country
        );

        if (isSuspicious) {
            securityAuditService.logSecurityEvent(
                    user.getId(),
                    SecurityAuditService.EVENT_LOGIN_SUSPICIOUS,
                    "Suspicious login attempt from " + location.city + ", " + location.country
            );

            securityAuditService.logSecurityEvent(
                    user.getId(),
                    SecurityAuditService.EVENT_NEW_LOCATION,
                    "New location detected: " + location.city + ", " + location.country
            );

            emailService.sendSecurityAlert(
                    user.getEmail(),
                    user.getPrenom(),
                    "New Login Location Detected",
                    "A login attempt was made from " + location.city + ", " + location.country +
                            ". If this was you, please verify with the OTP code sent to your email."
            );

            initiateOTPVerification(user, true);
            navigateToOTPScreen(event);
            return;
        }

        rateLimitService.clearAll(email);
        securityAuditService.logSecurityEvent(
                user.getId(),
                SecurityAuditService.EVENT_LOGIN_SUCCESS,
                "Login successful from " + location.city + ", " + location.country
        );

        // mark latest successful login
        try {
            personneService.updateLastLogin(user.getId(), java.time.LocalDateTime.now());
        } catch (Exception ignored) {}

        Session.setCurrentUser(user);
        navigateTo("/Frontoffice/HomePage.fxml", event);
    }

    @FXML
    void handleOTPLogin(ActionEvent event) {
        String email = getEmail();
        hideError();

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        if (rateLimitService.isBlocked(email)) {
            long minutes = rateLimitService.getBlockMinutesRemaining(email);
            showError("Too many attempts. Try again in " + minutes + " minutes.");
            return;
        }

        if (rateLimitService.requiresCaptcha(email)) {
            if (!handleCaptchaVerification(email)) {
                return;
            }
        }

        Personne user = personneService.findByEmail(email);
        if (user == null) {
            showError("No account found with this email.");
            rateLimitService.recordAttempt(email, false);
            updateAttemptsDisplay(email);
            return;
        }

        if ("suspendu".equalsIgnoreCase(user.getStatutCompte())) {
            showError("Your account is suspended. Contact support.");
            return;
        }

        if ("inactif".equalsIgnoreCase(user.getStatutCompte())) {
            showError("Your account is inactive. Contact support.");
            return;
        }

        initiateOTPVerification(user, false);

        disableButton(otpLoginButton, "Sending...");

        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                enableButton(otpLoginButton, "Sign in with OTP");
                navigateToOTPScreen(event);
            });
        }).start();
    }

    @FXML
    void handleFaceLogin(ActionEvent event) {
        String email = getEmail();
        hideError();

        if (email.isEmpty()) {
            showError("Please enter your email address first.");
            return;
        }

        if (!faceService.isConfigured()) {
            showError("Face login is not configured. Please use password or OTP login.");
            return;
        }

        Personne user = personneService.findByEmail(email);
        if (user == null) {
            showError("No account found with this email.");
            return;
        }

        if (!faceService.isUserEnrolled(email)) {
            showError("No face enrolled for this account. Enroll your face from profile settings after logging in.");
            return;
        }

        if ("suspendu".equalsIgnoreCase(user.getStatutCompte())) {
            showError("Your account is suspended. Contact support.");
            return;
        }

        FaceLoginController.setPendingEmail(email);
        navigateTo("/Frontoffice/FaceLogin.fxml", event);
    }

    private void initiateOTPVerification(Personne user, boolean isSuspicious) {
        String otp = otpService.generateOTP(user.getEmail());

        emailService.sendOTPEmail(user.getEmail(), user.getPrenom(), otp);

        securityAuditService.logSecurityEvent(
                user.getId(),
                SecurityAuditService.EVENT_OTP_SENT,
                "OTP sent to " + user.getEmail()
        );

        VerifyOTPController.setPendingVerification(user.getEmail(), user, isSuspicious);
    }

    private void handleFailedLogin(String email, Personne user, String message) {
        rateLimitService.recordAttempt(email, false);

        if (user != null) {
            securityAuditService.logSecurityEvent(
                    user.getId(),
                    SecurityAuditService.EVENT_LOGIN_FAILED,
                    "Login failed: " + message
            );
        }

        int remaining = rateLimitService.getRemainingAttempts(email);
        if (remaining > 0) {
            showError(message + " " + remaining + " attempts remaining.");
        } else {
            long blockMinutes = rateLimitService.getBlockMinutesRemaining(email);
            showError("Account temporarily locked. Try again in " + blockMinutes + " minutes.");
        }

        updateAttemptsDisplay(email);

        if (rateLimitService.requiresCaptcha(email)) {
            showCaptcha(email);
        }
    }

    private boolean handleCaptchaVerification(String email) {
        if (captchaBox == null || !captchaBox.isVisible()) {
            showCaptcha(email);
            showError("Please solve the security question.");
            return false;
        }

        if (captchaField == null || captchaField.getText().isEmpty()) {
            showError("Please answer the security question.");
            return false;
        }

        if (!rateLimitService.verifyCaptcha(email, captchaField.getText())) {
            showError("Incorrect answer. Please try again.");
            showCaptcha(email);
            return false;
        }

        hideCaptcha();
        return true;
    }

    private void showCaptcha(String email) {
        if (captchaBox != null && captchaLabel != null) {
            String question = rateLimitService.generateCaptcha(email);
            captchaLabel.setText(question);
            captchaBox.setVisible(true);
            captchaBox.setManaged(true);
            if (captchaField != null) {
                captchaField.clear();
            }
        }
    }

    private void hideCaptcha() {
        if (captchaBox != null) {
            captchaBox.setVisible(false);
            captchaBox.setManaged(false);
        }
    }

    private void updateAttemptsDisplay(String email) {
        if (attemptsLabel != null) {
            int remaining = rateLimitService.getRemainingAttempts(email);
            if (remaining < 5) {
                attemptsLabel.setText(remaining + " attempts remaining");
                attemptsLabel.setVisible(true);
            }
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "Login", message);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    private void disableButton(Button button, String text) {
        if (button != null) {
            button.setDisable(true);
            button.setText(text);
        }
    }

    private void enableButton(Button button, String text) {
        if (button != null) {
            button.setDisable(false);
            button.setText(text);
        }
    }

    private void navigateToOTPScreen(ActionEvent event) {
        navigateTo("/Frontoffice/VerifyOTP.fxml", event);
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load page: " + fxmlPath);
        }
    }

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
        navigateTo("/Frontoffice/RegisterPage.fxml", event);
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