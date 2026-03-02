package services;

import models.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationService {

    private final String URL = "jdbc:mysql://localhost:3306/rehlaPI?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "";

    public Conversation getConversationBetweenUsers(int user1, int user2){

        String sql =
                """
                SELECT * FROM conversation
                WHERE (user1_id=? AND user2_id=?)
                OR (user1_id=? AND user2_id=?)
                """;

        try(Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement ps = cnx.prepareStatement(sql)){

            ps.setInt(1,user1);
            ps.setInt(2,user2);
            ps.setInt(3,user2);
            ps.setInt(4,user1);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                Conversation c = new Conversation();
                c.setId(rs.getInt("id"));
                c.setUser1Id(rs.getInt("user1_id"));
                c.setUser2Id(rs.getInt("user2_id"));

                return c;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
    public List<Conversation> getAllPossibleConversations(int userId){

        List<Conversation> list = new ArrayList<>();

        String sql = "SELECT id FROM personne WHERE id <> ?";

        try(Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement ps = cnx.prepareStatement(sql)){

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                int otherUserId = rs.getInt("id");

                // chercher si une conversation existe déjà
                Conversation existing =
                        getConversationBetweenUsers(userId, otherUserId);

                if(existing != null){

                    list.add(existing);

                }else{

                    Conversation conv = new Conversation();
                    conv.setUser1Id(userId);
                    conv.setUser2Id(otherUserId);

                    conv.setId(-1); // conversation virtuelle
                    list.add(conv);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return list;
    }
    public Conversation createConversation(int user1,int user2){

        String sql =
                "INSERT INTO conversation(user1_id,user2_id) VALUES(?,?)";

        try(Connection cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement ps =
                    cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){

            ps.setInt(1,user1);
            ps.setInt(2,user2);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if(rs.next()){
                Conversation c = new Conversation();
                c.setId(rs.getInt(1));
                c.setUser1Id(user1);
                c.setUser2Id(user2);
                return c;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

}