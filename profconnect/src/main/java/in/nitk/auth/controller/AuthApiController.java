package in.nitk.auth.controller;

import in.nitk.auth.model.User;
import in.nitk.auth.repository.UserRepository;
import in.nitk.auth.service.AuthService;
import in.nitk.auth.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import in.nitk.auth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3001")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private JwtUtil jwtUtil;

    // JSON login endpoint for external frontends
    @PostMapping("/login")
    public ResponseEntity<?> apiLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        }

        User user = authService.authenticate(email, password);
        if (user != null) {
            // hide password
            user.setPassword(null);
             String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            return ResponseEntity.ok(Map.of("user", user, "token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    // Return users list (without passwords)
    @GetMapping("/users")
    public ResponseEntity<?> apiUsers(@RequestHeader(value = "Authorization", required = false) String auth) {
        List<User> users;
        // Validate token if present
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing Authorization header"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        try {
            users = userRepository.findAll();
        } catch (Exception ex) {
            users = mockDataService.getMockUsers();
        }

        var safe = users.stream().map(u -> {
            User copy = new User();
            copy.setId(u.getId());
            copy.setUsername(u.getUsername());
            copy.setEmail(u.getEmail());
            copy.setFirstName(u.getFirstName());
            copy.setLastName(u.getLastName());
            copy.setRole(u.getRole());
            copy.setIsActive(u.getIsActive());
            copy.setCreatedAt(u.getCreatedAt());
            copy.setUpdatedAt(u.getUpdatedAt());
            // intentionally do not set password
            return copy;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("users", safe));
    }

    // Return current user's profile (from token)
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing Authorization header"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        String subject = jwtUtil.getSubject(token);
        try {
            var userOpt = userRepository.findByEmail(subject);
            log.debug("/api/me lookup for subject={}; present={}", subject, userOpt.isPresent());
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                u.setPassword(null);
                return ResponseEntity.ok(Map.of("user", u));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
        } catch (Exception ex) {
            // fallback to mock
            var mocks = mockDataService.getMockUsers();
            for (User mu : mocks) {
                if (mu.getEmail() != null && mu.getEmail().equals(subject)) {
                    mu.setPassword(null);
                    return ResponseEntity.ok(Map.of("user", mu));
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unable to fetch user"));
        }
    }

    // Change password for current user. Body: { currentPassword, newPassword }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @RequestBody Map<String, String> body) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing Authorization header"));
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        String subject = jwtUtil.getSubject(token);
        String current = body.get("currentPassword");
        String next = body.get("newPassword");
        if (next == null || next.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newPassword is required"));
        }
        try {
            log.info("/api/change-password invoked for subject={}", subject);
            var userOpt = userRepository.findByEmail(subject);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                log.debug("Found user id={} email={} (hasPassword={})", u.getId(), u.getEmail(), u.getPassword() != null);
                // verify current password if provided
                if (u.getPassword() != null) {
                    if (current == null || !u.getPassword().equals(current)) {
                        log.info("Current password mismatch for {}", subject);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Current password incorrect"));
                    }
                }
                u.setPassword(next);
                userRepository.save(u);
                log.info("Password updated for {}", subject);
                return ResponseEntity.ok(Map.of("message", "Password updated"));
            } else {
                log.info("User not found during change-password: {}", subject);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }
        } catch (Exception ex) {
            log.error("Exception while changing password for {}: {}", subject, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

}
