package Controllers;

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

    public static void setComingFromRegistration(boolean value) {
        comingFromRegistration = value;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        faceService = FaceRecognitionService.getInstance();
        auditService = SecurityAuditService.getInstance();
        currentUser = Session.getCurrentUser();

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
        if (skipButton != null) {
            skipButton.setVisible(comingFromRegistration);
            comingFromRegistration = false;
        }
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
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        noCameraBox.setVisible(false);
                        webcamView.setVisible(true);
                        faceGuide.setVisible(true);
                    } else {
                        showNoCamera();
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showNoCamera());
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
        showError("Camera not available. Close other apps using the webcam, then retry.");
    }

    @FXML
    void handleCapture(ActionEvent event) {
        clearMessages();
        statusLabel.setText("Capturing...");

        capturedImage = webcamCapture.capture();

        if (capturedImage == null) {
            showError("Failed to capture image. Please try again.");
            return;
        }

        if (webcamCapture != null) {
            webcamCapture.stop();
        }

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

                    if (skipButton != null && skipButton.isVisible()) {
                        skipButton.setText("Continue to Home →");
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

        if (webcamCapture != null) {
            webcamCapture.stop();
            webcamCapture = null;
        }
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                webcamCapture = new WebcamCapture();
                boolean started = webcamCapture.start(webcamView);
                if (started) {
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
        comingFromRegistration = false;
        navigateTo("/Frontoffice/HomePage.fxml");
    }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) (webcamView != null ? webcamView.getScene().getWindow() : captureButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/HomePage.fxml"));
                Stage stage = (Stage) captureButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        enrollButton.setDisable(loading);
        retakeButton.setDisable(loading);
        if (loading) {
            webcamView.setOpacity(0.5);
        } else {
            webcamView.setOpacity(1.0);
        }
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
            webcamCapture.stop();
        }
    }

    public void cleanup() {
        stopWebcam();
    }
}