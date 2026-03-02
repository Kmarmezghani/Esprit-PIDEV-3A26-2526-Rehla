package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Personne;
import services.*;
import util.Session;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for OTP verification screen.
 * Handles 6-digit code input, countdown timer, and verification.
 */
public class VerifyOTPController implements Initializable {

    @FXML private TextField digit1;
    @FXML private TextField digit2;
    @FXML private TextField digit3;
    @FXML private TextField digit4;
    @FXML private TextField digit5;
    @FXML private TextField digit6;

    @FXML private Label timerLabel;
    @FXML private Label emailLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label errorLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;

    private TextField[] digitFields;
    private Timeline countdownTimer;

    private static String pendingEmail;
    private static Personne pendingUser;
    private static boolean isSuspiciousLogin;

    private final OTPService otpService = OTPService.getInstance();
    private final EmailUserService emailService = EmailUserService.getInstance();
    private final SecurityAuditService securityAuditService = SecurityAuditService.getInstance();

    /**
     * Sets the user data before navigating to this screen.
     */
    public static void setPendingVerification(String email, Personne user, boolean suspicious) {
        pendingEmail = email;
        pendingUser = user;
        isSuspiciousLogin = suspicious;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        digitFields = new TextField[]{digit1, digit2, digit3, digit4, digit5, digit6};

        if (pendingEmail != null) {
            emailLabel.setText(maskEmail(pendingEmail));
        }

        if (isSuspiciousLogin) {
            subtitleLabel.setText("New location detected. Please verify your identity.");
        }

        setupDigitFields();
        startCountdown();

        Platform.runLater(() -> digit1.requestFocus());
    }

    private void setupDigitFields() {
        for (int i = 0; i < digitFields.length; i++) {
            final int index = i;
            TextField field = digitFields[i];

            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > 1) {
                    field.setText(newVal.substring(0, 1));
                    return;
                }

                if (!newVal.matches("\\d*")) {
                    field.setText(oldVal);
                    return;
                }

                if (newVal.length() == 1 && index < digitFields.length - 1) {
                    digitFields[index + 1].requestFocus();
                }

                if (isCodeComplete()) {
                    verifyButton.requestFocus();
                }
            });

            field.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case BACK_SPACE:
                        if (field.getText().isEmpty() && index > 0) {
                            digitFields[index - 1].requestFocus();
                            digitFields[index - 1].clear();
                        }
                        break;
                    case LEFT:
                        if (index > 0) {
                            digitFields[index - 1].requestFocus();
                        }
                        break;
                    case RIGHT:
                        if (index < digitFields.length - 1) {
                            digitFields[index + 1].requestFocus();
                        }
                        break;
                    case ENTER:
                        if (isCodeComplete()) {
                            handleVerify(null);
                        }
                        break;
                    default:
                        break;
                }
            });
        }
    }

    private void startCountdown() {
        updateTimerDisplay();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = otpService.getRemainingSeconds(pendingEmail);
            if (remaining <= 0) {
                timerLabel.setText("Expired");
                timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                verifyButton.setDisable(true);
                countdownTimer.stop();
            } else {
                int minutes = (int) (remaining / 60);
                int seconds = (int) (remaining % 60);
                timerLabel.setText(String.format("%d:%02d", minutes, seconds));

                if (remaining <= 60) {
                    timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateTimerDisplay() {
        long remaining = otpService.getRemainingSeconds(pendingEmail);
        int minutes = (int) (remaining / 60);
        int seconds = (int) (remaining % 60);
        timerLabel.setText(String.format("%d:%02d", minutes, seconds));
    }

    @FXML
    void handleVerify(ActionEvent event) {
        String code = getEnteredCode();

        if (code.length() != 6) {
            showError("Please enter all 6 digits");
            return;
        }

        boolean verified = otpService.verifyOTP(pendingEmail, code);

        if (verified) {
            if (countdownTimer != null) {
                countdownTimer.stop();
            }

            securityAuditService.logSecurityEvent(
                    pendingUser.getId(),
                    SecurityAuditService.EVENT_OTP_VERIFIED,
                    "OTP verified successfully"
            );

            GeoIPService.LocationInfo location = GeoIPService.getInstance().getCurrentLocation();
            securityAuditService.logSecurityEvent(
                    pendingUser.getId(),
                    SecurityAuditService.EVENT_LOGIN_SUCCESS,
                    "Login successful from " + location.city + ", " + location.country
            );

            Session.setCurrentUser(pendingUser);

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/HomePage.fxml"));
                Stage stage = (Stage) verifyButton.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Could not open home page.");
            }
        } else {
            int remaining = otpService.getRemainingAttempts(pendingEmail);
            if (remaining > 0) {
                showError("Invalid code. " + remaining + " attempts remaining.");
                securityAuditService.logSecurityEvent(
                        pendingUser.getId(),
                        SecurityAuditService.EVENT_OTP_FAILED,
                        "OTP verification failed"
                );
            } else {
                showError("Too many failed attempts. Please request a new code.");
                securityAuditService.logSecurityEvent(
                        pendingUser.getId(),
                        SecurityAuditService.EVENT_OTP_FAILED,
                        "OTP verification failed - max attempts exceeded"
                );
            }
            clearCode();
        }
    }

    @FXML
    void handleResend(ActionEvent event) {
        resendButton.setDisable(true);
        resendButton.setText("Sending...");

        new Thread(() -> {
            String newOtp = otpService.generateOTP(pendingEmail);
            String userName = pendingUser != null ? pendingUser.getPrenom() : "User";
            boolean sent = emailService.sendOTPEmail(pendingEmail, userName, newOtp);

            Platform.runLater(() -> {
                if (sent) {
                    securityAuditService.logSecurityEvent(
                            pendingUser.getId(),
                            SecurityAuditService.EVENT_OTP_SENT,
                            "OTP resent to " + pendingEmail
                    );

                    showSuccess("New code sent to your email");
                    verifyButton.setDisable(false);
                    startCountdown();
                } else {
                    showError("Failed to send code. Please try again.");
                }
                resendButton.setDisable(false);
                resendButton.setText("Resend");
                clearCode();
            });
        }).start();
    }

    @FXML
    void goToLogin(ActionEvent event) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        otpService.invalidateOTP(pendingEmail);

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEnteredCode() {
        StringBuilder code = new StringBuilder();
        for (TextField field : digitFields) {
            code.append(field.getText());
        }
        return code.toString();
    }

    private boolean isCodeComplete() {
        for (TextField field : digitFields) {
            if (field.getText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void clearCode() {
        for (TextField field : digitFields) {
            field.clear();
        }
        digit1.requestFocus();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #27ae60;");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    @FXML
    void closewindow(ActionEvent event) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
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