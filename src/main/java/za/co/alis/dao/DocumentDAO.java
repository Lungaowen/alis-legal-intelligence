package za.co.alis.dao;

import za.co.alis.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {
    public int save(Document doc) throws SQLException {
        String sql = "INSERT INTO documents (user_id, filename, content, document_hash, status, ingestion_source) VALUES (?, ?, ?, ?, ?, ?) RETURNING document_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doc.getUserId());
            stmt.setString(2, doc.getFilename());
            stmt.setString(3, doc.getContent());
            stmt.setString(4, doc.getDocumentHash());
            stmt.setString(5, doc.getStatus());
            stmt.setString(6, doc.getIngestionSource());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public Document findById(int docId) throws SQLException {
        String sql = "SELECT * FROM documents WHERE document_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, docId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Document d = new Document();
                d.setDocumentId(rs.getInt("document_id"));
                d.setUserId(rs.getInt("user_id"));
                d.setFilename(rs.getString("filename"));
                d.setContent(rs.getString("content"));
                d.setDocumentHash(rs.getString("document_hash"));
                d.setStatus(rs.getString("status"));
                d.setIngestionSource(rs.getString("ingestion_source"));
                d.setUploadedAt(rs.getTimestamp("uploaded_at"));
                return d;
            }
        }
        return null;
    }

    public List<Document> findByUser(int userId) throws SQLException {
        List<Document> list = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE user_id = ? ORDER BY uploaded_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Document d = new Document();
                d.setDocumentId(rs.getInt("document_id"));
                d.setUserId(rs.getInt("user_id"));
                d.setFilename(rs.getString("filename"));
                d.setStatus(rs.getString("status"));
                d.setUploadedAt(rs.getTimestamp("uploaded_at"));
                list.add(d);
            }
        }
        return list;
    }

    public void updateStatus(int docId, String status) throws SQLException {
        String sql = "UPDATE documents SET status = ? WHERE document_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, docId);
            stmt.executeUpdate();
        }
    }
}
