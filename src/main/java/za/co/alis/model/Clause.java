package za.co.alis.model;

public class Clause {
    private int clauseId;
    private int documentId;
    private String clauseText;
    private String riskLevel;

    // Getters and setters
    public int getClauseId() { return clauseId; }
    public void setClauseId(int clauseId) { this.clauseId = clauseId; }
    public int getDocumentId() { return documentId; }
    public void setDocumentId(int documentId) { this.documentId = documentId; }
    public String getClauseText() { return clauseText; }
    public void setClauseText(String clauseText) { this.clauseText = clauseText; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}
