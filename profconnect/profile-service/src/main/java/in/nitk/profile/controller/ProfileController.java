package in.nitk.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import in.nitk.profile.model.User;
import in.nitk.profile.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/profiles")
@CrossOrigin(origins = "http://localhost:3001")
public class ProfileController {

    @Autowired
    private UserRepository repo;

    @GetMapping("/by-email")
    public ResponseEntity<?> getByEmail(@RequestParam String email) {
        if (email == null || email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","email is required"));
        var opt = repo.findByEmail(email);
        if (opt.isPresent()) return ResponseEntity.ok(Map.of("user", opt.get()));
        return ResponseEntity.status(404).body(Map.of("error","User not found"));
    }

    @PostMapping("/by-email/location")
    public ResponseEntity<?> updateLocation(@RequestParam String email, @RequestBody Map<String,String> body) {
        if (email == null || email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","email is required"));
        String location = body.get("location");
        if (location == null) return ResponseEntity.badRequest().body(Map.of("error","location is required"));
        var opt = repo.findByEmail(email);
        if (opt.isPresent()) {
            User u = opt.get();
            u.setLocation(location);
            User saved = repo.save(u);
            return ResponseEntity.ok(Map.of("user", saved));
        }
        return ResponseEntity.status(404).body(Map.of("error","User not found"));
    }
}
