package com.nitk.meeting.service;

import com.nitk.meeting.model.MeetingBooking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CalendarClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${calendar.service.url:http://localhost:8084}")
    private String calendarServiceUrl;

    public String createProfessorEvent(String professorEmail, MeetingBooking booking) {
        System.out.println("📅 CalendarClient: Creating professor event for " + professorEmail);
        
        // Build description with student info
        StringBuilder description = new StringBuilder();
        if (booking.getDescription() != null && !booking.getDescription().isBlank()) {
            description.append(booking.getDescription()).append("\n\n");
        }
        description.append("--- ProfConnect Student Info ---\n");
        description.append("Student Name: ").append(booking.getStudentName() != null ? booking.getStudentName() : "N/A").append("\n");
        description.append("Student Email: ").append(booking.getStudentEmail() != null ? booking.getStudentEmail() : "N/A");
        
        Map<String, Object> body = new HashMap<>();
        body.put("summary", booking.getTitle() != null && !booking.getTitle().isBlank() ? booking.getTitle() : "Meeting");
        body.put("description", description.toString());
        body.put("start", booking.getStartIso());
        body.put("end", booking.getEndIso());
        if (booking.getLocation() != null && !booking.getLocation().isBlank()) {
            body.put("location", booking.getLocation());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = calendarServiceUrl + "/calendar-api/events?email=" + encode(professorEmail);
        try {
            System.out.println("📤 Sending POST request to: " + url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("id");
                String eventId = id != null ? id.toString() : null;
                System.out.println("✅ Professor event created successfully with ID: " + eventId);
                return eventId;
            } else {
                System.err.println("❌ Professor event creation failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating professor event: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String createStudentEvent(String studentEmail, MeetingBooking booking) {
        System.out.println("📅 CalendarClient: Creating student event for " + studentEmail);
        
        // Build description with professor info
        StringBuilder description = new StringBuilder();
        if (booking.getDescription() != null && !booking.getDescription().isBlank()) {
            description.append(booking.getDescription()).append("\n\n");
        }
        description.append("--- ProfConnect Meeting Info ---\n");
        description.append("Professor Name: ").append(booking.getProfessorName() != null ? booking.getProfessorName() : "N/A").append("\n");
        description.append("Professor Email: ").append(booking.getProfessorEmail() != null ? booking.getProfessorEmail() : "N/A");
        
        Map<String, Object> body = new HashMap<>();
        body.put("summary", booking.getTitle() != null && !booking.getTitle().isBlank() ? booking.getTitle() : "Meeting with Professor");
        body.put("description", description.toString());
        body.put("start", booking.getStartIso());
        body.put("end", booking.getEndIso());
        if (booking.getLocation() != null && !booking.getLocation().isBlank()) {
            body.put("location", booking.getLocation());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = calendarServiceUrl + "/calendar-api/events?email=" + encode(studentEmail);
        try {
            System.out.println("📤 Sending POST request to: " + url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("id");
                String eventId = id != null ? id.toString() : null;
                System.out.println("✅ Student event created successfully with ID: " + eventId);
                return eventId;
            } else {
                System.err.println("❌ Student event creation failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating student event: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void cancelProfessorEvent(String eventId, String professorEmail, String reason, String studentEmail) {
        if (eventId == null || eventId.isBlank()) {
            System.out.println("⚠️ CalendarClient: No professor event ID provided, skipping deletion");
            return;
        }
        System.out.println("🗑️ CalendarClient: Deleting professor event " + eventId + " for " + professorEmail);
        
        StringBuilder url = new StringBuilder(calendarServiceUrl)
                .append("/calendar-api/events/")
                .append(encode(eventId))
                .append("?email=")
                .append(encode(professorEmail));
        if (reason != null && !reason.isBlank()) {
            url.append("&reason=").append(encode(reason));
        }
        if (studentEmail != null && !studentEmail.isBlank()) {
            url.append("&studentEmail=").append(encode(studentEmail));
        }
        try {
            System.out.println("📤 Sending DELETE request to: " + url.toString());
            ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.DELETE, HttpEntity.EMPTY, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Professor event deleted successfully");
            } else {
                System.err.println("❌ Professor event deletion failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error cancelling professor event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void cancelStudentEvent(String eventId, String studentEmail, String reason) {
        if (eventId == null || eventId.isBlank()) {
            System.out.println("⚠️ CalendarClient: No student event ID provided, skipping deletion");
            return;
        }
        System.out.println("🗑️ CalendarClient: Deleting student event " + eventId + " for " + studentEmail);
        
        StringBuilder url = new StringBuilder(calendarServiceUrl)
                .append("/calendar-api/events/")
                .append(encode(eventId))
                .append("?email=")
                .append(encode(studentEmail));
        if (reason != null && !reason.isBlank()) {
            url.append("&reason=").append(encode(reason));
        }
        try {
            System.out.println("📤 Sending DELETE request to: " + url.toString());
            ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.DELETE, HttpEntity.EMPTY, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Student event deleted successfully");
            } else {
                System.err.println("❌ Student event deletion failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error cancelling student event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}


