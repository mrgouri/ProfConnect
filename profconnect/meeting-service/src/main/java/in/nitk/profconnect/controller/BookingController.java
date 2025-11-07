package com.nitk.meeting.controller;

import com.nitk.meeting.model.MeetingBooking;
import com.nitk.meeting.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/booking-api")
@CrossOrigin(origins = "http://localhost:3001")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> create(@RequestBody MeetingBooking booking) {
        try {
            MeetingBooking saved = bookingService.createBooking(booking);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<MeetingBooking> found = bookingService.getById(id);
        return found.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Booking not found")));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> list(@RequestParam(required = false) String studentEmail,
                                  @RequestParam(required = false) String professorEmail) {
        if (studentEmail != null && !studentEmail.isBlank()) {
            List<MeetingBooking> list = bookingService.listByStudent(studentEmail);
            return ResponseEntity.ok(list);
        }
        if (professorEmail != null && !professorEmail.isBlank()) {
            List<MeetingBooking> list = bookingService.listByProfessor(professorEmail);
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Provide studentEmail or professorEmail"));
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        boolean ok = bookingService.cancelBooking(id, reason);
        if (!ok) return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
        return ResponseEntity.ok(Map.of("message", "Booking cancelled"));
    }
}


