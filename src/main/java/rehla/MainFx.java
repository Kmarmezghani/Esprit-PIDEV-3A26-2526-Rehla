package rehla;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainFx extends Application {
double x,y =0;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try{
            // Load HomePage (Frontoffice) - change to /Dashboard.fxml for Backoffice
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 700);  // Set size for HomePage
            primaryStage.initStyle(StageStyle.UNDECORATED);
            root.setOnMousePressed(event -> {
                x = event.getSceneX();
                y = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() - x);
                primaryStage.setY(event.getScreenY() - y);
            });
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }


    }
}
