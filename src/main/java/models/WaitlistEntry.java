package models;

import java.time.LocalDateTime;

public class WaitlistEntry {
    private int id;
    private int activiteId;
    private int personneId;
    private String status; // WAITING, HOLD, CONFIRMED, EXPIRED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime holdExpiresAt;
    private String holdToken;

    public WaitlistEntry() {}

    public WaitlistEntry(int id, int activiteId, int personneId, String status,
                         LocalDateTime createdAt, LocalDateTime holdExpiresAt, String holdToken) {
        this.id = id;
        this.activiteId = activiteId;
        this.personneId = personneId;
        this.status = status;
        this.createdAt = createdAt;
        this.holdExpiresAt = holdExpiresAt;
        this.holdToken = holdToken;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActiviteId() { return activiteId; }
    public void setActiviteId(int activiteId) { this.activiteId = activiteId; }

    public int getPersonneId() { return personneId; }
    public void setPersonneId(int personneId) { this.personneId = personneId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }
}