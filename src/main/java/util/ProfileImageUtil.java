package util;

import javafx.scene.image.Image;
import models.Personne;

import java.io.File;

/**
 * Small helper to get the current user's profile image anywhere in the app.
 * Uses the logged-in user from {@link Session} and falls back to a default icon.
 */
public final class ProfileImageUtil {

    private ProfileImageUtil() {
        // utility class
    }

    /**
     * Returns the JavaFX {@link Image} for the currently logged-in user.
     * <p>
     * Priority:
     * 1) If the session user has a non-empty {@code profilePhoto} path and the file exists,
     *    that image is used.
     * 2) Otherwise, the bundled {@code /Backoffice/icons/usericon.png} is used.
     * 3) If even the fallback is missing, {@code null} is returned.
     *
     * @param requestedWidth  suggested width (pass 0 to keep original)
     * @param requestedHeight suggested height (pass 0 to keep original)
     */
    public static Image getCurrentUserProfileImage(double requestedWidth, double requestedHeight) {
        Personne user = Session.getCurrentUser();

        // 1) Try user-specific photo path from DB
        if (user != null && user.getProfilePhoto() != null && !user.getProfilePhoto().isEmpty()) {
            try {
                File file = new File(user.getProfilePhoto());
                if (file.exists()) {
                    String uri = file.toURI().toString();
                    return new Image(uri,
                            requestedWidth > 0 ? requestedWidth : 0,
                            requestedHeight > 0 ? requestedHeight : 0,
                            true,
                            true
                    );
                }
            } catch (Exception ignored) {
            }
        }

        // 2) Fallback to bundled default avatar
        try {
            java.net.URL url = ProfileImageUtil.class.getResource("/Backoffice/icons/usericon.png");
            if (url != null) {
                return new Image(url.toExternalForm(),
                        requestedWidth > 0 ? requestedWidth : 0,
                        requestedHeight > 0 ? requestedHeight : 0,
                        true,
                        true
                );
            }
        } catch (Exception ignored) {
        }

        // 3) Absolute last resort
        return null;
    }
}

