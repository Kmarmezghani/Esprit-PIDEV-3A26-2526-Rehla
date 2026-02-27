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
            // SWITCH BETWEEN VIEWS - Uncomment the one you want to test:
            
            // Option 1: ADMIN DASHBOARD (Backoffice) - Manage data with tables
             FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
             primaryStage.setTitle("Rehla - Admin Dashboard");
            
            // Option 2: CLIENT FRONTOFFICE (Multi-page user interface) ✨ RECOMMENDED
            //FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/HomePage.fxml"));
          // primaryStage.setTitle("Rehla - Your Travel Companion");
            

            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
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
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }


    }
}
