package za.co.alis.model;

public class Act {
    private int actId;
    private String actName;
    private String actNumber;
    private int actYear;
    private String actSection;
    private String description;
    private String jurisdiction;

    // Getters and setters (omitted for brevity)
    public int getActId() { return actId; }
    public void setActId(int actId) { this.actId = actId; }
    public String getActName() { return actName; }
    public void setActName(String actName) { this.actName = actName; }
    public String getActNumber() { return actNumber; }
    public void setActNumber(String actNumber) { this.actNumber = actNumber; }
    public int getActYear() { return actYear; }
    public void setActYear(int actYear) { this.actYear = actYear; }
    public String getActSection() { return actSection; }
    public void setActSection(String actSection) { this.actSection = actSection; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
}
