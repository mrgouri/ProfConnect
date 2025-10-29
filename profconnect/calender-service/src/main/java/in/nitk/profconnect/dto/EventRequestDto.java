package com.nitk.calendar.dto;

public class EventRequestDto {
    private String summary;
    private String description;
    private String start; // ISO 8601
    private String end;   // ISO 8601

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
}
