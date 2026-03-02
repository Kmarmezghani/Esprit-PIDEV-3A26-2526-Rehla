package Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.Personne;
import services.FaceRecognitionService;
import services.PersonneService;
import services.SecurityAuditService;
import util.Session;
import util.WebcamCapture;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Face Login screen.
 * Handles webcam capture and face verification.
 */
public class FaceLoginController implements Initializable {

    @FXML private ImageView webcamView;
    @FXML private StackPane webcamContainer;
    @FXML private VBox noCameraBox;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Rectangle faceGuide;
    
    @FXML private Label instructionLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private Label confidenceLabel;
    @FXML private HBox confidenceBox;
    
    @FXML private Button captureButton;
    @FXML private Button retryButton;

    private WebcamCapture webcamCapture;
    private FaceRecognitionService faceService;
    private PersonneService personneService;
    private SecurityAuditService auditService;
    
    private String pendingEmail;
    private Personne pendingUser;

    public static void setPendingEmail(String email) {
        pendingEmailStatic = email;
    }
    
    private static String pendingEmailStatic;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        faceService = FaceRecognitionService.getInstance();
        personneService = new PersonneService();
        auditService = SecurityAuditService.getInstance();
        
        pendingEmail = pendingEmailStatic;
        pendingEmailStatic = null;

        if (!faceService.isConfigured()) {
            showError("Face++ API not configured. Please add API keys to config.properties");
            captureButton.setDisable(true);
        }

        retryButton.setVisible(false);
        initializeWebcamAsync();
    }

    private void initializeWebcamAsync() {
        statusLabel.setText("Detecting camera...");
        errorLabel.setText("");
        new Thread(() -> {
            try {
                Thread.sleep(300);
                boolean available = WebcamCapture.isWebcamAvailable();
                boolean started = false;
                if (available) {
                    webcamCapture = new WebcamCapture();
                    started = webcamCapture.start(webcamView);
                }
                final boolean ok = started;
                Platform.runLater(() -> {
                    if (ok) {
                        noCameraBox.setVisible(false);
                        webcamView.setVisible(true);
                        faceGuide.setVisible(true);
                        statusLabel.setText("");
                        retryButton.setVisible(false);
                    } else {
                        showNoCamera();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showNoCamera());
            }
        }).start();
    }

    private void initializeWebcam() {
        if (!WebcamCapture.isWebcamAvailable()) {
            showNoCamera();
            return;
        }

        webcamCapture = new WebcamCapture();
        boolean started = webcamCapture.start(webcamView);

        if (!started) {
            showNoCamera();
        } else {
            noCameraBox.setVisible(false);
            webcamView.setVisible(true);
            faceGuide.setVisible(true);
        }
    }

    private void showNoCamera() {
        noCameraBox.setVisible(true);
        webcamView.setVisible(false);
        faceGuide.setVisible(false);
        captureButton.setDisable(true);
        retryButton.setVisible(true);
        statusLabel.setText("");
        showError("Camera not available. Close other apps using the webcam, then click Retry.");
    }

    @FXML
    void handleCapture(ActionEvent event) {
        clearMessages();

        if (pendingEmail == null || pendingEmail.isBlank()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Face Login");
            dialog.setHeaderText("Enter your email address");
            dialog.setContentText("Email:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isBlank()) {
                pendingEmail = result.get().trim();
            } else {
                showError("Please enter your email to continue.");
                return;
            }
        }

        pendingUser = personneService.getByEmail(pendingEmail);
        if (pendingUser == null) {
            showError("No account found with this email.");
            return;
        }

        if (!faceService.isUserEnrolled(pendingEmail)) {
            showError("No face enrolled for this account. Please enroll your face first from your profile settings.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Capturing face...");

        new Thread(() -> {
            try {
                Thread.sleep(500);
                
                BufferedImage faceImage = webcamCapture.capture();
                
                if (faceImage == null) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Failed to capture image. Please try again.");
                    });
                    return;
                }

                Platform.runLater(() -> statusLabel.setText("Verifying face..."));

                FaceRecognitionService.FaceResult result = faceService.verifyFace(pendingEmail, faceImage);

                Platform.runLater(() -> {
                    setLoading(false);
                    
                    if (result.success) {
                        handleSuccessfulLogin();
                    } else {
                        handleFailedVerification(result);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Error during verification: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleSuccessfulLogin() {
        faceGuide.setStroke(javafx.scene.paint.Color.web("#27ae60"));
        statusLabel.setText("✓ Face verified successfully!");
        statusLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        auditService.logSecurityEvent(
            pendingUser.getId(),
            SecurityAuditService.EVENT_LOGIN_SUCCESS,
            "Face login successful"
        );

        Session.setCurrentUser(pendingUser);

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(() -> navigateToHome());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void handleFailedVerification(FaceRecognitionService.FaceResult result) {
        faceGuide.setStroke(javafx.scene.paint.Color.web("#e74c3c"));
        showError(result.message);
        
        if (result.confidence > 0) {
            confidenceBox.setVisible(true);
            confidenceLabel.setText(String.format("%.1f%%", result.confidence));
            
            if (result.confidence >= 70) {
                confidenceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
            } else {
                confidenceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }
        }

        retryButton.setVisible(true);

        if (pendingUser != null) {
            auditService.logSecurityEvent(
                pendingUser.getId(),
                SecurityAuditService.EVENT_LOGIN_FAILED,
                "Face verification failed (confidence: " + String.format("%.1f", result.confidence) + "%)"
            );
        }
    }

    @FXML
    void handleRetry(ActionEvent event) {
        stopWebcam();
        webcamCapture = null;
        clearMessages();
        retryButton.setVisible(false);
        confidenceBox.setVisible(false);
        faceGuide.setStroke(javafx.scene.paint.Color.web("#4facfe"));
        instructionLabel.setText("Position your face in the frame");
        statusLabel.setText("Reconnecting camera...");
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(this::initializeWebcamAsync);
        }).start();
    }

    @FXML
    void goToLogin(ActionEvent event) {
        stopWebcam();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/loginPage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToHome() {
        stopWebcam();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Stage stage = (Stage) webcamView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error navigating to home page.");
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        captureButton.setDisable(loading);
        if (loading) {
            webcamView.setOpacity(0.5);
        } else {
            webcamView.setOpacity(1.0);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void clearMessages() {
        errorLabel.setText("");
        statusLabel.setText("");
        statusLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666666; -fx-font-weight: bold;");
    }

    private void stopWebcam() {
        if (webcamCapture != null) {
            webcamCapture.stop();
        }
    }

    public void cleanup() {
        stopWebcam();
    }
}
