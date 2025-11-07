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

    @Value("${email.service.url:http://localhost:8086}")
    private String emailServiceUrl;

    public void sendBookingEmails(String professorEmail, String professorName, String studentEmail, 
                                 String studentName, String meetingTitle, String description, 
                                 String location, String startTime, String endTime) {
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

    public void sendCancellationEmails(String professorEmail, String professorName, String studentEmail, 
                                      String studentName, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("professorEmail", professorEmail);
        body.put("professorName", professorName);
        body.put("studentEmail", studentEmail);
        body.put("studentName", studentName);
        body.put("reason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

