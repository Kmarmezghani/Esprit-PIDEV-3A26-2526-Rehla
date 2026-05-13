package Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;
import models.Personne;
import services.FaceRecognitionService;
import services.SecurityAuditService;
import util.Session;
import util.WebcamCapture;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Face Enrollment screen.
 * Allows users to enroll their face for face login.
 */
public class FaceEnrollmentController implements Initializable {

    @FXML private ImageView webcamView;
    @FXML private StackPane webcamContainer;
    @FXML private VBox noCameraBox;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Rectangle faceGuide;

    @FXML private Label userEmailLabel;
    @FXML private Label statusLabel;
    @FXML private Label messageLabel;
    @FXML private Label enrollmentStatusLabel;
    @FXML private HBox enrollmentStatus;

    @FXML private Button captureButton;
    @FXML private Button enrollButton;
    @FXML private Button retakeButton;
    @FXML private Button removeButton;
    @FXML private Button skipButton;

    private WebcamCapture webcamCapture;
    private FaceRecognitionService faceService;
    private SecurityAuditService auditService;

    private Personne currentUser;
    private BufferedImage capturedImage;
    private Image capturedFXImage;

    private static boolean comingFromRegistration;

    // ✅ NEW: callback to continue after register flow
    private Runnable onContinueAfterRegister;

    public static void setComingFromRegistration(boolean value) {
        comingFromRegistration = value;
    }

    /**
     * ✅ NEW: called by RegisterController when opening this page from registration.
     * This ensures currentUser is correct and allows redirect after enroll/skip.
     */
    public void initAfterRegister(Personne user, Runnable onContinue) {
        this.currentUser = user;
        this.onContinueAfterRegister = onContinue;

        // keep session consistent
        if (user != null) Session.setCurrentUser(user);

        // update label if FXML already injected
        if (userEmailLabel != null && user != null) {
            userEmailLabel.setText(user.getEmail());
        }

        // show skip button in registration flow
        if (skipButton != null) {
            skipButton.setVisible(true);
            skipButton.setText("Skip for now →");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        faceService = FaceRecognitionService.getInstance();
        auditService = SecurityAuditService.getInstance();

        // If initAfterRegister already set currentUser, keep it.
        if (currentUser == null) currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            showError("Please log in to enroll your face.");
            return;
        }

        userEmailLabel.setText(currentUser.getEmail());

        if (!faceService.isConfigured()) {
            showError("Face++ API not configured. Please add API keys to config.properties");
            captureButton.setDisable(true);
            return;
        }

        checkEnrollmentStatus();
        retakeButton.setVisible(false);

        // If not coming from registration, skip button stays hidden
        if (skipButton != null && !skipButton.isVisible()) {
            skipButton.setVisible(comingFromRegistration);
        }
        comingFromRegistration = false;

        initializeWebcamAsync();
    }

    private void initializeWebcamAsync() {
        messageLabel.setText("");
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
                        statusLabel.setText("Ready to capture");
                    } else {
                        showNoCamera();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::showNoCamera);
            }
        }).start();
    }

    private void checkEnrollmentStatus() {
        if (faceService.isUserEnrolled(currentUser.getEmail())) {
            enrollmentStatus.setVisible(true);
            enrollmentStatusLabel.setText("Face already enrolled - you can update it below");
            removeButton.setVisible(true);
        } else {
            enrollmentStatus.setVisible(false);
            removeButton.setVisible(false);
        }
    }

    private void showNoCamera() {
        noCameraBox.setVisible(true);
        webcamView.setVisible(false);
        faceGuide.setVisible(false);
        captureButton.setDisable(true);
        showError("Camera not available. Close other apps using the webcam, then retry.");
        statusLabel.setText("Camera not detected");
    }

    @FXML
    void handleCapture(ActionEvent event) {
        clearMessages();
        statusLabel.setText("Capturing...");

        if (webcamCapture == null) {
            showError("Camera not initialized.");
            return;
        }

        capturedImage = webcamCapture.capture();

        if (capturedImage == null) {
            showError("Failed to capture image. Please try again.");
            return;
        }

        // stop live feed while previewing capture
        webcamCapture.stop();

        capturedFXImage = SwingFXUtils.toFXImage(capturedImage, null);
        webcamView.setImage(capturedFXImage);

        statusLabel.setText("Face captured! Review and confirm enrollment.");
        faceGuide.setStroke(javafx.scene.paint.Color.web("#27ae60"));

        captureButton.setVisible(false);
        enrollButton.setVisible(true);
        retakeButton.setVisible(true);
    }

    @FXML
    void handleEnroll(ActionEvent event) {
        if (capturedImage == null) {
            showError("No face captured. Please capture your face first.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Enrolling face...");

        new Thread(() -> {
            FaceRecognitionService.FaceResult result = faceService.enrollFace(
                    currentUser.getEmail(),
                    capturedImage
            );

            Platform.runLater(() -> {
                setLoading(false);

                if (result.success) {
                    showSuccess(result.message);
                    statusLabel.setText("✓ Face enrolled successfully!");
                    faceGuide.setStroke(javafx.scene.paint.Color.web("#27ae60"));

                    auditService.logSecurityEvent(
                            currentUser.getId(),
                            "FACE_ENROLLED",
                            "Face enrolled for face login"
                    );

                    checkEnrollmentStatus();

                    enrollButton.setVisible(false);
                    retakeButton.setText("Update Face");
                    retakeButton.setVisible(true);

                    // ✅ IMPORTANT: if we came from registration -> auto continue to Home
                    if (onContinueAfterRegister != null) {
                        // stop resources before navigation
                        stopWebcam();
                        Platform.runLater(onContinueAfterRegister);
                        return;
                    }

                    // If not registration flow, keep page and show user can go back
                    if (skipButton != null && skipButton.isVisible()) {
                        skipButton.setText("Continue →");
                    }

                } else {
                    showError(result.message);
                    statusLabel.setText("Enrollment failed");
                    faceGuide.setStroke(javafx.scene.paint.Color.web("#e74c3c"));
                }
            });
        }).start();
    }

    @FXML
    void handleRetake(ActionEvent event) {
        clearMessages();
        capturedImage = null;
        capturedFXImage = null;

        captureButton.setVisible(true);
        enrollButton.setVisible(false);
        retakeButton.setVisible(false);

        faceGuide.setStroke(javafx.scene.paint.Color.web("#4facfe"));
        statusLabel.setText("Reconnecting camera...");

        stopWebcam();
        webcamCapture = null;

        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Platform.runLater(() -> {
                webcamCapture = new WebcamCapture();
                boolean started = webcamCapture.start(webcamView);
                if (started) {
                    noCameraBox.setVisible(false);
                    webcamView.setVisible(true);
                    faceGuide.setVisible(true);
                    statusLabel.setText("Ready to capture");
                } else {
                    showNoCamera();
                }
            });
        }).start();
    }

    @FXML
    void handleRemove(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Face Enrollment");
        confirm.setHeaderText("Remove your enrolled face?");
        confirm.setContentText("You will no longer be able to use face login until you enroll again.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            faceService.removeEnrollment(currentUser.getEmail());

            auditService.logSecurityEvent(
                    currentUser.getId(),
                    "FACE_REMOVED",
                    "Face enrollment removed"
            );

            showSuccess("Face enrollment removed successfully.");
            checkEnrollmentStatus();
            handleRetake(event);
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        stopWebcam();
        navigateTo("/Frontoffice/ProfilePage.fxml");
    }

    @FXML
    void handleSkip(ActionEvent event) {
        stopWebcam();

        // ✅ If registration flow -> use callback (best)
        if (onContinueAfterRegister != null) {
            Platform.runLater(onContinueAfterRegister);
            return;
        }

        navigateTo("/Frontoffice/HomePage.fxml");
    }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) (webcamView != null ? webcamView.getScene().getWindow() : captureButton.getScene().getWindow());
            if (stage.getScene() == null) util.NavigationUtil.switchScene(stage, root);
            else stage.getScene().setRoot(root);
            root.applyCss();
            root.layout();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        enrollButton.setDisable(loading);
        retakeButton.setDisable(loading);
        captureButton.setDisable(loading);
        if (loading) webcamView.setOpacity(0.5);
        else webcamView.setOpacity(1.0);
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void clearMessages() {
        messageLabel.setText("");
    }

    private void stopWebcam() {
        if (webcamCapture != null) {
            try { webcamCapture.stop(); } catch (Exception ignored) {}
        }
    }

    public void cleanup() {
        stopWebcam();
    }
}
