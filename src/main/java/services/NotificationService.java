package services;

import models.notification;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private final Connection cnx;

    public NotificationService() {
        this.cnx = DBConnection.getInstance().getConn();
    }

    public void createWaitlistHoldNotif(int senderId, int receiverId, int activiteId, String message) throws SQLException {
        String sql = """
            INSERT INTO notification
                (message, type, post_id, comment_id, activite_id, sender_id, receiver_id, is_read)
            VALUES
                (?, 'WAITLIST_HOLD', NULL, NULL, ?, ?, ?, 0)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setInt(2, activiteId);
            ps.setInt(3, senderId);
            ps.setInt(4, receiverId);
            ps.executeUpdate();
        }
    }

    public int countUnread(int receiverId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE receiver_id=? AND IFNULL(is_read,0)=0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<NotifRow> getLatest(int receiverId, int limit) throws SQLException {
        String sql = """
            SELECT id, message, type, post_id, comment_id, activite_id, sender_id, receiver_id, is_read, created_at
            FROM notification
            WHERE receiver_id = ?
            ORDER BY created_at DESC
            LIMIT ?
        """;
        List<NotifRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new NotifRow(
                            rs.getInt("id"),
                            rs.getString("message"),
                            rs.getString("type"),
                            (Integer) rs.getObject("post_id"),
                            (Integer) rs.getObject("comment_id"),
                            (Integer) rs.getObject("activite_id"),
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getInt("is_read") == 1,
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return list;
    }

    public void markRead(int notifId) throws SQLException {
        String sql = "UPDATE notification SET is_read=1 WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        }
    }

    public void markAllRead(int receiverId) throws SQLException {
        String sql = "UPDATE notification SET is_read=1 WHERE receiver_id=? AND IFNULL(is_read,0)=0";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.executeUpdate();
        }
    }

    public static class NotifRow {
        public final int id;
        public final String message;
        public final String type;
        public final Integer postId;
        public final Integer commentId;
        public final Integer activiteId;
        public final int senderId;
        public final int receiverId;
        public final boolean isRead;
        public final Timestamp createdAt;

        public NotifRow(int id, String message, String type, Integer postId, Integer commentId, Integer activiteId,
                        int senderId, int receiverId, boolean isRead, Timestamp createdAt) {
            this.id = id;
            this.message = message;
            this.type = type;
            this.postId = postId;
            this.commentId = commentId;
            this.activiteId = activiteId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }
    public List<NotifRow> getLatestUnread(int receiverId, int limit) throws SQLException {
        String sql = """
        SELECT id, message, type, post_id, comment_id, activite_id, sender_id, receiver_id, is_read, created_at
        FROM notification
        WHERE receiver_id = ?
          AND IFNULL(is_read,0) = 0
        ORDER BY created_at DESC
        LIMIT ?
    """;
        List<NotifRow> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer senderObj = (Integer) rs.getObject("sender_id"); // peut être null
                    list.add(new NotifRow(
                            rs.getInt("id"),
                            rs.getString("message"),
                            rs.getString("type"),
                            (Integer) rs.getObject("post_id"),
                            (Integer) rs.getObject("comment_id"),
                            (Integer) rs.getObject("activite_id"),
                            senderObj == null ? 0 : senderObj,
                            rs.getInt("receiver_id"),
                            rs.getInt("is_read") == 1,
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return list;
    }
    private final String URL = "jdbc:mysql://localhost:3306/rehla?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    public void add(notification n) {

        String sql = "INSERT INTO notification " +
                "(message, type, post_id, comment_id, activite_id, sender_id, receiver_id, is_read, is_sent_sms, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, NOW())";
        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, n.getMessage());
            ps.setString(2, n.getType());
            ps.setObject(3, n.getPostId());
            ps.setObject(4, n.getCommentId());
            ps.setObject(5, n.getActiviteId());
            ps.setInt(6, n.getSenderId());
            ps.setInt(7, n.getReceiverId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= GET NOTIFICATIONS =================
    public List<notification> getByReceiver(int receiverId) {

        List<notification> list = new ArrayList<>();

        String sql = "SELECT * FROM notification WHERE receiver_id = ? ORDER BY created_at DESC";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, receiverId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                notification n = new notification(
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        (Integer) rs.getObject("post_id"),
                        (Integer) rs.getObject("comment_id"),
                        (Integer) rs.getObject("activite_id"), // 🔥 ajouté
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getBoolean("is_read"),
                        rs.getBoolean("is_sent_sms"), // 🔥 ajouté
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                list.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<notification> getByReceiver2(int receiverId) {

        List<notification> list = new ArrayList<>();

        String sql;

        // Si c’est le root fictif, récupérer toutes les notifications (ou un subset si tu veux)
        if (receiverId == 0) {
            sql = "SELECT * FROM notification ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM notification WHERE receiver_id = ? ORDER BY created_at DESC";
        }

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            if (receiverId != 0) ps.setInt(1, receiverId); // pour les vrais users

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notification n = new notification(
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        (Integer) rs.getObject("post_id"),
                        (Integer) rs.getObject("comment_id"),
                        (Integer) rs.getObject("activite_id"),
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getBoolean("is_read"),
                        rs.getBoolean("is_sent_sms"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                list.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    // Récupérer les notifications non encore envoyées par SMS pour un utilisateur
    public List<notification> getPendingSmsNotifications(int receiverId) {
        List<notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE receiver_id = ? AND is_sent_sms = 0 ORDER BY created_at ASC";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notification n = new notification(
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        (Integer) rs.getObject("post_id"),
                        (Integer) rs.getObject("comment_id"),
                        (Integer) rs.getObject("activite_id"),
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getBoolean("is_read"),
                        rs.getBoolean("is_sent_sms"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                list.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    // Marquer plusieurs notifications comme envoyées
    public void markAllAsSmsSent(List<notification> notifications) {

        String sql = "UPDATE notification SET is_sent_sms = 1 WHERE id = ?";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            for(notification n : notifications){
                ps.setInt(1, n.getId());
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Méthode pour générer un message récapitulatif SMS pour un utilisateur
    public String buildDailySmsMessage(List<notification> notifications) {
        StringBuilder sb = new StringBuilder();
        sb.append("📢 Notifications du jour :\n");
        int count = 0;
        for (notification n : notifications) {
            if (count >= 10) break; // max 10 notifications par SMS
            sb.append("• ").append(n.getMessage()).append("\n");
            count++;
        }
        return sb.toString();
    }



}