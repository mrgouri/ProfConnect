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

    @Value("${calendar.service.url:http://calendar-service:8084}")
    private String calendarServiceUrl;

    /** Create event for professor */
    public String createProfessorEvent(String professorEmail, MeetingBooking booking, String authHeader) {
        System.out.println("📅 CalendarClient: Creating professor event for " + professorEmail);

        StringBuilder description = new StringBuilder();
        if (booking.getDescription() != null && !booking.getDescription().isBlank()) {
            description.append(booking.getDescription()).append("\n\n");
        }
        description.append("--- ProfConnect Student Info ---\n")
                .append("Student Name: ").append(booking.getStudentName() != null ? booking.getStudentName() : "N/A").append("\n")
                .append("Student Email: ").append(booking.getStudentEmail() != null ? booking.getStudentEmail() : "N/A");

        Map<String, Object> body = new HashMap<>();
        body.put("summary", booking.getTitle() != null && !booking.getTitle().isBlank() ? booking.getTitle() : "Meeting");
        body.put("description", description.toString());
        body.put("start", booking.getStartIso());
        body.put("end", booking.getEndIso());
        if (booking.getLocation() != null && !booking.getLocation().isBlank()) {
            body.put("location", booking.getLocation());
        }

        HttpEntity<Map<String, Object>> request = buildAuthorizedRequest(body, authHeader);
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

    /** Create event for student */
    public String createStudentEvent(String studentEmail, MeetingBooking booking, String authHeader) {
        System.out.println("📅 CalendarClient: Creating student event for " + studentEmail);

        StringBuilder description = new StringBuilder();
        if (booking.getDescription() != null && !booking.getDescription().isBlank()) {
            description.append(booking.getDescription()).append("\n\n");
        }
        description.append("--- ProfConnect Meeting Info ---\n")
                .append("Professor Name: ").append(booking.getProfessorName() != null ? booking.getProfessorName() : "N/A").append("\n")
                .append("Professor Email: ").append(booking.getProfessorEmail() != null ? booking.getProfessorEmail() : "N/A");

        Map<String, Object> body = new HashMap<>();
        body.put("summary", booking.getTitle() != null && !booking.getTitle().isBlank() ? booking.getTitle() : "Meeting with Professor");
        body.put("description", description.toString());
        body.put("start", booking.getStartIso());
        body.put("end", booking.getEndIso());
        if (booking.getLocation() != null && !booking.getLocation().isBlank()) {
            body.put("location", booking.getLocation());
        }

        HttpEntity<Map<String, Object>> request = buildAuthorizedRequest(body, authHeader);
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

    /** Cancel professor event */
    public void cancelProfessorEvent(String eventId, String professorEmail, String reason, String studentEmail, String authHeader) {
        if (eventId == null || eventId.isBlank()) {
            System.out.println("⚠️ CalendarClient: No professor event ID provided, skipping deletion");
            return;
        }

        StringBuilder url = new StringBuilder(calendarServiceUrl)
                .append("/calendar-api/events/")
                .append(encode(eventId))
                .append("?email=").append(encode(professorEmail));
        if (reason != null && !reason.isBlank()) url.append("&reason=").append(encode(reason));
        if (studentEmail != null && !studentEmail.isBlank()) url.append("&studentEmail=").append(encode(studentEmail));

        try {
            System.out.println("🗑️ Deleting professor event: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.DELETE,
                    buildAuthorizedRequest(null, authHeader),
                    Map.class
            );
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

    /** Cancel student event */
    public void cancelStudentEvent(String eventId, String studentEmail, String reason, String authHeader) {
        if (eventId == null || eventId.isBlank()) {
            System.out.println("⚠️ CalendarClient: No student event ID provided, skipping deletion");
            return;
        }

        StringBuilder url = new StringBuilder(calendarServiceUrl)
                .append("/calendar-api/events/")
                .append(encode(eventId))
                .append("?email=").append(encode(studentEmail));
        if (reason != null && !reason.isBlank()) url.append("&reason=").append(encode(reason));

        try {
            System.out.println("🗑️ Deleting student event: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.DELETE,
                    buildAuthorizedRequest(null, authHeader),
                    Map.class
            );
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

    /** Helper: build authorized HttpEntity */
    private HttpEntity<Map<String, Object>> buildAuthorizedRequest(Map<String, Object> body, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isBlank()) {
            headers.add("Authorization", authHeader);
        }
        return new HttpEntity<>(body, headers);
    }

    /** Helper: safe URL encoding */
    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
