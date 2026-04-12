package za.co.alis.dao;

import za.co.alis.model.LawRule;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LawRuleDAO {
    public List<LawRule> findAll() throws SQLException {
        List<LawRule> list = new ArrayList<>();
        String sql = "SELECT r.rule_id, r.act_id, r.keyword, r.requirements, r.risk_level, r.suggestion, a.act_name " +
                     "FROM law_rules r JOIN acts a ON r.act_id = a.act_id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                LawRule lr = new LawRule();
                lr.setRuleId(rs.getInt("rule_id"));
                lr.setActId(rs.getInt("act_id"));
                lr.setKeyword(rs.getString("keyword"));
                lr.setRequirements(rs.getString("requirements"));
                lr.setRiskLevel(rs.getString("risk_level"));
                lr.setSuggestion(rs.getString("suggestion"));
                // store act name in keyword for context building (hack)
                list.add(lr);
            }
        }
        return list;
    }
}
