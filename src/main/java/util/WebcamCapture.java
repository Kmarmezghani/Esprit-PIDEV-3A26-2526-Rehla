package util;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Webcam capture utility for JavaFX.
 * Uses the sarxos webcam-capture library.
 */
public class WebcamCapture {

    private Webcam webcam;
    private ScheduledExecutorService executor;
    private ImageView imageView;
    private boolean running;
    private Consumer<BufferedImage> onCapture;

    public WebcamCapture() {
    }

    /**
     * Opens the default webcam and starts streaming to the ImageView.
     */
    public boolean start(ImageView targetView) {
        this.imageView = targetView;
        
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("[WebcamCapture] No webcam found");
                return false;
            }

            Dimension[] customSizes = new Dimension[] {
                WebcamResolution.VGA.getSize(),
                new Dimension(640, 480),
                new Dimension(320, 240)
            };
            webcam.setCustomViewSizes(customSizes);
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            
            if (!webcam.open()) {
                System.err.println("[WebcamCapture] Could not open webcam");
                return false;
            }

            running = true;
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this::updateFrame, 0, 33, TimeUnit.MILLISECONDS);

            System.out.println("[WebcamCapture] Started webcam: " + webcam.getName());
            return true;

        } catch (Exception e) {
            System.err.println("[WebcamCapture] Error starting webcam: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the ImageView with the current webcam frame.
     */
    private void updateFrame() {
        if (!running || webcam == null || !webcam.isOpen()) {
            return;
        }

        try {
            BufferedImage frame = webcam.getImage();
            if (frame != null && imageView != null) {
                Image fxImage = SwingFXUtils.toFXImage(frame, null);
                Platform.runLater(() -> imageView.setImage(fxImage));
            }
        } catch (Exception e) {
            // Ignore frame errors
        }
    }

    /**
     * Captures the current frame.
     */
    public BufferedImage capture() {
        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }
        return null;
    }

    /**
     * Stops the webcam and releases resources.
     */
    public void stop() {
        running = false;
        
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            System.out.println("[WebcamCapture] Stopped webcam");
        }
    }

    /**
     * Checks if a webcam is available.
     */
    public static boolean isWebcamAvailable() {
        try {
            return Webcam.getDefault() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets a list of available webcam names.
     */
    public static String[] getWebcamNames() {
        try {
            return Webcam.getWebcams().stream()
                .map(Webcam::getName)
                .toArray(String[]::new);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
