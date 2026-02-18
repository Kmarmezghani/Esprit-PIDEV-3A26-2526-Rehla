package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.StageStyle;

public class ClientPostsController {


    @FXML
    private BorderPane mainContent;

    @FXML
    private StackPane root;   // ajoute fx:id="root" au StackPane


    @FXML
    private void handleComment(ActionEvent event) {
        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/CommentPopup.fxml"));

            Parent popup = loader.load();
            CommentPopupController controller = loader.getController();

            // 🔥 Blur seulement sur le blog
            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            // IMPORTANT: ne pas toucher la taille du popup
            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            // 🔥 Fermeture
            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public void handleFullScreen(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }


}





