package za.co.alis.model;

import java.sql.Timestamp;

public class Document {
    private int documentId;
    private int userId;
    private String filename;
    private String content;
    private String documentHash;
    private String status;
    private String ingestionSource;
    private Timestamp uploadedAt;

    // Getters and setters
    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIngestionSource() { return ingestionSource; }
    public void setIngestionSource(String ingestionSource) { this.ingestionSource = ingestionSource; }
    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
}
