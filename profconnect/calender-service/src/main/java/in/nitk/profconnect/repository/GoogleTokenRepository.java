package com.nitk.calendar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.nitk.calendar.model.GoogleToken;

@Repository
public interface GoogleTokenRepository extends MongoRepository<GoogleToken, String> {
    GoogleToken findByUserEmail(String userEmail);
}
