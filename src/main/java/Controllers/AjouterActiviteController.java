package Controllers;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class AjouterActiviteController {

    private final ActiviteService activiteService = new ActiviteService();

    @FXML private TextField TFdescriptionactivite;
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
    @FXML private ImageView imagePreview;   // ✅ preview comme ton partenaire

    private Map<String, Integer> destinationMap;

    private Activite activiteToEdit = null;
    private boolean editMode = false;

    private Integer fixedGuideId = null;
    private boolean adminMode = false;

    private File selectedImageFile = null;      // ✅ comme ton partenaire
    private String selectedImagePath = null;    // chemin final sauvegardé (après copie)

    public void setGuideId(int guideId) {
        this.fixedGuideId = guideId;
        updateMaxPlacesVisibility();
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        updateMaxPlacesVisibility();
    }

    @FXML
    public void initialize() {

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

        if (LBLimageName != null) LBLimageName.setText("Aucun fichier");

        updateMaxPlacesVisibility();
    }

    private void updateMaxPlacesVisibility() {
        if (SPmaxPlaces == null) return;

        boolean isGuideCreating = (fixedGuideId != null) && !adminMode;

        SPmaxPlaces.setVisible(isGuideCreating);
        SPmaxPlaces.setManaged(isGuideCreating);

        if (LBLmaxPlaces != null) {
            LBLmaxPlaces.setVisible(isGuideCreating);
            LBLmaxPlaces.setManaged(isGuideCreating);
        }
    }

    // ✅ EXACTEMENT comme ton partenaire
    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        selectedImageFile = fileChooser.showOpenDialog(null);

        if (selectedImageFile != null) {
            if (LBLimageName != null) LBLimageName.setText(selectedImageFile.getName());

            if (imagePreview != null) {
                imagePreview.setImage(new Image(selectedImageFile.toURI().toString()));
            }
        }
    }

    // ✅ EXACTEMENT comme ton partenaire (dossier HOME)
    private String copierImagePath(File imageFile) throws IOException {

        String dossier = System.getProperty("user.home") + "/myapp/uploads/activities/";
        Files.createDirectories(Paths.get(dossier));

        String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
        String fileName = "activity_" + System.currentTimeMillis() + extension;

        Path destination = Paths.get(dossier + fileName);

        Files.copy(
                imageFile.toPath(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        return destination.toAbsolutePath().toString(); // ✅ chemin absolu
    }

    @FXML
    void ajouterActivite(ActionEvent event) {

        String nom = TFnameactivite.getText();
        String description = TFdescriptionactivite.getText();
        String type = TFtypeactivite.getText();

        Double price = pricespinneractivite.getValue();
        String status = CBstatus.getValue();

        LocalDateTime dateDebut = buildDateTime(DPdateDebut, SPheureDebut, SPminuteDebut);
        LocalDateTime dateFin = buildDateTime(DPdateFin, SPheureFin, SPminuteFin);

        String destNom = CBdestination.getValue();
        int destinationId = (destNom != null && destinationMap.containsKey(destNom))
                ? destinationMap.get(destNom)
                : 0;

        if (nom == null || nom.isBlank()) { showWarn("Nom manquant", "Veuillez saisir le nom."); return; }
        if (description == null || description.isBlank()) { showWarn("Description manquante", "Veuillez saisir la description."); return; }
        if (type == null || type.isBlank()) { showWarn("Type manquant", "Veuillez saisir le type."); return; }
        if (price == null || price <= 0) { showWarn("Prix invalide", "Le prix doit être > 0."); return; }

        if (destinationId == 0) { showWarn("Destination manquante", "Veuillez choisir une destination."); return; }
        if (dateDebut == null) { showWarn("Date début manquante", "Veuillez choisir la date début."); return; }
        if (dateFin == null) { showWarn("Date fin manquante", "Veuillez choisir la date fin."); return; }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (dateDebut.toLocalDate().isBefore(today)) { showWarn("Date début invalide", "La date début ne peut pas être avant aujourd'hui."); return; }
        if (dateDebut.toLocalDate().isEqual(today) && dateDebut.isBefore(now)) { showWarn("Heure début invalide", "L'heure début ne peut pas être avant maintenant."); return; }
        if (dateFin.isBefore(dateDebut)) { showWarn("Dates invalides", "La date fin doit être après la date début."); return; }

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
        boolean isGuideActivity = (guideIdToUse != null);

        if (isGuideActivity) {
            Integer v = (SPmaxPlaces != null) ? SPmaxPlaces.getValue() : null;
            if (v == null || v <= 0) {
                showWarn("Max places manquant", "Veuillez choisir un max de participants.");
                return;
            }
            maxPlacesToUse = v;
        }

        // ✅ si image choisie : copier vers HOME/myapp/uploads/activities/
        if (selectedImageFile != null) {
            try {
                selectedImagePath = copierImagePath(selectedImageFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                showWarn("Erreur image", "Impossible de copier l'image.");
                return;
            }
        }

        if (editMode && activiteToEdit != null) {

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

            // ✅ seulement si une nouvelle image a été choisie
            if (selectedImagePath != null && !selectedImagePath.isBlank()) {
                activiteToEdit.setImage(selectedImagePath);
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

            a.setImage(selectedImagePath); // ممكن تكون null

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

        TFnameactivite.setText(activite.getNom());
        TFdescriptionactivite.setText(activite.getDescription());
        TFtypeactivite.setText(activite.getTypeActivite());

        pricespinneractivite.getValueFactory().setValue(activite.getPrix());

        if (activite.getStatus() != null) {
            CBstatus.getSelectionModel().select(activite.getStatus());
        }

        String nomDest = activiteService.getDestinationNameById(activite.getDestinationId());
        if (nomDest != null && !nomDest.isBlank()) CBdestination.setValue(nomDest);

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

        updateMaxPlacesVisibility();

        if (SPmaxPlaces != null && activite.getGuideId() != 0) {
            Integer mp = activite.getMaxPlaces();
            if (mp != null && mp > 0) {
                SPmaxPlaces.getValueFactory().setValue(mp);
            }
        }

        // ✅ afficher image existante dans preview
        if (activite.getImage() != null && !activite.getImage().isBlank()) {

            if (LBLimageName != null) {
                File f = new File(activite.getImage());
                LBLimageName.setText(f.getName());
            }

            if (imagePreview != null) {
                try {
                    File f = new File(activite.getImage());
                    if (f.exists()) {
                        imagePreview.setImage(new Image(f.toURI().toString()));
                    }
                } catch (Exception ignored) {}
            }
        } else {
            if (LBLimageName != null) LBLimageName.setText("Aucun fichier");
            if (imagePreview != null) imagePreview.setImage(null);
        }

        // ✅ reset new selection
        selectedImageFile = null;
        selectedImagePath = null;
    }

    private LocalDateTime buildDateTime(DatePicker dp, Spinner<Integer> h, Spinner<Integer> m) {
        if (dp == null || dp.getValue() == null) return null;
        LocalDate d = dp.getValue();
        int hh = (h != null && h.getValue() != null) ? h.getValue() : 0;
        int mm = (m != null && m.getValue() != null) ? m.getValue() : 0;
        return LocalDateTime.of(d, LocalTime.of(hh, mm));
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showWarn(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}