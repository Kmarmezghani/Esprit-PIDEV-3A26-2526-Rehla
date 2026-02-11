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

        // (Optionnel) mini validation
        if (nom == null || nom.isBlank() || description == null || description.isBlank() || type == null || type.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Champs manquants");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez remplir Name, Description et Type.");
            alert.showAndWait();
            return;
        }

        Activite activite = new Activite(nom, description, price, duration, type, 0);
        // ↑ Mets 0 si guideId est int. Sinon adapte selon ton modèle.

        activiteService.add(activite);

        // fermer la popup
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

}
