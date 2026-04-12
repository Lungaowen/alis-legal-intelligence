package za.co.alis.dao;

import za.co.alis.model.SummaryReport;
import java.sql.*;

public class SummaryReportDAO {
    public void save(SummaryReport report) throws SQLException {
        String sql = "INSERT INTO summary_reports (document_id, user_id, rule_id, similarity_score, compliance_status, risk_flag, suggestion_text, report_text) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, report.getDocumentId());
            stmt.setInt(2, report.getUserId());
            stmt.setInt(3, report.getRuleId());
            stmt.setDouble(4, report.getSimilarityScore());
            stmt.setString(5, report.getComplianceStatus());
            stmt.setString(6, report.getRiskFlag());
            stmt.setString(7, report.getSuggestionText());
            stmt.setString(8, report.getReportText());
            stmt.executeUpdate();
        }
    }
}
