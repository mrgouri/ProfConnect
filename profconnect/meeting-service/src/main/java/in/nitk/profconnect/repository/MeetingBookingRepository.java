package com.nitk.meeting.repository;

import com.nitk.meeting.model.MeetingBooking;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MeetingBookingRepository extends MongoRepository<MeetingBooking, String> {
    List<MeetingBooking> findByStudentEmailOrderByStartIsoDesc(String studentEmail);
    List<MeetingBooking> findByProfessorEmailOrderByStartIsoDesc(String professorEmail);
}


