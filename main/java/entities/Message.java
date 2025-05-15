package entities;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int idSender;
    private int idReceiver;
    private String content;
    private LocalDateTime timestamp;
    private boolean isRead;
    private String senderName;

    public Message(int idSender, int idReceiver, String content) {
        this.idSender = idSender;
        this.idReceiver = idReceiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    public Message(int id, int idSender, int idReceiver, String content, LocalDateTime timestamp, boolean isRead) {
        this.id = id;
        this.idSender = idSender;
        this.idReceiver = idReceiver;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }
    
    public Message(int idSender, int idReceiver, String content, LocalDateTime timestamp, boolean isRead) {
        this.idSender = idSender;
        this.idReceiver = idReceiver;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdSender() { return idSender; }
    public void setIdSender(int idSender) { this.idSender = idSender; }
    public int getIdReceiver() { return idReceiver; }
    public void setIdReceiver(int idReceiver) { this.idReceiver = idReceiver; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", idSender=" + idSender +
                ", idReceiver=" + idReceiver +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                '}';
    }
}
