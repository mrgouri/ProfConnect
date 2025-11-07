package in.nitk.profconnect.controller;

import in.nitk.profconnect.dto.BookingEmailRequest;
import in.nitk.profconnect.dto.CancellationEmailRequest;
import in.nitk.profconnect.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/email-api")
@CrossOrigin(origins = "http://localhost:3001")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/booking")
    public ResponseEntity<?> sendBookingEmails(@RequestBody BookingEmailRequest request) {
        try {
            System.out.println("📧 Received booking email request");
            System.out.println("   Professor: " + request.getProfessorEmail());
            System.out.println("   Student: " + request.getStudentEmail());
            
            // Send email to professor
            emailService.sendMeetingNotification(
                    request.getProfessorEmail(),
                    request.getStudentEmail(),
                    request.getStudentName(),
                    request.getMeetingTitle() != null ? request.getMeetingTitle() : "Meeting",
                    request.getDescription(),
                    request.getLocation(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            
            // Send email to student
            emailService.sendConfirmationEmail(
                    request.getStudentEmail(),
                    request.getStudentName(),
                    request.getProfessorEmail(),
                    request.getProfessorName(),
                    request.getMeetingTitle() != null ? request.getMeetingTitle() : "Meeting",
                    request.getStartTime(),
                    request.getEndTime()
            );
            
            System.out.println("✅ Booking emails sent successfully to both professor and student");
            return ResponseEntity.ok(Map.of("message", "Emails sent successfully"));
        } catch (Exception e) {
            System.err.println("❌ Error sending booking emails: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send emails",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/cancellation")
    public ResponseEntity<?> sendCancellationEmails(@RequestBody CancellationEmailRequest request) {
        try {
            System.out.println("📧 Received cancellation email request");
            System.out.println("   Professor: " + request.getProfessorEmail());
            System.out.println("   Student: " + request.getStudentEmail());
            
            // Send email to student
            emailService.sendCancellationEmailToStudent(
                    request.getStudentEmail(),
                    request.getProfessorEmail(),
                    request.getProfessorName(),
                    request.getReason()
            );
            
            // Send email to professor
            emailService.sendCancellationEmailToProfessor(
                    request.getProfessorEmail(),
                    request.getStudentEmail(),
                    request.getStudentName(),
                    request.getReason()
            );
            
            System.out.println("✅ Cancellation emails sent successfully to both professor and student");
            return ResponseEntity.ok(Map.of("message", "Cancellation emails sent successfully"));
        } catch (Exception e) {
            System.err.println("❌ Error sending cancellation emails: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send cancellation emails",
                    "message", e.getMessage()
            ));
        }
    }
}

