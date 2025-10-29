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
import java.util.Map;

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
        boolean linked = service.isCalendarLinked(email);
        return ResponseEntity.ok(Map.of("connected", linked));
    }


    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl(@RequestParam String email) throws Exception {
        return ResponseEntity.ok(java.util.Map.of("url", service.getAuthUrl(email)));
    }

    @GetMapping("/oauth2/callback")
    public void oauthCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws Exception {
        service.handleCallback(code, state);
        response.sendRedirect("http://localhost:3001/prof-calendar.html?linked=true");
    }


    @GetMapping("/events")
    public ResponseEntity<?> listEvents(@RequestParam String email, @RequestParam(defaultValue = "10") int max) throws Exception {
        List<Map<String, String>> events = service.listEvents(email, max);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestParam String email, @RequestBody EventRequestDto body) throws Exception {
        Event event = new Event();
        event.setSummary(body.getSummary() != null ? body.getSummary() : "Meeting");
        event.setDescription(body.getDescription());
        event.setStart(new com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(body.getStart())));
        event.setEnd(new com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(body.getEnd())));

        Event created = service.createEvent(email, event);
        if (created == null) return ResponseEntity.status(404).body(java.util.Map.of("message", "No calendar linked for " + email));
        return ResponseEntity.ok(created);
    }
}
