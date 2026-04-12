package za.co.alis.dao;

import za.co.alis.model.Clause;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClauseDAO {
    public int save(Clause clause) throws SQLException {
        String sql = "INSERT INTO clauses (document_id, clause_text, risk_level) VALUES (?, ?, ?) RETURNING clause_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clause.getDocumentId());
            stmt.setString(2, clause.getClauseText());
            stmt.setString(3, clause.getRiskLevel());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public List<Clause> findByDocument(int docId) throws SQLException {
        List<Clause> list = new ArrayList<>();
        String sql = "SELECT * FROM clauses WHERE document_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, docId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Clause c = new Clause();
                c.setClauseId(rs.getInt("clause_id"));
                c.setDocumentId(rs.getInt("document_id"));
                c.setClauseText(rs.getString("clause_text"));
                c.setRiskLevel(rs.getString("risk_level"));
                list.add(c);
            }
        }
        return list;
    }
}
