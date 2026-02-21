package services;


import models.notification;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class notificationService {


    private Connection cnx;

    public notificationService() {
        cnx = DBConnection.getInstance().getConn();
    }

    // ================= AJOUT =================
    public void add(notification n) {

        String sql = "INSERT INTO notification " +
                "(message, type, post_id, comment_id, sender_id, receiver_id, is_read, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0, NOW())";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, n.getMessage());
            ps.setString(2, n.getType());
            ps.setObject(3, n.getPostId());
            ps.setObject(4, n.getCommentId());
            ps.setInt(5, n.getSenderId());
            ps.setInt(6, n.getReceiverId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= GET ADMIN NOTIFS =================
    public List<notification> getByReceiver(int receiverId) {

        List<notification> list = new ArrayList<>();

        String sql = "SELECT * FROM notification WHERE receiver_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, receiverId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                notification n = new notification(
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        (Integer) rs.getObject("post_id"),
                        (Integer) rs.getObject("comment_id"),
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getBoolean("is_read"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                list.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}
