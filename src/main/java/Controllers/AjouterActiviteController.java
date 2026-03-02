package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;
import services.AiDescriptionService;
import services.AttractionService;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class AjouterActiviteController {

    private final ActiviteService activiteService = new ActiviteService();
    private final AiDescriptionService aiDescriptionService = new AiDescriptionService();
    private final AttractionService attractionService = new AttractionService();

    @FXML private TextArea TFdescriptionactivite;
    @FXML private TextField TFnameactivite;
    @FXML private TextField TFtypeactivite;

    @FXML private Spinner<Double> pricespinneractivite;

    @FXML private DatePicker DPdateDebut;
    @FXML private DatePicker DPdateFin;

    @FXML private Spinner<Integer> SPheureDebut;
    @FXML private Spinner<Integer> SPminuteDebut;
    @FXML private Spinner<Integer> SPheureFin;
    @FXML private Spinner<Integer> SPminuteFin;

    @FXML private ComboBox<String> CBstatus;
    @FXML private ComboBox<String> CBdestination;

    @FXML private Label LBLmaxPlaces;
    @FXML private Spinner<Integer> SPmaxPlaces;

    @FXML private Label LBLimageName;
    @FXML private ImageView imagePreview;

    @FXML private Button BTNaiDesc;

    @FXML private Label LBLformTitle;
    @FXML private Button BTNprimary;

    private Map<String, Integer> destinationMap;

    private Activite activiteToEdit = null;
    private boolean editMode = false;

    private Integer fixedGuideId = null;
    private boolean adminMode = false;

    private File selectedImageFile = null;
    private String selectedImagePath = null;

    private LocalDateTime originalStart = null;
    private LocalDateTime originalEnd = null;

    public void setGuideId(int guideId) {
        this.fixedGuideId = guideId;
        refreshRoleVisibility();
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        refreshRoleVisibility();
    }

    @FXML
    public void initialize() {

        // =========================
        // SPINNERS / PICKERS SETUP
        // =========================
        pricespinneractivite.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100000.0, 1.0, 5.0)
        );
        pricespinneractivite.setEditable(true);

        SPheureDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        SPminuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
        SPheureFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10, 1));
        SPminuteFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));

        SPheureDebut.setEditable(true);
        SPminuteDebut.setEditable(true);
        SPheureFin.setEditable(true);
        SPminuteFin.setEditable(true);

        CBstatus.setItems(FXCollections.observableArrayList("DISPONIBLE", "INDISPONIBLE"));
        CBstatus.getSelectionModel().selectFirst();

        destinationMap = activiteService.getDestinationsMap();
        CBdestination.setItems(FXCollections.observableArrayList(destinationMap.keySet()));

        if (SPmaxPlaces != null) {
            SPmaxPlaces.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 10, 1));
            SPmaxPlaces.setEditable(true);
        }

        if (LBLimageName != null) LBLimageName.setText("No file selected");

        // =========================
        // DESCRIPTION TEXTAREA FIX
        // =========================
        if (TFdescriptionactivite != null) {
            TFdescriptionactivite.setWrapText(true);

            Tooltip tip = new Tooltip();
            tip.textProperty().bind(TFdescriptionactivite.textProperty());
            TFdescriptionactivite.setTooltip(tip);

            enableAutoGrowDescription();
        }

        setCreateModeUI();
        refreshRoleVisibility();
    }

    private void enableAutoGrowDescription() {
        TFdescriptionactivite.textProperty().addListener((obs, oldVal, newVal) -> {
            int lines = 1;
            if (newVal != null && !newVal.isBlank()) {
                lines = newVal.split("\n").length;
            }
            int approxExtra = (newVal == null) ? 0 : Math.max(0, newVal.length() / 70);
            int target = Math.min(10, Math.max(4, lines + approxExtra));
            TFdescriptionactivite.setPrefRowCount(target);
        });
    }

    private void scrollToEndDescription() {
        String t = TFdescriptionactivite.getText();
        if (t == null) return;
        TFdescriptionactivite.positionCaret(t.length());
        TFdescriptionactivite.requestFocus();
    }

    private void setCreateModeUI() {
        if (LBLformTitle != null) LBLformTitle.setText("Create activity");
        if (BTNprimary != null) BTNprimary.setText("Save");
    }

    private void setUpdateModeUI() {
        if (LBLformTitle != null) LBLformTitle.setText("Update activity");
        if (BTNprimary != null) BTNprimary.setText("Update");
    }

    private boolean isGuideCreating() {
        return fixedGuideId != null && !adminMode;
    }

    private void refreshRoleVisibility() {
        boolean guideCreating = isGuideCreating();

        if (SPmaxPlaces != null) {
            SPmaxPlaces.setVisible(guideCreating);
            SPmaxPlaces.setManaged(guideCreating);
        }
        if (LBLmaxPlaces != null) {
            LBLmaxPlaces.setVisible(guideCreating);
            LBLmaxPlaces.setManaged(guideCreating);
        }

        if (BTNaiDesc != null) {
            boolean showAi = guideCreating && !editMode; // AI only for guide + only in create
            BTNaiDesc.setVisible(showAi);
            BTNaiDesc.setManaged(showAi);
        }
    }

    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = getStageSafe();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            if (LBLimageName != null) LBLimageName.setText(selectedImageFile.getName());
            if (imagePreview != null) imagePreview.setImage(new Image(selectedImageFile.toURI().toString(), true));
        }
    }

    private String copyImageToUploads(File imageFile) throws IOException {
        String folder = System.getProperty("user.home") + "/myapp/uploads/activities/";
        Files.createDirectories(Paths.get(folder));

        String name = imageFile.getName();
        int dot = name.lastIndexOf('.');
        String extension = (dot >= 0) ? name.substring(dot) : ".jpg";

        String fileName = "activity_" + System.currentTimeMillis() + extension;
        Path destination = Paths.get(folder + fileName);

        Files.copy(imageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().toString();
    }

    @FXML
    public void generateDescriptionAi(ActionEvent event) {

        if (!isGuideCreating()) return;

        String name = safe(TFnameactivite.getText());
        String type = safe(TFtypeactivite.getText());
        String dest = (CBdestination != null) ? CBdestination.getValue() : null;

        if (name.isBlank() || type.isBlank() || dest == null || dest.isBlank()) {
            showWarn("Missing info", "Please fill Name, Type, and Destination first.");
            return;
        }

        String duration = buildDurationText();

        TFdescriptionactivite.setDisable(true);
        if (BTNaiDesc != null) BTNaiDesc.setDisable(true);

        TFdescriptionactivite.setText("Generating...");

        new Thread(() -> {
            try {
                int destinationId = (dest != null && destinationMap != null && destinationMap.containsKey(dest))
                        ? destinationMap.get(dest)
                        : 0;

                // ✅ Option 2 : on prend la liste des attractions de la ville/destination
                var list = attractionService.getAttractionsByVille(destinationId, 8);

                List<String> attractionLines = list.stream()
                        .map(a -> "Name: " + a.getNom()
                                + " | Type: " + a.getType()
                                + " | Hours: " + a.getHeureOuverture()
                                + " | Note: " + a.getDescription())
                        .toList();

                String text = aiDescriptionService.generate(name, type, dest, duration, attractionLines);

                Platform.runLater(() -> {
                    TFdescriptionactivite.setText(text == null ? "" : text.trim());
                    TFdescriptionactivite.setDisable(false);
                    if (BTNaiDesc != null) BTNaiDesc.setDisable(false);
                    scrollToEndDescription();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    TFdescriptionactivite.setText("");
                    TFdescriptionactivite.setDisable(false);
                    if (BTNaiDesc != null) BTNaiDesc.setDisable(false);

                    String msg = ex.getMessage() == null ? "Erreur inconnue" : ex.getMessage();
                    showWarn("AI error", msg);
                });
            }
        }).start();
    }

    private String buildDurationText() {
        LocalDate sd = DPdateDebut != null ? DPdateDebut.getValue() : null;
        LocalDate ed = DPdateFin != null ? DPdateFin.getValue() : null;

        if (sd == null || ed == null) return "Not specified";

        LocalTime st = LocalTime.of(val(SPheureDebut), val(SPminuteDebut));
        LocalTime et = LocalTime.of(val(SPheureFin), val(SPminuteFin));

        LocalDateTime start = LocalDateTime.of(sd, st);
        LocalDateTime end = LocalDateTime.of(ed, et);

        if (!end.isAfter(start)) return "Not specified";

        long mins = ChronoUnit.MINUTES.between(start, end);
        long days = mins / (60 * 24);
        long hours = (mins % (60 * 24)) / 60;
        long remMins = mins % 60;

        if (days >= 2) return days + " days";
        if (days == 1) return (hours > 0) ? "1 day " + hours + " hours" : "1 day";
        if (hours >= 1) return (remMins > 0) ? hours + " hours " + remMins + " min" : hours + " hours";
        return mins + " minutes";
    }

    @FXML
    void ajouterActivite(ActionEvent event) {

        String nom = safe(TFnameactivite.getText());
        String description = safe(TFdescriptionactivite.getText());
        String type = safe(TFtypeactivite.getText());

        Double price = pricespinneractivite.getValue();
        String status = (CBstatus != null) ? CBstatus.getValue() : null;

        LocalDateTime dateDebut = buildDateTime(DPdateDebut, SPheureDebut, SPminuteDebut);
        LocalDateTime dateFin = buildDateTime(DPdateFin, SPheureFin, SPminuteFin);

        String destNom = (CBdestination != null) ? CBdestination.getValue() : null;
        int destinationId = (destNom != null && destinationMap != null && destinationMap.containsKey(destNom))
                ? destinationMap.get(destNom)
                : 0;

        if (nom.isBlank()) { showWarn("Missing name", "Please enter the activity name."); return; }
        if (description.isBlank()) { showWarn("Missing description", "Please enter a description."); return; }
        if (type.isBlank()) { showWarn("Missing type", "Please enter the activity type."); return; }
        if (price == null || price <= 0) { showWarn("Invalid price", "Price must be greater than 0."); return; }

        if (destinationId == 0) { showWarn("Missing destination", "Please select a destination."); return; }
        if (dateDebut == null) { showWarn("Missing start date", "Please select a start date."); return; }
        if (dateFin == null) { showWarn("Missing end date", "Please select an end date."); return; }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (dateDebut.toLocalDate().isBefore(today)) { showWarn("Invalid start date", "Start date cannot be before today."); return; }
        if (dateDebut.toLocalDate().isEqual(today) && dateDebut.isBefore(now)) { showWarn("Invalid start time", "Start time cannot be before now."); return; }
        if (!dateFin.isAfter(dateDebut)) { showWarn("Invalid dates", "End date must be after start date."); return; }

        Integer guideIdToUse = null;

        if (editMode && activiteToEdit != null) {
            int existing = activiteToEdit.getGuideId();
            guideIdToUse = (existing == 0 ? null : existing);
        } else if (fixedGuideId != null) {
            guideIdToUse = fixedGuideId;
        } else if (adminMode) {
            guideIdToUse = null;
        }

        Integer maxPlacesToUse = null;
        boolean guideActivity = (guideIdToUse != null);

        if (guideActivity) {
            Integer v = (SPmaxPlaces != null) ? SPmaxPlaces.getValue() : null;
            if (v == null || v <= 0) {
                showWarn("Missing max places", "Please choose a maximum number of participants.");
                return;
            }
            maxPlacesToUse = v;
        }

        if (selectedImageFile != null) {
            try {
                selectedImagePath = copyImageToUploads(selectedImageFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                showWarn("Image error", "Unable to copy the image.");
                return;
            }
        }

        if (editMode && activiteToEdit != null) {

            boolean datesChanged =
                    (originalStart != null && !originalStart.equals(dateDebut)) ||
                            (originalEnd != null && !originalEnd.equals(dateFin));

            boolean statusNotAvailable = (status != null && !"DISPONIBLE".equalsIgnoreCase(status));

            activiteToEdit.setNom(nom);
            activiteToEdit.setDescription(description);
            activiteToEdit.setPrix(price);
            activiteToEdit.setTypeActivite(type);
            activiteToEdit.setDestinationId(destinationId);

            if (guideIdToUse != null) activiteToEdit.setGuideId(guideIdToUse);

            activiteToEdit.setStatus(status);
            activiteToEdit.setDateDebut(dateDebut);
            activiteToEdit.setDateFin(dateFin);
            activiteToEdit.setMaxPlaces(maxPlacesToUse);

            if (selectedImagePath != null && !selectedImagePath.isBlank()) {
                activiteToEdit.setImage(selectedImagePath);
            }

            if (datesChanged || statusNotAvailable) {
                activiteToEdit.setFlashPrice(null);
                activiteToEdit.setFlashExpiresAt(null);
            }

            activiteService.update(activiteToEdit);

        } else {

            Activite a = new Activite();
            a.setNom(nom);
            a.setDescription(description);
            a.setPrix(price);
            a.setTypeActivite(type);
            a.setDestinationId(destinationId);

            if (guideIdToUse != null) a.setGuideId(guideIdToUse);

            a.setStatus(status);
            a.setDateDebut(dateDebut);
            a.setDateFin(dateFin);
            a.setNoteMoyenne(0);

            a.setMaxPlaces(maxPlacesToUse);
            a.setImage(selectedImagePath);

            activiteService.add(a);
        }

        closeStage(event);
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage(event);
    }

    public void setActiviteToEdit(Activite activite) {
        this.activiteToEdit = activite;
        this.editMode = true;

        setUpdateModeUI();

        originalStart = activite.getDateDebut();
        originalEnd = activite.getDateFin();

        TFnameactivite.setText(safe(activite.getNom()));
        TFdescriptionactivite.setText(safe(activite.getDescription()));
        TFtypeactivite.setText(safe(activite.getTypeActivite()));

        pricespinneractivite.getValueFactory().setValue(activite.getPrix());

        if (activite.getStatus() != null) {
            CBstatus.getSelectionModel().select(activite.getStatus());
        }

        String destDisplay = activiteService.getDestinationDisplayById(activite.getDestinationId()); // "Ville, Pays"
        if (destDisplay != null && !destDisplay.isBlank()) {
            CBdestination.setValue(destDisplay);
        }

        if (activite.getDateDebut() != null) {
            DPdateDebut.setValue(activite.getDateDebut().toLocalDate());
            SPheureDebut.getValueFactory().setValue(activite.getDateDebut().getHour());
            SPminuteDebut.getValueFactory().setValue(activite.getDateDebut().getMinute());
        }

        if (activite.getDateFin() != null) {
            DPdateFin.setValue(activite.getDateFin().toLocalDate());
            SPheureFin.getValueFactory().setValue(activite.getDateFin().getHour());
            SPminuteFin.getValueFactory().setValue(activite.getDateFin().getMinute());
        }

        if (activite.getGuideId() != 0 && SPmaxPlaces != null) {
            Integer mp = activite.getMaxPlaces();
            if (mp != null && mp > 0) SPmaxPlaces.getValueFactory().setValue(mp);
        }

        if (activite.getImage() != null && !activite.getImage().isBlank()) {
            if (LBLimageName != null) {
                File f = new File(activite.getImage());
                LBLimageName.setText(f.getName());
            }
            if (imagePreview != null) {
                File f = new File(activite.getImage());
                if (f.exists()) imagePreview.setImage(new Image(f.toURI().toString(), true));
            }
        } else {
            if (LBLimageName != null) LBLimageName.setText("No file selected");
            if (imagePreview != null) imagePreview.setImage(null);
        }

        selectedImageFile = null;
        selectedImagePath = null;

        refreshRoleVisibility();
    }

    private LocalDateTime buildDateTime(DatePicker dp, Spinner<Integer> h, Spinner<Integer> m) {
        if (dp == null || dp.getValue() == null) return null;
        LocalDate d = dp.getValue();
        int hh = val(h);
        int mm = val(m);
        return LocalDateTime.of(d, LocalTime.of(hh, mm));
    }

    private int val(Spinner<Integer> sp) {
        if (sp == null) return 0;
        Integer v = sp.getValue();
        return v == null ? 0 : v;
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private Stage getStageSafe() {
        if (TFnameactivite == null || TFnameactivite.getScene() == null) return null;
        return (Stage) TFnameactivite.getScene().getWindow();
    }

    private void showWarn(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}