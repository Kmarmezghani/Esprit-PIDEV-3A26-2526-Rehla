package services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting login attempts and providing anti-bot protection.
 * Includes simple math captcha functionality.
 */
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;
    private static final int BLOCK_MINUTES = 30;
    private static final int CAPTCHA_THRESHOLD = 3;

    private static RateLimitService instance;

    private final ConcurrentHashMap<String, AttemptData> attemptStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MathCaptcha> captchaStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private RateLimitService() {}

    public static synchronized RateLimitService getInstance() {
        if (instance == null) {
            instance = new RateLimitService();
        }
        return instance;
    }

    /**
     * Checks if the email is blocked due to too many failed attempts.
     */
    public boolean isBlocked(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        AttemptData data = attemptStore.get(key);

        if (data == null) return false;

        if (data.blockedUntil != null && LocalDateTime.now().isBefore(data.blockedUntil)) {
            return true;
        }

        if (data.blockedUntil != null && LocalDateTime.now().isAfter(data.blockedUntil)) {
            attemptStore.remove(key);
            return false;
        }

        return false;
    }

    /**
     * Gets the remaining block time in minutes.
     */
    public long getBlockMinutesRemaining(String email) {
        if (email == null) return 0;
        String key = email.trim().toLowerCase();
        AttemptData data = attemptStore.get(key);

        if (data == null || data.blockedUntil == null) return 0;
        if (LocalDateTime.now().isAfter(data.blockedUntil)) return 0;

        return java.time.Duration.between(LocalDateTime.now(), data.blockedUntil).toMinutes() + 1;
    }

    /**
     * Records a login attempt (success or failure).
     */
    public void recordAttempt(String email, boolean success) {
        if (email == null) return;
        String key = email.trim().toLowerCase();

        if (success) {
            attemptStore.remove(key);
            captchaStore.remove(key);
            return;
        }

        AttemptData data = attemptStore.computeIfAbsent(key, k -> new AttemptData());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(WINDOW_MINUTES);

        data.attempts.removeIf(t -> t.isBefore(windowStart));
        data.attempts.add(now);

        if (data.attempts.size() >= MAX_ATTEMPTS) {
            data.blockedUntil = now.plusMinutes(BLOCK_MINUTES);
            System.out.println("[RateLimit] Blocked " + key + " until " + data.blockedUntil);
        }
    }

    /**
     * Gets the remaining attempts before blocking.
     */
    public int getRemainingAttempts(String email) {
        if (email == null) return MAX_ATTEMPTS;
        String key = email.trim().toLowerCase();
        AttemptData data = attemptStore.get(key);

        if (data == null) return MAX_ATTEMPTS;

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        data.attempts.removeIf(t -> t.isBefore(windowStart));

        return Math.max(0, MAX_ATTEMPTS - data.attempts.size());
    }

    /**
     * Checks if captcha should be required (after CAPTCHA_THRESHOLD failed attempts).
     */
    public boolean requiresCaptcha(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        AttemptData data = attemptStore.get(key);

        if (data == null) return false;

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        data.attempts.removeIf(t -> t.isBefore(windowStart));

        return data.attempts.size() >= CAPTCHA_THRESHOLD;
    }

    /** Session key for registration captcha (no email yet). */
    private static final String REGISTRATION_CAPTCHA_KEY = "_registration_";

    /**
     * Generates a new math captcha for the email.
     * Returns the question string (e.g., "What is 7 + 4?")
     */
    public String generateCaptcha(String email) {
        if (email == null) return null;
        String key = email.trim().toLowerCase();

        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int answer = a + b;

        MathCaptcha captcha = new MathCaptcha(a, b, answer);
        captchaStore.put(key, captcha);

        return "What is " + a + " + " + b + "?";
    }

    /**
     * Generates an advanced math captcha for registration (always required).
     * Uses slightly harder math: two-digit numbers.
     */
    public String generateRegistrationCaptcha() {
        int a = random.nextInt(20) + 5;
        int b = random.nextInt(15) + 3;
        int answer = a + b;
        MathCaptcha captcha = new MathCaptcha(a, b, answer);
        captchaStore.put(REGISTRATION_CAPTCHA_KEY, captcha);
        return "Security check: What is " + a + " + " + b + "?";
    }

    /**
     * Verifies the registration captcha answer.
     */
    public boolean verifyRegistrationCaptcha(String answer) {
        return verifyCaptcha(REGISTRATION_CAPTCHA_KEY, answer);
    }

    /**
     * Clears registration captcha after successful registration.
     */
    public void clearRegistrationCaptcha() {
        captchaStore.remove(REGISTRATION_CAPTCHA_KEY);
    }

    /**
     * Verifies the captcha answer. Email can be REGISTRATION_CAPTCHA_KEY for registration flow.
     */
    public boolean verifyCaptcha(String email, String answer) {
        if (email == null || answer == null) return false;
        String key = email.trim().toLowerCase();

        MathCaptcha captcha = captchaStore.get(key);
        if (captcha == null) return false;

        try {
            int userAnswer = Integer.parseInt(answer.trim());
            boolean correct = (userAnswer == captcha.answer);

            if (correct) {
                captchaStore.remove(key);
            }

            return correct;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if there's an active captcha for this email.
     */
    public boolean hasPendingCaptcha(String email) {
        if (email == null) return false;
        return captchaStore.containsKey(email.trim().toLowerCase());
    }

    /**
     * Clears all data for an email (used on successful login).
     */
    public void clearAll(String email) {
        if (email != null) {
            String key = email.trim().toLowerCase();
            attemptStore.remove(key);
            captchaStore.remove(key);
        }
    }

    private static class AttemptData {
        List<LocalDateTime> attempts = new ArrayList<>();
        LocalDateTime blockedUntil = null;
    }

    private static class MathCaptcha {
        int a, b, answer;

        MathCaptcha(int a, int b, int answer) {
            this.a = a;
            this.b = b;
            this.answer = answer;
        }
    }
}
