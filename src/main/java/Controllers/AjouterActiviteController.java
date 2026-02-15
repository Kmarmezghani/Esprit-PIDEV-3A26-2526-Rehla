package Controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;

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

    @FXML private TextField TFguideId;

    @FXML private ComboBox<String> CBstatus;
    @FXML private ComboBox<String> CBdestination;

    private Map<String, Integer> destinationMap;

    private Activite activiteToEdit = null;
    private boolean editMode = false;

    @FXML
    public void initialize() {

        pricespinneractivite.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100000.0, 0.0, 5.0)
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

        CBstatus.setItems(FXCollections.observableArrayList("DISPONIBLE", "NON_DISPONIBLE"));
        CBstatus.getSelectionModel().selectFirst();

        destinationMap = activiteService.getDestinationsMap();
        CBdestination.setItems(FXCollections.observableArrayList(destinationMap.keySet()));
    }

    @FXML
    void ajouterActivite(ActionEvent event) {

        String nom = TFnameactivite.getText();
        String description = TFdescriptionactivite.getText();
        String type = TFtypeactivite.getText();

        Double price = pricespinneractivite.getValue();
        int guideId = parseIntOrZero(TFguideId);

        String status = CBstatus.getValue();

        LocalDateTime dateDebut = buildDateTime(DPdateDebut, SPheureDebut, SPminuteDebut);
        LocalDateTime dateFin = buildDateTime(DPdateFin, SPheureFin, SPminuteFin);

        String destNom = CBdestination.getValue();
        int destinationId = (destNom != null && destinationMap.containsKey(destNom)) ? destinationMap.get(destNom) : 0;

        if (nom == null || nom.isBlank()) {
            showWarn("Missing name", "Please enter the activity name.");
            return;
        }

        if (description == null || description.isBlank()) {
            showWarn("Missing description", "Please enter the activity description.");
            return;
        }

        if (type == null || type.isBlank()) {
            showWarn("Missing type", "Please enter the activity type.");
            return;
        }

        if (destinationId == 0) {
            showWarn("Missing destination", "Please select a destination.");
            return;
        }

        if (dateDebut == null) {
            showWarn("Missing start date", "Please select a start date and time.");
            return;
        }

        if (dateFin == null) {
            showWarn("Missing end date", "Please select an end date and time.");
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (dateDebut.toLocalDate().isBefore(today)) {
            showWarn("Invalid start date", "Start date cannot be before today.");
            return;
        }

        if (dateDebut.toLocalDate().isEqual(today) && dateDebut.isBefore(now)) {
            showWarn("Invalid start time", "Start time cannot be earlier than the current time.");
            return;
        }

        if (dateFin.isBefore(dateDebut)) {
            showWarn("Invalid dates", "End date must be after start date.");
            return;
        }

        if (editMode && activiteToEdit != null) {

            activiteToEdit.setNom(nom);
            activiteToEdit.setDescription(description);
            activiteToEdit.setPrix(price);
            activiteToEdit.setTypeActivite(type);

            activiteToEdit.setDestinationId(destinationId);
            activiteToEdit.setGuideId(guideId);

            activiteToEdit.setStatus(status);
            activiteToEdit.setDateDebut(dateDebut);
            activiteToEdit.setDateFin(dateFin);

            activiteService.update(activiteToEdit);

        } else {

            Activite a = new Activite();
            a.setNom(nom);
            a.setDescription(description);
            a.setPrix(price);
            a.setTypeActivite(type);

            a.setDestinationId(destinationId);
            a.setGuideId(guideId);

            a.setStatus(status);
            a.setDateDebut(dateDebut);
            a.setDateFin(dateFin);

            a.setNoteMoyenne(0);

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
    }

    private int parseIntOrZero(TextField tf) {
        if (tf == null) return 0;
        String s = tf.getText();
        if (s == null || s.isBlank()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            showWarn("Invalid value", "Please enter a valid integer for: " + tf.getId());
            return 0;
        }
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
