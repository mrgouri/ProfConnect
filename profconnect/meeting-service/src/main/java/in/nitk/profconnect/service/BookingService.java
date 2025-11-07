package com.nitk.meeting.service;

import com.nitk.meeting.model.MeetingBooking;
import com.nitk.meeting.repository.MeetingBookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingService {

    private final MeetingBookingRepository repository;
    private final EmailClient emailClient;
    private final CalendarClient calendarClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${profile.service.url:http://localhost:8083}")
    private String profileServiceUrl;

    @Value("${crud.service.url:http://localhost:8081}")
    private String crudServiceUrl;

    public BookingService(MeetingBookingRepository repository, EmailClient emailClient, CalendarClient calendarClient) {
        this.repository = repository;
        this.emailClient = emailClient;
        this.calendarClient = calendarClient;
    }
    
    /**
     * Fetch user name from profile service or CRUD service by email
     */
    private String fetchUserName(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        
        try {
            // Try profile service first
            try {
                String url = profileServiceUrl + "/profiles/by-email?email=" + 
                    java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                var response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    if (body.containsKey("user")) {
                        Map<String, Object> user = (Map<String, Object>) body.get("user");
                        String name = (String) user.get("name");
                        if (StringUtils.hasText(name)) {
                            System.out.println("✅ Fetched user name from profile service: " + name);
                            return name;
                        }
                        // Try firstName + lastName
                        String firstName = (String) user.get("firstName");
                        String lastName = (String) user.get("lastName");
                        if (StringUtils.hasText(firstName)) {
                            String fullName = firstName + (StringUtils.hasText(lastName) ? " " + lastName : "");
                            System.out.println("✅ Fetched user name from profile service (firstName+lastName): " + fullName);
                            return fullName;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Profile service unavailable, trying CRUD service: " + e.getMessage());
            }
            
            // Fallback to CRUD service
            try {
                String url = crudServiceUrl + "/admin-api/users/by-email?email=" + 
                    java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                var response = restTemplate.getForEntity(url, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    if (body.containsKey("user")) {
                        Map<String, Object> user = (Map<String, Object>) body.get("user");
                        String name = (String) user.get("name");
                        if (StringUtils.hasText(name)) {
                            System.out.println("✅ Fetched user name from CRUD service: " + name);
                            return name;
                        }
                        // Try firstName + lastName
                        String firstName = (String) user.get("firstName");
                        String lastName = (String) user.get("lastName");
                        if (StringUtils.hasText(firstName)) {
                            String fullName = firstName + (StringUtils.hasText(lastName) ? " " + lastName : "");
                            System.out.println("✅ Fetched user name from CRUD service (firstName+lastName): " + fullName);
                            return fullName;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ CRUD service unavailable: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching user name for " + email + ": " + e.getMessage());
        }
        
        return "";
    }

    public MeetingBooking createBooking(MeetingBooking booking) {
        if (!StringUtils.hasText(booking.getProfessorEmail()) || !StringUtils.hasText(booking.getStudentEmail())) {
            throw new IllegalArgumentException("professorEmail and studentEmail are required");
        }
        
        // Fetch student name from profile if not provided or empty
        if (!StringUtils.hasText(booking.getStudentName())) {
            String fetchedName = fetchUserName(booking.getStudentEmail());
            if (StringUtils.hasText(fetchedName)) {
                booking.setStudentName(fetchedName);
                System.out.println("✅ Set student name from profile: " + fetchedName);
            } else {
                System.out.println("⚠️ Could not fetch student name for: " + booking.getStudentEmail());
            }
        }
        
        // Fetch professor name from profile if not provided or empty
        if (!StringUtils.hasText(booking.getProfessorName())) {
            String fetchedName = fetchUserName(booking.getProfessorEmail());
            if (StringUtils.hasText(fetchedName)) {
                booking.setProfessorName(fetchedName);
                System.out.println("✅ Set professor name from profile: " + fetchedName);
            }
        }
        
        // Log what's being saved to database
        System.out.println("📝 Saving booking to database:");
        System.out.println("   - Professor Email: " + booking.getProfessorEmail());
        System.out.println("   - Professor Name: " + (StringUtils.hasText(booking.getProfessorName()) ? booking.getProfessorName() : "NOT SET"));
        System.out.println("   - Student Email: " + booking.getStudentEmail());
        System.out.println("   - Student Name: " + (StringUtils.hasText(booking.getStudentName()) ? booking.getStudentName() : "NOT SET"));
        System.out.println("   - Title: " + booking.getTitle());
        System.out.println("   - Start: " + booking.getStartIso());
        System.out.println("   - End: " + booking.getEndIso());
        
        booking.setStatus("BOOKED");
        booking.setCreatedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        MeetingBooking saved = repository.save(booking);
        
        System.out.println("✅ Booking saved with ID: " + saved.getId());

        // Send email notifications to both professor and student via email microservice
        try {
            System.out.println("📧 Sending booking emails via email service");
            emailClient.sendBookingEmails(
                    booking.getProfessorEmail(),
                    booking.getProfessorName(),
                    booking.getStudentEmail(),
                    booking.getStudentName(),
                    StringUtils.hasText(booking.getTitle()) ? booking.getTitle() : "Meeting",
                    booking.getDescription(),
                    booking.getLocation(),
                    booking.getStartIso(),
                    booking.getEndIso()
            );
            System.out.println("✅ Booking emails sent successfully to both professor and student");
        } catch (Exception e) {
            System.err.println("❌ Error sending booking emails: " + e.getMessage());
            e.printStackTrace();
        }

        // Create event in calendar-service for the professor and store event id
        try {
            System.out.println("📅 Creating calendar event for professor: " + booking.getProfessorEmail());
            String professorEventId = calendarClient.createProfessorEvent(booking.getProfessorEmail(), booking);
            if (professorEventId != null && !professorEventId.isBlank()) {
                saved.setProfessorCalendarEventId(professorEventId);
                saved = repository.save(saved);
                System.out.println("✅ Professor calendar event created with ID: " + professorEventId);
            } else {
                System.out.println("⚠️ Professor calendar event creation returned null/empty ID");
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating professor calendar event: " + e.getMessage());
            e.printStackTrace();
        }

        // Create event in calendar-service for the student and store event id
        try {
            System.out.println("📅 Creating calendar event for student: " + booking.getStudentEmail());
            String studentEventId = calendarClient.createStudentEvent(booking.getStudentEmail(), booking);
            if (studentEventId != null && !studentEventId.isBlank()) {
                saved.setStudentCalendarEventId(studentEventId);
                saved = repository.save(saved);
                System.out.println("✅ Student calendar event created with ID: " + studentEventId);
            } else {
                System.out.println("⚠️ Student calendar event creation returned null/empty ID");
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating student calendar event: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("📝 Final booking state - Professor Event ID: " + saved.getProfessorCalendarEventId() + 
                          ", Student Event ID: " + saved.getStudentCalendarEventId());
        return saved;
    }

    public Optional<MeetingBooking> getById(String id) {
        return repository.findById(id);
    }

    public List<MeetingBooking> listByStudent(String studentEmail) {
        return repository.findByStudentEmailOrderByStartIsoDesc(studentEmail);
    }

    public List<MeetingBooking> listByProfessor(String professorEmail) {
        return repository.findByProfessorEmailOrderByStartIsoDesc(professorEmail);
    }

    public boolean cancelBooking(String id, String reason) {
        Optional<MeetingBooking> opt = repository.findById(id);
        if (opt.isEmpty()) return false;
        MeetingBooking booking = opt.get();
        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(Instant.now());
        repository.save(booking);
        
        // Send cancellation emails to both professor and student via email microservice
        try {
            System.out.println("📧 Sending cancellation emails via email service");
            emailClient.sendCancellationEmails(
                    booking.getProfessorEmail(),
                    booking.getProfessorName(),
                    booking.getStudentEmail(),
                    booking.getStudentName(),
                    reason
            );
            System.out.println("✅ Cancellation emails sent successfully to both professor and student");
        } catch (Exception e) {
            System.err.println("❌ Error sending cancellation emails: " + e.getMessage());
            e.printStackTrace();
        }
        // Cancel event in calendar-service for professor if it exists
        try {
            if (booking.getProfessorCalendarEventId() != null && !booking.getProfessorCalendarEventId().isBlank()) {
                System.out.println("🗑️ Deleting professor calendar event: " + booking.getProfessorCalendarEventId());
                calendarClient.cancelProfessorEvent(booking.getProfessorCalendarEventId(), booking.getProfessorEmail(), reason, booking.getStudentEmail());
                System.out.println("✅ Professor calendar event deleted successfully");
            } else {
                System.out.println("⚠️ No professor calendar event ID found, skipping deletion");
            }
        } catch (Exception e) {
            System.err.println("❌ Error cancelling professor calendar event: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Cancel event in calendar-service for student if it exists
        try {
            if (booking.getStudentCalendarEventId() != null && !booking.getStudentCalendarEventId().isBlank()) {
                System.out.println("🗑️ Deleting student calendar event: " + booking.getStudentCalendarEventId());
                calendarClient.cancelStudentEvent(booking.getStudentCalendarEventId(), booking.getStudentEmail(), reason);
                System.out.println("✅ Student calendar event deleted successfully");
            } else {
                System.out.println("⚠️ No student calendar event ID found, skipping deletion");
            }
        } catch (Exception e) {
            System.err.println("❌ Error cancelling student calendar event: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}


