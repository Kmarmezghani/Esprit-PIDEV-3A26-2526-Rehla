package services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating and verifying One-Time Passwords (OTP).
 * OTPs are stored in-memory with expiration and attempt tracking.
 */
public class OTPService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    private static OTPService instance;

    private final ConcurrentHashMap<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private OTPService() {}

    public static synchronized OTPService getInstance() {
        if (instance == null) {
            instance = new OTPService();
        }
        return instance;
    }

    /**
     * Generates a new 6-digit OTP for the given email.
     * Invalidates any previous OTP for this email.
     */
    public String generateOTP(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String otp = generateSecureOTP();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        otpStore.put(normalizedEmail, new OTPData(otp, expiresAt, 0));

        System.out.println("[OTP] Generated OTP for " + normalizedEmail + ": " + otp + " (expires: " + expiresAt + ")");
        return otp;
    }

    /**
     * Verifies the OTP for the given email.
     * Returns true if valid, false otherwise.
     * Tracks failed attempts and invalidates after max attempts.
     */
    public boolean verifyOTP(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedCode = code.trim();

        OTPData data = otpStore.get(normalizedEmail);

        if (data == null) {
            System.out.println("[OTP] No OTP found for " + normalizedEmail);
            return false;
        }

        if (LocalDateTime.now().isAfter(data.expiresAt)) {
            System.out.println("[OTP] OTP expired for " + normalizedEmail);
            otpStore.remove(normalizedEmail);
            return false;
        }

        if (data.attempts >= MAX_ATTEMPTS) {
            System.out.println("[OTP] Max attempts exceeded for " + normalizedEmail);
            otpStore.remove(normalizedEmail);
            return false;
        }

        if (data.code.equals(normalizedCode)) {
            System.out.println("[OTP] OTP verified successfully for " + normalizedEmail);
            otpStore.remove(normalizedEmail);
            return true;
        }

        data.attempts++;
        System.out.println("[OTP] Invalid OTP for " + normalizedEmail + " (attempt " + data.attempts + "/" + MAX_ATTEMPTS + ")");

        if (data.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(normalizedEmail);
        }

        return false;
    }

    /**
     * Invalidates any existing OTP for the given email.
     */
    public void invalidateOTP(String email) {
        if (email != null) {
            otpStore.remove(email.trim().toLowerCase());
        }
    }

    /**
     * Checks if an OTP exists and is still valid for the given email.
     */
    public boolean hasValidOTP(String email) {
        if (email == null) return false;
        OTPData data = otpStore.get(email.trim().toLowerCase());
        return data != null && LocalDateTime.now().isBefore(data.expiresAt);
    }

    /**
     * Gets the remaining time in seconds for the OTP.
     */
    public long getRemainingSeconds(String email) {
        if (email == null) return 0;
        OTPData data = otpStore.get(email.trim().toLowerCase());
        if (data == null || LocalDateTime.now().isAfter(data.expiresAt)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), data.expiresAt).getSeconds();
    }

    /**
     * Gets remaining verification attempts for the email.
     */
    public int getRemainingAttempts(String email) {
        if (email == null) return 0;
        OTPData data = otpStore.get(email.trim().toLowerCase());
        if (data == null) return MAX_ATTEMPTS;
        return MAX_ATTEMPTS - data.attempts;
    }

    private String generateSecureOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private static class OTPData {
        final String code;
        final LocalDateTime expiresAt;
        int attempts;

        OTPData(String code, LocalDateTime expiresAt, int attempts) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.attempts = attempts;
        }
    }
}
