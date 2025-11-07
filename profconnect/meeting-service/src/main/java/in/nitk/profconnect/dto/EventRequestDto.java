package com.nitk.meeting.dto;

public class EventRequestDto {
    private String summary;
    private String description;
    private String start; // ISO 8601
    private String end;   // ISO 8601
    private String location;
    private String studentEmail;
    private String studentName;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
