package models;

import java.time.LocalDateTime;

public class notification {

    private int id;

    private String message;
    private String type;

    private Integer postId;
    private Integer commentId;
    private Integer activiteId;

    private int senderId;
    private int receiverId;

    private boolean isRead;


    private boolean isSentSms;

    private LocalDateTime createdAt;

    // ================= CONSTRUCTORS =================

    public notification() {}

    // 👉 Pour insertion simple
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
        this.isSentSms = false; // 🔥 important
    }

    // 👉 Constructeur complet
    public notification(int id, String message, String type,
                        Integer postId, Integer commentId, Integer activiteId,
                        int senderId, int receiverId,
                        boolean isRead, boolean isSentSms,
                        LocalDateTime createdAt) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.activiteId = activiteId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.isRead = isRead;
        this.isSentSms = isSentSms;
        this.createdAt = createdAt;
    }

    // ================= GETTERS =================

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

    public Integer getActiviteId() {
        return activiteId;
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

    public boolean isSentSms() {
        return isSentSms;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ================= SETTERS =================

    public void setId(int id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public void setActiviteId(Integer activiteId) {
        this.activiteId = activiteId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setSentSms(boolean sentSms) {
        isSentSms = sentSms;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}