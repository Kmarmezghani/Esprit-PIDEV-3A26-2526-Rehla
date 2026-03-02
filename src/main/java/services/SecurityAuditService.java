package services;

import models.notification;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for security audit logging and analysis.
 * Uses the existing notification table as an audit log.
 */
public class SecurityAuditService {

    public static final String EVENT_OTP_SENT = "OTP_SENT";
    public static final String EVENT_OTP_VERIFIED = "OTP_VERIFIED";
    public static final String EVENT_OTP_FAILED = "OTP_FAILED";
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String EVENT_LOGIN_SUSPICIOUS = "LOGIN_SUSPICIOUS";
    public static final String EVENT_NEW_LOCATION = "NEW_LOCATION_DETECTED";
    public static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_SESSION_LOGOUT = "SESSION_LOGOUT";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rehla?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private static SecurityAuditService instance;

    private SecurityAuditService() {}

    public static synchronized SecurityAuditService getInstance() {
        if (instance == null) {
            instance = new SecurityAuditService();
        }
        return instance;
    }

    /**
     * Logs a security event for a user.
     */
    public void logSecurityEvent(int userId, String eventType, String message) {
        String sql = "INSERT INTO notification (message, type, sender_id, receiver_id, is_read, created_at) " +
                     "VALUES (?, ?, 0, ?, 0, NOW())";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, message);
            ps.setString(2, eventType);
            ps.setInt(3, userId);
            ps.executeUpdate();

            System.out.println("[SecurityAudit] Logged event: " + eventType + " for user " + userId);

        } catch (SQLException e) {
            System.err.println("[SecurityAudit] Error logging event: " + e.getMessage());
        }
    }

    /**
     * Checks if this login is from a suspicious location.
     * Compares with the last known successful login location.
     */
    public boolean isSuspiciousLogin(int userId, String currentCity, String currentCountry) {
        String lastLocation = getLastLoginLocation(userId);

        if (lastLocation == null || lastLocation.isBlank()) {
            return false;
        }

        String currentLocation = currentCity + ", " + currentCountry;
        boolean suspicious = !lastLocation.equalsIgnoreCase(currentLocation);

        if (suspicious) {
            System.out.println("[SecurityAudit] Suspicious login detected for user " + userId +
                    ": Last=" + lastLocation + ", Current=" + currentLocation);
        }

        return suspicious;
    }

    /**
     * Gets the last successful login location from audit logs.
     */
    public String getLastLoginLocation(int userId) {
        String sql = "SELECT message FROM notification " +
                     "WHERE receiver_id = ? AND type = ? " +
                     "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, EVENT_LOGIN_SUCCESS);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String message = rs.getString("message");
                int fromIndex = message.indexOf("from ");
                if (fromIndex > 0) {
                    return message.substring(fromIndex + 5).trim();
                }
            }
        } catch (SQLException e) {
            System.err.println("[SecurityAudit] Error getting last location: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calculates a security score for the user (0-100).
     * Higher is more secure.
     */
    public int calculateSecurityScore(int userId) {
        int score = 100;

        int failedOtpCount = countRecentEvents(userId, EVENT_OTP_FAILED, 24);
        score -= failedOtpCount * 10;

        int suspiciousLogins = countRecentEvents(userId, EVENT_LOGIN_SUSPICIOUS, 24 * 7);
        score -= suspiciousLogins * 20;

        int loginFailures = countRecentEvents(userId, EVENT_LOGIN_FAILED, 24);
        score -= loginFailures * 5;

        int accountLocks = countRecentEvents(userId, EVENT_ACCOUNT_LOCKED, 24 * 30);
        score -= accountLocks * 30;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Gets the security status label based on score.
     */
    public String getSecurityStatus(int score) {
        if (score >= 80) return "Secure";
        if (score >= 50) return "At Risk";
        return "Critical";
    }

    /**
     * Gets the security status color based on score.
     */
    public String getSecurityStatusColor(int score) {
        if (score >= 80) return "#27ae60";
        if (score >= 50) return "#f39c12";
        return "#e74c3c";
    }

    /**
     * Counts recent events of a specific type within the last N hours.
     */
    private int countRecentEvents(int userId, String eventType, int hoursBack) {
        String sql = "SELECT COUNT(*) FROM notification " +
                     "WHERE receiver_id = ? AND type = ? AND created_at > ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, eventType);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusHours(hoursBack)));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[SecurityAudit] Error counting events: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Gets the security timeline (recent security events) for a user.
     */
    public List<SecurityEvent> getSecurityTimeline(int userId, int limit) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT id, message, type, created_at FROM notification " +
                     "WHERE receiver_id = ? AND type IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, EVENT_OTP_SENT);
            ps.setString(3, EVENT_OTP_VERIFIED);
            ps.setString(4, EVENT_OTP_FAILED);
            ps.setString(5, EVENT_LOGIN_SUCCESS);
            ps.setString(6, EVENT_LOGIN_FAILED);
            ps.setString(7, EVENT_LOGIN_SUSPICIOUS);
            ps.setString(8, EVENT_NEW_LOCATION);
            ps.setString(9, EVENT_ACCOUNT_LOCKED);
            ps.setString(10, EVENT_PASSWORD_RESET);
            ps.setString(11, EVENT_SESSION_LOGOUT);
            ps.setInt(12, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SecurityEvent event = new SecurityEvent();
                event.id = rs.getInt("id");
                event.message = rs.getString("message");
                event.type = rs.getString("type");
                event.timestamp = rs.getTimestamp("created_at").toLocalDateTime();
                events.add(event);
            }
        } catch (SQLException e) {
            System.err.println("[SecurityAudit] Error getting timeline: " + e.getMessage());
        }

        return events;
    }

    /**
     * Gets all security events count for the user.
     */
    public int getTotalSecurityEvents(int userId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE receiver_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[SecurityAudit] Error counting total events: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Security event data class.
     */
    public static class SecurityEvent {
        public int id;
        public String message;
        public String type;
        public LocalDateTime timestamp;

        public String getIcon() {
            return switch (type) {
                case EVENT_LOGIN_SUCCESS -> "✓";
                case EVENT_LOGIN_FAILED, EVENT_OTP_FAILED -> "✗";
                case EVENT_LOGIN_SUSPICIOUS, EVENT_NEW_LOCATION -> "⚠";
                case EVENT_OTP_SENT, EVENT_OTP_VERIFIED -> "📧";
                case EVENT_ACCOUNT_LOCKED -> "🔒";
                case EVENT_PASSWORD_RESET -> "🔑";
                case EVENT_SESSION_LOGOUT -> "🚪";
                default -> "•";
            };
        }

        public String getTypeLabel() {
            return switch (type) {
                case EVENT_LOGIN_SUCCESS -> "Login Success";
                case EVENT_LOGIN_FAILED -> "Login Failed";
                case EVENT_LOGIN_SUSPICIOUS -> "Suspicious Login";
                case EVENT_OTP_SENT -> "OTP Sent";
                case EVENT_OTP_VERIFIED -> "OTP Verified";
                case EVENT_OTP_FAILED -> "OTP Failed";
                case EVENT_NEW_LOCATION -> "New Location";
                case EVENT_ACCOUNT_LOCKED -> "Account Locked";
                case EVENT_PASSWORD_RESET -> "Password Reset";
                case EVENT_SESSION_LOGOUT -> "Session Logout";
                default -> type;
            };
        }
    }
}
