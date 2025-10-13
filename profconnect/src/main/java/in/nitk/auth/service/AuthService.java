package in.nitk.auth.service;

import in.nitk.auth.model.User;
import in.nitk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockDataService mockDataService;

    /**
     * Authenticate user by email and password.
     * If repository access fails (e.g. DB down), fall back to mock data.
     */
    public User authenticate(String email, String password) {
        try {
            log.debug("Authenticating user by email={}", email);
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.debug("Found user id={}, username={}", user.getId(), user.getUsername());
                // Simple password check (in real app, use proper password hashing)
                if (user.getPassword() != null && user.getPassword().equals(password)) {
                    log.info("Authentication successful for email={}", email);
                    return user;
                } else {
                    log.info("Authentication failed for email={} due to password mismatch", email);
                }
            } else {
                log.info("No user found with email={}", email);
            }
            return null;
        } catch (Exception ex) {
            // Log and fallback to mock users to avoid 500 when DB is unreachable
            log.warn("Repository access failed, falling back to mock data: {}", ex.getMessage());
            List<User> mocks = mockDataService.getMockUsers();
            for (User u : mocks) {
                if (u.getEmail() != null && u.getEmail().equals(email)) {
                    log.debug("Found mock user {} for email={}", u.getUsername(), email);
                    if (u.getPassword() != null && u.getPassword().equals(password)) {
                        log.info("Mock authentication successful for email={}", email);
                        return u;
                    } else {
                        log.info("Mock authentication failed for email={} due to password mismatch", email);
                    }
                }
            }
            return null;
        }
    }
}