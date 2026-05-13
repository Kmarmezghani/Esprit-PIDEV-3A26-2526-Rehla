package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utility for switching scenes without resizing the window.
 * Always preserves the current stage dimensions and maximized state.
 */
public class NavigationUtil {

    public static void switchScene(Stage stage, Parent root) {
        boolean wasMaximized = stage.isMaximized();
        double w = stage.getScene() != null ? stage.getWidth()  : 1200;
        double h = stage.getScene() != null ? stage.getHeight() : 750;

        stage.setMaximized(false);
        stage.setScene(new Scene(root));
        if (wasMaximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(w);
            stage.setHeight(h);
        }
        stage.show();
    }

    public static void switchScene(Stage stage, String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));
        Parent root = (Parent) loader.load();
        switchScene(stage, root);
    }
}
