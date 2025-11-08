package com.nitk.calendar.controller;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.nitk.calendar.dto.EventRequestDto;
import com.nitk.calendar.service.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/calendar-api")
@CrossOrigin(origins = "http://localhost:3001")
public class CalendarController {

    private final GoogleCalendarService service;

    public CalendarController(GoogleCalendarService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<?> checkCalendarStatus(@RequestParam String email) {
        try {
            String decoded = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8);
            boolean linked = service.isCalendarLinked(decoded);
            return ResponseEntity.ok(Map.of("connected", linked));
        } catch (Exception e) {
            boolean linked = service.isCalendarLinked(email);
            return ResponseEntity.ok(Map.of("connected", linked));
        }
    }


    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl(@RequestParam String email) throws Exception {
        return ResponseEntity.ok(java.util.Map.of("url", service.getAuthUrl(email)));
    }

    @GetMapping("/oauth2/callback")
    public void oauthCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws Exception {
        // state contains the email
        service.handleCallback(code, state);
        response.sendRedirect("http://localhost:3001/prof-calendar.html?linked=true");
    }


    @GetMapping("/events")
    public ResponseEntity<?> listEvents(@RequestParam String email, @RequestParam(defaultValue = "10") int max) {
        try {
            // decode percent-encoded email if present and validate
            String decodedEmail = email;
            try { decodedEmail = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ex) { }
            if (decodedEmail == null || decodedEmail.isBlank()) {
                System.err.println("❌ Missing or empty email parameter for listEvents");
                return ResponseEntity.badRequest().body(Map.of("error", "email query parameter is required"));
            }

            List<Map<String, Object>> events = service.listEvents(decodedEmail, max);
            if (events == null || events.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            System.err.println("❌ Error fetching events for " + email + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(404).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Failed to fetch events",
                "message", e.getMessage() != null ? e.getMessage() : "Failed to fetch events"
            ));
        }
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestParam String email, @RequestBody EventRequestDto body) {
        try {
            // decode percent-encoded emails if present (incoming callers may encode the query param)
            String decodedEmail = email;
            try { decodedEmail = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ex) { /* ignore */ }
            System.out.println("📥 Received event creation request for: " + email + " (decoded: " + decodedEmail + ")");
            System.out.println("   Summary: " + body.getSummary());
            System.out.println("   Start: " + body.getStart());
            System.out.println("   End: " + body.getEnd());
            
            // Check if calendar is linked first (use decoded email)
            if (!service.isCalendarLinked(decodedEmail)) {
                System.err.println("❌ Calendar not linked for: " + decodedEmail);
                return ResponseEntity.status(404).body(java.util.Map.of(
                    "error", "Calendar not linked",
                    "message", "No calendar linked for " + decodedEmail + ". Please connect your Google Calendar first."
                ));
            }
            
            Event event = new Event();
            event.setSummary(body.getSummary() != null ? body.getSummary() : "Meeting");
            event.setDescription(body.getDescription());
            
            // Parse and set start time
            try {
                com.google.api.client.util.DateTime startDateTime = new com.google.api.client.util.DateTime(body.getStart());
                event.setStart(new com.google.api.services.calendar.model.EventDateTime()
                        .setDateTime(startDateTime));
                System.out.println("✅ Start time parsed: " + startDateTime.toStringRfc3339());
            } catch (Exception e) {
                System.err.println("❌ Error parsing start time: " + body.getStart() + " - " + e.getMessage());
                return ResponseEntity.status(400).body(java.util.Map.of(
                    "error", "Invalid start time format",
                    "message", "Failed to parse start time: " + body.getStart()
                ));
            }
            
            // Parse and set end time
            try {
                com.google.api.client.util.DateTime endDateTime = new com.google.api.client.util.DateTime(body.getEnd());
                event.setEnd(new com.google.api.services.calendar.model.EventDateTime()
                        .setDateTime(endDateTime));
                System.out.println("✅ End time parsed: " + endDateTime.toStringRfc3339());
            } catch (Exception e) {
                System.err.println("❌ Error parsing end time: " + body.getEnd() + " - " + e.getMessage());
                return ResponseEntity.status(400).body(java.util.Map.of(
                    "error", "Invalid end time format",
                    "message", "Failed to parse end time: " + body.getEnd()
                ));
            }
            
            if (body.getLocation() != null && !body.getLocation().isBlank()) {
                event.setLocation(body.getLocation());
            }

            Event created = service.createEvent(decodedEmail, event);
            if (created == null || created.getId() == null) {
                System.err.println("❌ Event creation returned null or no ID");
                return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Event creation failed",
                    "message", "Failed to create event in Google Calendar"
                ));
            }
            
            System.out.println("✅ Event created successfully with ID: " + created.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("❌ Error in createEvent endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Failed to create event",
                "message", e.getMessage() != null ? e.getMessage() : "Failed to create event in Google Calendar"
            ));
        }
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable String eventId,
            @RequestParam String email,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String studentEmail) {
        try {
            String decodedEmail = email;
            try { decodedEmail = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ex) { }
            boolean deleted = service.deleteEvent(decodedEmail, eventId, reason);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
            } else {
                return ResponseEntity.status(404).body(Map.of("message", "Event not found or could not be deleted"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting event " + eventId + " for " + email + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Failed to delete event",
                "message", e.getMessage() != null ? e.getMessage() : "Failed to delete event"
            ));
        }
    }
}