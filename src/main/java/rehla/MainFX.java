package rehla;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.image.Image;

public class MainFX extends Application {
    double x,y =0;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            //FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Backoffice/Dashboard.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle("Rehla");
            Image icon = new Image(getClass().getResource("/icons/logoblue.png").toExternalForm());
            primaryStage.getIcons().add(icon);
            root.setOnMousePressed(event -> {
                x = event.getSceneX();
                y = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() - x);
                primaryStage.setY(event.getScreenY() - y);
            });
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}