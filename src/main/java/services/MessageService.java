package services;

import models.Conversation;
import models.Message;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private final String URL = "jdbc:mysql://localhost:3306/rehla?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

   /* private final String URL = "jdbc:mysql://127.0.0.1:3306/rehla?useSSL=false&serverTimezone=UTC";
    private final String USER = "REHLA";
    private final String PASSWORD = "Rehla123";*/


    // Envoyer un message
    public void sendMessage(Message msg) {
        String sql = "INSERT INTO message(conversation_id,sender_id,contenu) VALUES (?,?,?)";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, msg.getConversationId());
            ps.setInt(2, msg.getSenderId());
            ps.setString(3, msg.getContenu());
            ps.executeUpdate();

        } catch (Exception e) {
            System.out.println("Erreur ajout message : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Récupérer les messages d'une conversation
    public List<Message> getMessagesByConversation(int conversationId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM message WHERE conversation_id=? ORDER BY sent_at";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt("id"));
                m.setConversationId(rs.getInt("conversation_id"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setSentAt(rs.getObject("sent_at", LocalDateTime.class));
                m.setContenu(rs.getString("contenu"));
                list.add(m);
            }

        } catch (Exception e) {
            System.out.println("Erreur récupération messages : " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }


    public Message getLastMessage(int conversationId) {

        String sql = "SELECT * FROM message " +
                "WHERE conversation_id = ? " +
                "ORDER BY sent_at DESC LIMIT 1";

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt("id"));
                m.setContenu(rs.getString("contenu"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setSentAt(rs.getObject("sent_at", java.time.LocalDateTime.class));
                m.setRead(rs.getBoolean("is_read"));
                return m;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void markConversationAsRead(int conversationId, int userId) {

        String sql = """
        UPDATE message
        SET is_read = true
        WHERE conversation_id = ?
        AND sender_id != ?
        AND is_read = false
    """;

        try (Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int countUnreadMessagesForUser(int userId){

        String sql =
                """
                SELECT COUNT(DISTINCT conversation_id)
                FROM message
                WHERE sender_id != ?
                AND is_read = false
                AND conversation_id IN
                (
                    SELECT id FROM conversation
                    WHERE user1_id = ?
                    OR user2_id = ?
                )
                """;

        try(Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement ps = cnx.prepareStatement(sql)){

            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt(1);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return 0;
    }




}