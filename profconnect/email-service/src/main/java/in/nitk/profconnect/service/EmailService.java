package in.nitk.profconnect.service;

import in.nitk.profconnect.model.EmailRecord;
import in.nitk.profconnect.repository.EmailRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailRecordRepository emailRecordRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, EmailRecordRepository emailRecordRepository) {
        this.mailSender = mailSender;
        this.emailRecordRepository = emailRecordRepository;
    }

    public void sendMeetingNotification(String professorEmail, String studentEmail, String studentName,
                                       String meetingTitle, String description, String location,
                                       String startTime, String endTime) {

        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setType("BOOKING");
        emailRecord.setRecipientRole("PROFESSOR");
        emailRecord.setRecipientEmail(professorEmail);
        emailRecord.setRecipientName(null);
        emailRecord.setSubject("New Meeting Scheduled: " + meetingTitle);
        emailRecord.setMeetingTitle(meetingTitle);
        emailRecord.setStudentEmail(studentEmail);
        emailRecord.setStudentName(studentName);
        emailRecord.setProfessorEmail(professorEmail);
        emailRecord.setSentAt(Instant.now());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(professorEmail);
            message.setSubject("New Meeting Scheduled: " + meetingTitle);

            String formattedStart = formatDateTime(startTime);
            String formattedEnd = formatDateTime(endTime);

            String emailBody = String.format(
                "Dear Professor,\n\n" +
                "A new meeting has been scheduled with you:\n\n" +
                "Title: %s\n" +
                "Student: %s (%s)\n" +
                "Date & Time: %s to %s\n" +
                "Location: %s\n" +
                "Description: %s\n\n" +
                "This meeting has been automatically added to your Google Calendar.\n\n" +
                "Best regards,\n" +
                "ProfConnect System",
                meetingTitle,
                studentName != null ? studentName : "Student",
                studentEmail,
                formattedStart,
                formattedEnd,
                location != null && !location.isEmpty() ? location : "Not specified",
                description != null && !description.isEmpty() ? description : "No description provided"
            );

            message.setText(emailBody);
            mailSender.send(message);
            emailRecord.setStatus("SENT");
            System.out.println("✅ Email notification sent to professor: " + professorEmail);
        } catch (Exception e) {
            emailRecord.setStatus("FAILED");
            emailRecord.setErrorMessage(e.getMessage());
            System.err.println("⚠️ Failed to send email notification: " + e.getMessage());
            throw new RuntimeException("Failed to send email notification", e);
        } finally {
            emailRecordRepository.save(emailRecord);
        }
    }

    public void sendConfirmationEmail(String studentEmail, String studentName, String professorEmail,
                                     String professorName, String meetingTitle, String startTime, String endTime) {

        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setType("BOOKING");
        emailRecord.setRecipientRole("STUDENT");
        emailRecord.setRecipientEmail(studentEmail);
        emailRecord.setRecipientName(studentName);
        emailRecord.setSubject("Meeting Confirmation: " + meetingTitle);
        emailRecord.setMeetingTitle(meetingTitle);
        emailRecord.setProfessorEmail(professorEmail);
        emailRecord.setProfessorName(professorName);
        emailRecord.setStudentEmail(studentEmail);
        emailRecord.setStudentName(studentName);
        emailRecord.setSentAt(Instant.now());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(studentEmail);
            message.setSubject("Meeting Confirmation: " + meetingTitle);

            String formattedStart = formatDateTime(startTime);
            String formattedEnd = formatDateTime(endTime);

            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your meeting has been successfully booked!\n\n" +
                "Meeting Details:\n" +
                "Title: %s\n" +
                "Professor: %s (%s)\n" +
                "Date & Time: %s to %s\n\n" +
                "This meeting has been automatically added to your Google Calendar.\n" +
                "You will receive a reminder before the meeting.\n\n" +
                "Best regards,\n" +
                "ProfConnect System",
                studentName != null ? studentName : "Student",
                meetingTitle,
                professorName != null ? professorName : "Professor",
                professorEmail,
                formattedStart,
                formattedEnd
            );

            message.setText(emailBody);
            mailSender.send(message);
            emailRecord.setStatus("SENT");
            System.out.println("✅ Confirmation email sent to student: " + studentEmail);
        } catch (Exception e) {
            emailRecord.setStatus("FAILED");
            emailRecord.setErrorMessage(e.getMessage());
            System.err.println("⚠️ Failed to send confirmation email: " + e.getMessage());
            throw new RuntimeException("Failed to send confirmation email", e);
        } finally {
            emailRecordRepository.save(emailRecord);
        }
    }

    public void sendCancellationEmailToStudent(String studentEmail, String professorEmail, String professorName, String reason) {

        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setType("CANCELLATION");
        emailRecord.setRecipientRole("STUDENT");
        emailRecord.setRecipientEmail(studentEmail);
        emailRecord.setRecipientName(null);
        emailRecord.setSubject("Meeting Cancelled");
        emailRecord.setProfessorEmail(professorEmail);
        emailRecord.setProfessorName(professorName);
        emailRecord.setStudentEmail(studentEmail);
        emailRecord.setStudentName(null);
        emailRecord.setReason(reason);
        emailRecord.setSentAt(Instant.now());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(studentEmail);
            message.setSubject("Meeting Cancelled");

            String emailBody = String.format(
                "Dear Student,\n\n" +
                "Unfortunately, your meeting with Professor %s (%s) has been cancelled.\n\n",
                professorName != null ? professorName : "Professor",
                professorEmail
            );

            if (reason != null && !reason.trim().isEmpty()) {
                emailBody += String.format("Reason: %s\n\n", reason);
            }

            emailBody += "You may want to book a new meeting time if needed.\n\n" +
                        "Best regards,\n" +
                        "ProfConnect System";

            message.setText(emailBody);
            mailSender.send(message);
            emailRecord.setStatus("SENT");
            System.out.println("✅ Cancellation email sent to student: " + studentEmail);
        } catch (Exception e) {
            emailRecord.setStatus("FAILED");
            emailRecord.setErrorMessage(e.getMessage());
            System.err.println("⚠️ Failed to send cancellation email to student: " + e.getMessage());
            throw new RuntimeException("Failed to send cancellation email to student", e);
        } finally {
            emailRecordRepository.save(emailRecord);
        }
    }

    public void sendCancellationEmailToProfessor(String professorEmail, String studentEmail, String studentName, String reason) {

        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setType("CANCELLATION");
        emailRecord.setRecipientRole("PROFESSOR");
        emailRecord.setRecipientEmail(professorEmail);
        emailRecord.setRecipientName(null);
        emailRecord.setSubject("Meeting Cancelled");
        emailRecord.setProfessorEmail(professorEmail);
        emailRecord.setStudentEmail(studentEmail);
        emailRecord.setStudentName(studentName);
        emailRecord.setReason(reason);
        emailRecord.setSentAt(Instant.now());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(professorEmail);
            message.setSubject("Meeting Cancelled");

            String emailBody = String.format(
                "Dear Professor,\n\n" +
                "The meeting with %s (%s) has been cancelled.\n\n",
                studentName != null ? studentName : "Student",
                studentEmail
            );

            if (reason != null && !reason.trim().isEmpty()) {
                emailBody += String.format("Reason: %s\n\n", reason);
            }

            emailBody += "The event has been removed from your Google Calendar.\n\n" +
                        "Best regards,\n" +
                        "ProfConnect System";

            message.setText(emailBody);
            mailSender.send(message);
            emailRecord.setStatus("SENT");
            System.out.println("✅ Cancellation email sent to professor: " + professorEmail);
        } catch (Exception e) {
            emailRecord.setStatus("FAILED");
            emailRecord.setErrorMessage(e.getMessage());
            System.err.println("⚠️ Failed to send cancellation email to professor: " + e.getMessage());
            throw new RuntimeException("Failed to send cancellation email to professor", e);
        } finally {
            emailRecordRepository.save(emailRecord);
        }
    }

    private String formatDateTime(String isoDateTime) {
        try {
            java.time.Instant instant = java.time.Instant.parse(isoDateTime);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            return zdt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a"));
        } catch (Exception e) {
            return isoDateTime; // Return original if parsing fails
        }
    }
}

