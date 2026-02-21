package models;

import java.time.LocalDateTime;

public class notification {

    private int id;

    private String message;


    private String type;

    private Integer postId;        // nullable
    private Integer commentId;     // nullable

    private int senderId;          // auteur contenu suspect
    private int receiverId;        // admin

    private boolean isRead;

    private LocalDateTime createdAt;

    // ================= CONSTRUCTORS =================

    public notification() {
    }

    // Pour insertion
    public notification(String message, String type,
                        Integer postId, Integer commentId,
                        int senderId, int receiverId) {
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.isRead = false;
    }


    public notification(int id, String message, String type,
                        Integer postId, Integer commentId,
                        int senderId, int receiverId,
                        boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // ================= GETTERS & SETTERS =================

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public Integer getPostId() {
        return postId;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setId(int id) {
        this.id = id;
    }
}