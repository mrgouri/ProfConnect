package com.nitk.appointments.repository;

import com.nitk.appointments.model.CalendarToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CalendarTokenRepository extends MongoRepository<CalendarToken, String> {
    Optional<CalendarToken> findByEmail(String email);
}


