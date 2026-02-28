package util;

import java.util.regex.Pattern;

/**
 * Input validation for registration and profile (email, password, names).
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /** Valid email format (e.g. user@domain.com). */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /** Strong password: at least one digit and at least one special character. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetter(c)) hasSpecial = true; // special = not letter, not digit
            if (hasDigit && hasSpecial) return true;
        }
        return false;
    }

    /** Name (nom / prénom) must not start with a number. */
    public static boolean nameDoesNotStartWithNumber(String name) {
        if (name == null || name.isBlank()) return true;
        return !Character.isDigit(name.trim().charAt(0));
    }

    public static String emailErrorMessage() {
        return "Please enter a valid email address (e.g. user@example.com).";
    }

    public static String strongPasswordErrorMessage() {
        return "Password must be at least 6 characters and contain at least one number and one special character.";
    }

    public static String nameErrorMessage() {
        return "Name (Nom / Prénom) must not start with a number.";
    }
}
