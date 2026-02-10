package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class test {

    @FXML
    private void openAddPopup(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Formulaire.fxml"));

            Stage popupStage = new Stage();
            popupStage.setTitle("Add new user");
            popupStage.initModality(Modality.APPLICATION_MODAL); // block main window
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace(); // will show real error if popup fails
        }
    }
}
