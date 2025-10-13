package in.nitk.crud.repository;

import in.nitk.crud.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByRole(String role);
    List<User> findByUsernameContainingIgnoreCase(String q);
    List<User> findByEmailContainingIgnoreCase(String q);
    List<User> findByNameContainingIgnoreCase(String q);
    List<User> findByRoleIgnoreCase(String role);
    List<User> findByRoleIgnoreCaseAndNameContainingIgnoreCase(String role, String q);
    Optional<User> findByEmail(String email);
}
