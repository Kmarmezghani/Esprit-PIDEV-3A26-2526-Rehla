package services;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {

    public enum Type { SUCCESS, INFO, WARNING, ERROR }

    // ===== Public API =====
    public static void show(Stage stage, Type type, String title, String message) {
        show(stage, type, title, message, null, null);
    }

    // Snackbar with action button (ex: "View bookings")
    public static void show(Stage stage,
                            Type type,
                            String title,
                            String message,
                            String actionText,
                            Runnable action) {

        if (stage == null) return;

        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            VBox card = buildCard(type, title, message, actionText, action, popup);
            popup.getContent().add(card);

            popup.show(stage);

            // position bottom-right after show (needs card sizes)
            positionBottomRight(stage, popup, card, 22, 22);

            // fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(140), card);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // auto hide after 3s (longer for error)
            double seconds = (type == Type.ERROR) ? 4.5 : 3.0;
            PauseTransition wait = new PauseTransition(Duration.seconds(seconds));
            wait.setOnFinished(e -> hideWithFade(popup, card));
            wait.play();
        });
    }

    // ===== UI builders =====
    private static VBox buildCard(Type type, String title, String message,
                                  String actionText, Runnable action, Popup popup) {

        Color accent = switch (type) {
            case SUCCESS -> Color.web("#16a34a");
            case INFO    -> Color.web("#3A5BC7");
            case WARNING -> Color.web("#f59e0b");
            case ERROR   -> Color.web("#ef4444");
        };

        VBox root = new VBox(8);
        root.setPadding(new Insets(14));
        root.setMaxWidth(420);
        root.setStyle("""
            -fx-background-color: rgba(17,24,39,0.96);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-color: rgba(255,255,255,0.10);
        """);
        root.setOpacity(0);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(16);
        shadow.setOffsetY(6);
        shadow.setColor(Color.rgb(0, 0, 0, 0.28));
        root.setEffect(shadow);

        // Title row
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("""
            -fx-background-radius: 999;
        """);
        dot.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(accent, new javafx.scene.layout.CornerRadii(999), Insets.EMPTY)
        ));

        Label lblTitle = new Label(title == null ? "" : title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("✕");
        close.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: rgba(255,255,255,0.75);
            -fx-font-size: 14;
            -fx-cursor: hand;
        """);
        close.setOnAction(e -> hideWithFade(popup, root));

        top.getChildren().addAll(dot, lblTitle, spacer, close);

        // Message
        Label lblMsg = new Label(message == null ? "" : message);
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13; -fx-line-spacing: 2;");

        root.getChildren().addAll(top, lblMsg);

        // Optional action button (snackbar style)
        if (actionText != null && !actionText.isBlank() && action != null) {
            Button actionBtn = new Button(actionText);
            actionBtn.setStyle("""
                -fx-background-color: rgba(255,255,255,0.12);
                -fx-text-fill: white;
                -fx-font-weight: 800;
                -fx-background-radius: 10;
                -fx-padding: 8 12;
                -fx-cursor: hand;
            """);
            actionBtn.setOnAction(e -> {
                try { action.run(); } finally { hideWithFade(popup, root); }
            });

            HBox row = new HBox(actionBtn);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(6, 0, 0, 0));
            root.getChildren().add(row);
        }

        return root;
    }

    private static void hideWithFade(Popup popup, VBox card) {
        if (popup == null || card == null) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(160), card);
        fadeOut.setFromValue(card.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
    }

    private static void positionBottomRight(Stage stage, Popup popup, VBox card,
                                            double marginRight, double marginBottom) {

        Scene scene = stage.getScene();
        if (scene == null) return;

        double x = stage.getX() + scene.getX() + scene.getWidth() - card.getWidth() - marginRight;
        double y = stage.getY() + scene.getY() + scene.getHeight() - card.getHeight() - marginBottom;

        popup.setX(x);
        popup.setY(y);
    }
}
