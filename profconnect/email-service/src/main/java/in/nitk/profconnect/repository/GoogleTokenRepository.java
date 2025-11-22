package in.nitk.profconnect.repository;

import in.nitk.profconnect.model.GoogleToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleTokenRepository extends MongoRepository<GoogleToken, String> {
    GoogleToken findByUserEmail(String userEmail);
}

