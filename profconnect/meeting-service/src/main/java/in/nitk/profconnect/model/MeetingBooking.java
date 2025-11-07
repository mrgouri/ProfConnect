package com.nitk.meeting.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "booking_meetings")
public class MeetingBooking {

    @Id
    private String id;

    @Indexed
    private String professorEmail;
    private String professorName;

    @Indexed
    private String studentEmail;
    private String studentName;

    private String title;
    private String description;
    private String location;

    // ISO-8601 strings (UTC) for simplicity with frontend and email service
    private String startIso;
    private String endIso;

    // Optional Google Calendar event ids
    private String professorCalendarEventId;
    private String studentCalendarEventId;

    @Indexed
    private String status; // BOOKED, CANCELLED

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProfessorEmail() { return professorEmail; }
    public void setProfessorEmail(String professorEmail) { this.professorEmail = professorEmail; }

    public String getProfessorName() { return professorName; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStartIso() { return startIso; }
    public void setStartIso(String startIso) { this.startIso = startIso; }

    public String getEndIso() { return endIso; }
    public void setEndIso(String endIso) { this.endIso = endIso; }

    public String getProfessorCalendarEventId() { return professorCalendarEventId; }
    public void setProfessorCalendarEventId(String professorCalendarEventId) { this.professorCalendarEventId = professorCalendarEventId; }

    public String getStudentCalendarEventId() { return studentCalendarEventId; }
    public void setStudentCalendarEventId(String studentCalendarEventId) { this.studentCalendarEventId = studentCalendarEventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


