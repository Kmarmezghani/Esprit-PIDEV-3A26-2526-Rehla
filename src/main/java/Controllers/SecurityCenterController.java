package Controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Personne;
import org.json.JSONArray;
import org.json.JSONObject;
import services.PersonneService;
import services.SecurityAuditService;
import util.Session;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Security & Privacy Center.
 * Displays security score, activity timeline, and provides quick actions.
 */
public class SecurityCenterController implements Initializable {

    @FXML private Label scoreValueLabel;
    @FXML private Label scoreStatusLabel;
    @FXML private Label scoreDescLabel;
    @FXML private VBox scoreCard;
    @FXML private Label lastLocationLabel;
    @FXML private Label lastLoginTimeLabel;
    @FXML private Label totalEventsLabel;
    @FXML private TableView<SecurityAuditService.SecurityEvent> timelineTable;
    @FXML private TableColumn<SecurityAuditService.SecurityEvent, String> iconColumn;
    @FXML private TableColumn<SecurityAuditService.SecurityEvent, String> typeColumn;
    @FXML private TableColumn<SecurityAuditService.SecurityEvent, String> messageColumn;
    @FXML private TableColumn<SecurityAuditService.SecurityEvent, String> timestampColumn;
    @FXML private Button refreshButton;

    private final SecurityAuditService securityAuditService = SecurityAuditService.getInstance();
    private final PersonneService personneService = new PersonneService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private Personne currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = Session.getCurrentUser();

        if (currentUser == null || currentUser.getId() == 0) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Please log in to view your security center.");
            return;
        }

        setupTableColumns();
        loadSecurityData();
    }

    private void setupTableColumns() {
        iconColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIcon())
        );
        iconColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16;");

        typeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTypeLabel())
        );
        typeColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        messageColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().message)
        );
        messageColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        timestampColumn.setCellValueFactory(data -> {
            if (data.getValue().timestamp != null) {
                return new SimpleStringProperty(data.getValue().timestamp.format(dateFormatter));
            }
            return new SimpleStringProperty("");
        });
        timestampColumn.setStyle("-fx-alignment: CENTER;");
    }

    private void loadSecurityData() {
        if (currentUser == null) return;

        int score = securityAuditService.calculateSecurityScore(currentUser.getId());
        String status = securityAuditService.getSecurityStatus(score);
        String color = securityAuditService.getSecurityStatusColor(score);

        scoreValueLabel.setText(String.valueOf(score));
        scoreValueLabel.setStyle("-fx-text-fill: " + color + ";");
        scoreStatusLabel.setText(status);
        scoreStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        if (score >= 80) {
            scoreDescLabel.setText("Your account is well protected");
        } else if (score >= 50) {
            scoreDescLabel.setText("Some security improvements recommended");
        } else {
            scoreDescLabel.setText("Immediate action required to secure your account");
        }

        String lastLocation = securityAuditService.getLastLoginLocation(currentUser.getId());
        if (lastLocation != null && !lastLocation.isBlank()) {
            lastLocationLabel.setText(lastLocation);
        } else {
            lastLocationLabel.setText("No recent logins");
        }

        int totalEvents = securityAuditService.getTotalSecurityEvents(currentUser.getId());
        totalEventsLabel.setText(String.valueOf(totalEvents));

        loadTimeline();
    }

    private void loadTimeline() {
        List<SecurityAuditService.SecurityEvent> events =
            securityAuditService.getSecurityTimeline(currentUser.getId(), 50);

        ObservableList<SecurityAuditService.SecurityEvent> observableEvents =
            FXCollections.observableArrayList(events);

        timelineTable.setItems(observableEvents);

        if (events.isEmpty()) {
            timelineTable.setPlaceholder(new Label("No security events recorded yet"));
        }
    }

    @FXML
    void refreshTimeline(ActionEvent event) {
        refreshButton.setDisable(true);
        refreshButton.setText("Loading...");

        loadSecurityData();

        refreshButton.setDisable(false);
        refreshButton.setText("↻ Refresh");
    }

    @FXML
    void handleLogoutAll(MouseEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout All Sessions");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will log you out of all sessions including this one.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            securityAuditService.logSecurityEvent(
                currentUser.getId(),
                SecurityAuditService.EVENT_SESSION_LOGOUT,
                "All sessions logged out by user"
            );

            Session.clear();

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleResetPassword(MouseEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter your new password");

        ButtonType resetButtonType = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New password");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm password");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        content.getChildren().addAll(
            new Label("New Password:"), newPassword,
            new Label("Confirm Password:"), confirmPassword,
            errorLabel
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                return newPassword.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            if (password.length() < 6) {
                showAlert(Alert.AlertType.WARNING, "Invalid Password", "Password must be at least 6 characters.");
                return;
            }
            if (!password.equals(confirmPassword.getText())) {
                showAlert(Alert.AlertType.WARNING, "Password Mismatch", "Passwords do not match.");
                return;
            }

            currentUser.setMotDePasse(password);
            personneService.update(currentUser);

            securityAuditService.logSecurityEvent(
                currentUser.getId(),
                SecurityAuditService.EVENT_PASSWORD_RESET,
                "Password reset by user"
            );

            showAlert(Alert.AlertType.INFORMATION, "Success", "Your password has been updated.");
            loadSecurityData();
        });
    }

    @FXML
    void handleLockAccount(MouseEvent event) {
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Lock Account");
        confirm.setHeaderText("Are you sure you want to lock your account?");
        confirm.setContentText("Your account will be suspended. You will need to contact support to unlock it.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            currentUser.setStatutCompte("suspendu");
            personneService.update(currentUser);

            securityAuditService.logSecurityEvent(
                currentUser.getId(),
                SecurityAuditService.EVENT_ACCOUNT_LOCKED,
                "Account locked by user"
            );

            Session.clear();

            showAlert(Alert.AlertType.INFORMATION, "Account Locked",
                "Your account has been locked. Contact support to unlock.");

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleExportData(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export My Data");
        fileChooser.setInitialFileName("rehla_mydata_" + currentUser.getId() + ".json");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                JSONObject data = new JSONObject();

                JSONObject profile = new JSONObject();
                profile.put("id", currentUser.getId());
                profile.put("nom", currentUser.getNom());
                profile.put("prenom", currentUser.getPrenom());
                profile.put("email", currentUser.getEmail());
                profile.put("role", currentUser.getRole());
                profile.put("dateInscription", currentUser.getDateInscription() != null
                    ? currentUser.getDateInscription().toString() : "");
                profile.put("statutCompte", currentUser.getStatutCompte());
                data.put("profile", profile);

                JSONObject security = new JSONObject();
                security.put("securityScore", securityAuditService.calculateSecurityScore(currentUser.getId()));
                security.put("securityStatus", securityAuditService.getSecurityStatus(
                    securityAuditService.calculateSecurityScore(currentUser.getId())));
                security.put("totalEvents", securityAuditService.getTotalSecurityEvents(currentUser.getId()));
                data.put("security", security);

                List<SecurityAuditService.SecurityEvent> events =
                    securityAuditService.getSecurityTimeline(currentUser.getId(), 100);
                JSONArray eventsArray = new JSONArray();
                for (SecurityAuditService.SecurityEvent e : events) {
                    JSONObject eventObj = new JSONObject();
                    eventObj.put("type", e.type);
                    eventObj.put("message", e.message);
                    eventObj.put("timestamp", e.timestamp != null ? e.timestamp.toString() : "");
                    eventsArray.put(eventObj);
                }
                data.put("securityEvents", eventsArray);

                data.put("exportDate", java.time.LocalDateTime.now().toString());

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(data.toString(2));
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Complete",
                    "Your data has been exported to:\n" + file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                    "Could not export data: " + e.getMessage());
            }
        }
    }

    @FXML
    void goToHome(ActionEvent event) {
        navigateTo("/HomePage.fxml", event);
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
