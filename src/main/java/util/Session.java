package util;

import models.Personne;

/**
 * Holds the currently logged-in user. Used after login and cleared on logout.
 */
public class Session {

    private static Personne currentUser;

    public static Personne getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Personne user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
