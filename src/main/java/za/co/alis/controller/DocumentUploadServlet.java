package za.co.alis.controller;

import com.google.gson.*;
import za.co.alis.dao.*;
import za.co.alis.model.*;
import za.co.alis.util.OllamaClient;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@MultipartConfig
public class DocumentUploadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("login.jsp");
            return;
        }
        User user = (User) session.getAttribute("user");

        Part filePart = req.getPart("document");
        String fileName = filePart.getSubmittedFileName();
        String content = new String(filePart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Save document with status 'processing'
        Document doc = new Document();
        doc.setUserId(user.getUserId());
        doc.setFilename(fileName);
        doc.setContent(content);
        doc.setDocumentHash(OllamaClient.computeSHA256(content));
        doc.setStatus("processing");
        doc.setIngestionSource("manual");

        DocumentDAO docDAO = new DocumentDAO();
        int docId;
        try {
            docId = docDAO.save(doc);
            doc.setDocumentId(docId);
        } catch (SQLException e) {
            throw new ServletException("Failed to save document", e);
        }

        // Build law rules context
        StringBuilder rulesCtx = new StringBuilder();
        LawRuleDAO ruleDAO = new LawRuleDAO();
        List<LawRule> rules;
        try {
            rules = ruleDAO.findAll();
            for (LawRule r : rules) {
                rulesCtx.append("- Act ID ").append(r.getActId())
                        .append(", Keyword: ").append(r.getKeyword())
                        .append(", Requirements: ").append(r.getRequirements())
                        .append(", Risk: ").append(r.getRiskLevel())
                        .append(", Suggestion: ").append(r.getSuggestion()).append("\n");
            }
        } catch (SQLException e) {
            try { docDAO.updateStatus(docId, "error"); } catch (SQLException ignored) {}
            throw new ServletException("Failed to load law rules", e);
        }

        // Call Ollama
        OllamaClient ollama = new OllamaClient();
        String aiJson;
        try {
            aiJson = ollama.analyzeDocument(content, rulesCtx.toString());
        } catch (Exception e) {
            try { docDAO.updateStatus(docId, "error"); } catch (SQLException ignored) {}
            throw new ServletException("AI analysis failed", e);
        }

        // Parse JSON
        JsonObject result = JsonParser.parseString(aiJson).getAsJsonObject();
        JsonArray clausesArr = result.getAsJsonArray("clauses");

        // Save clauses and summary reports
        ClauseDAO clauseDAO = new ClauseDAO();
        SummaryReportDAO reportDAO = new SummaryReportDAO();
        LawRule defaultRule = rules.isEmpty() ? null : rules.get(0);

        try {
            for (JsonElement elem : clausesArr) {
                JsonObject cObj = elem.getAsJsonObject();
                Clause clause = new Clause();
                clause.setDocumentId(docId);
                clause.setClauseText(cObj.get("clause_text").getAsString());
                clause.setRiskLevel(cObj.get("risk_level").getAsString());
                int clauseId = clauseDAO.save(clause);

                if (defaultRule != null) {
                    SummaryReport report = new SummaryReport();
                    report.setDocumentId(docId);
                    report.setUserId(user.getUserId());
                    report.setRuleId(defaultRule.getRuleId());
                    report.setSimilarityScore(85.0);
                    report.setComplianceStatus(cObj.get("risk_level").getAsString().equals("Low") ? "Compliant" : "Non-compliant");
                    report.setRiskFlag(cObj.get("risk_level").getAsString());
                    report.setSuggestionText(cObj.get("suggestion").getAsString());
                    report.setReportText("AI analysis for clause: " + cObj.get("clause_text").getAsString());
                    reportDAO.save(report);
                }
            }
            docDAO.updateStatus(docId, "active");
        } catch (SQLException e) {
            try { docDAO.updateStatus(docId, "error"); } catch (SQLException ignored) {}
            throw new ServletException("Failed to save analysis results", e);
        }

        resp.sendRedirect("analysis?docId=" + docId);
    }
}