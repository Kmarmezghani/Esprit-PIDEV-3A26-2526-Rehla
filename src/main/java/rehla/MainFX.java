package rehla;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import models.NotificationScheduler;

public class MainFX extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {

            NotificationScheduler scheduler = new NotificationScheduler();
            scheduler.start();
            primaryStage = stage;

            Parent root = FXMLLoader.load(MainFX.class.getResource("/loginPage.fxml"));
            Scene scene = new Scene(root);

            stage.setTitle("Rehla");
            stage.setScene(scene);

            try {
                Image icon = new Image(MainFX.class.getResource("/icons/logoblue.png").toExternalForm());
                stage.getIcons().add(icon);
            } catch (Exception ignored) {}

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setRoot(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(MainFX.class.getResource(fxmlPath));
            primaryStage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}