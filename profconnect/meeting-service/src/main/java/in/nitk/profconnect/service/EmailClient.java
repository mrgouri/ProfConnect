package com.nitk.meeting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // default points to docker service hostname (not localhost)
    @Value("${email.service.url:http://email-service:8086}")
    private String emailServiceUrl;

    // Backwards-compatible wrapper (no auth)
    public void sendBookingEmails(String professorEmail, String professorName, String studentEmail,
                                 String studentName, String meetingTitle, String description,
                                 String location, String startTime, String endTime) {
        sendBookingEmails(professorEmail, professorName, studentEmail, studentName,
                meetingTitle, description, location, startTime, endTime, null);
    }

    // New signature that accepts auth header
    public void sendBookingEmails(String professorEmail, String professorName, String studentEmail,
                                 String studentName, String meetingTitle, String description,
                                 String location, String startTime, String endTime,
                                 String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("professorEmail", professorEmail);
        body.put("professorName", professorName);
        body.put("studentEmail", studentEmail);
        body.put("studentName", studentName);
        body.put("meetingTitle", meetingTitle);
        body.put("description", description);
        body.put("location", location);
        body.put("startTime", startTime);
        body.put("endTime", endTime);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isBlank()) {
            headers.add("Authorization", authHeader);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = emailServiceUrl + "/email-api/booking";
        try {
            System.out.println("📧 Sending booking email request to: " + url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Booking emails sent successfully via email service");
            } else {
                System.err.println("❌ Booking email request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error calling email service for booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Backwards-compatible wrapper (no auth)
    public void sendCancellationEmails(String professorEmail, String professorName, String studentEmail,
                                       String studentName, String reason) {
        sendCancellationEmails(professorEmail, professorName, studentEmail, studentName, reason, null);
    }

    // New signature that accepts auth header
    public void sendCancellationEmails(String professorEmail, String professorName, String studentEmail,
                                       String studentName, String reason, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("professorEmail", professorEmail);
        body.put("professorName", professorName);
        body.put("studentEmail", studentEmail);
        body.put("studentName", studentName);
        body.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isBlank()) {
            headers.add("Authorization", authHeader);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = emailServiceUrl + "/email-api/cancellation";
        try {
            System.out.println("📧 Sending cancellation email request to: " + url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Cancellation emails sent successfully via email service");
            } else {
                System.err.println("❌ Cancellation email request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error calling email service for cancellation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
