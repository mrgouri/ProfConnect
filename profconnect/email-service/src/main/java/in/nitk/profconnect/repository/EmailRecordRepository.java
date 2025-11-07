package in.nitk.profconnect.repository;

import in.nitk.profconnect.model.EmailRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailRecordRepository extends MongoRepository<EmailRecord, String> {
}


