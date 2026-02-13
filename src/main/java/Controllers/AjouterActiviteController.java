package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Activite;
import services.ActiviteService;


public class AjouterActiviteController {

   private ActiviteService activiteService=new ActiviteService();
/*
    @FXML
    private TextField tfage;

    @FXML
    private TextField tfnom;

    @FXML
    private TextField tfprenom;

    @FXML
    void ajouterActivite(ActionEvent event) {
        String nom=tfnom.getText().toString();
        String prenom=tfprenom.getText().toString();
        int age=Integer.parseInt(tfage.getText());
        //Activite activite = new Activite(nom,prenom,age);
        //activiteService.add(activite);

    }*/
   @FXML
   private TextField TFdescriptionactivite;

    @FXML
    private TextField TFnameactivite;

    @FXML
    private TextField TFtypeactivite;

    @FXML
    private Spinner<Double> durationspinneractivite;

    @FXML
    private Spinner<Double> pricespinneractivite;
    private Activite activiteToEdit = null;
    private boolean editMode = false;


    @FXML
    public void initialize() {

        pricespinneractivite.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10000.0, 0.0, 5.0)
        );
        pricespinneractivite.setEditable(true);


        durationspinneractivite.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 30.0, 1.0, 0.5)
        );
        durationspinneractivite.setEditable(true);
    }

    @FXML
    void ajouterActivite(ActionEvent event) {

        String nom = TFnameactivite.getText();
        String description = TFdescriptionactivite.getText();
        String type = TFtypeactivite.getText();

        Double price = pricespinneractivite.getValue();
        Double duration = durationspinneractivite.getValue();

        if (nom == null || nom.isBlank() ||
                description == null || description.isBlank() ||
                type == null || type.isBlank()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Champs manquants");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez remplir Name, Description et Type.");
            alert.showAndWait();
            return;
        }

        if (editMode) {
            // 🔥 UPDATE MODE
            activiteToEdit.setNom(nom);
            activiteToEdit.setDescription(description);
            activiteToEdit.setTypeActivite(type);
            activiteToEdit.setPrix(price);
            activiteToEdit.setDuree(duration);

            activiteService.update(activiteToEdit);

        } else {
            Activite activite = new Activite(nom, description, price, duration, type, 0);
            activiteService.add(activite);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }


    @FXML
   void handleCancel(ActionEvent event) {
       Stage stage = (Stage) ((Node) event.getSource())
               .getScene()
               .getWindow();
       stage.close();

   }
    public void setActiviteToEdit(Activite activite) {

        this.activiteToEdit = activite;
        this.editMode = true;

        // Pré-remplir les champs
        TFnameactivite.setText(activite.getNom());
        TFdescriptionactivite.setText(activite.getDescription());
        TFtypeactivite.setText(activite.getTypeActivite());

        pricespinneractivite.getValueFactory().setValue(activite.getPrix());
        durationspinneractivite.getValueFactory().setValue(activite.getDuree());
    }


}
