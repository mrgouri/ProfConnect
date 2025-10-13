package in.nitk.crud.controller;

import in.nitk.crud.model.User;
import in.nitk.crud.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/admin-api")
@CrossOrigin(origins = "http://localhost:3001")
public class UserController {

    @Autowired
    private UserRepository repo;

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @Autowired(required = false)
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database:}")
    private String configuredDatabase;

    @GetMapping("/users")
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(Map.of("users", repo.findAll()));
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        long count = repo.count();
        List<User> sample = repo.findAll().stream().limit(10).toList();
        return ResponseEntity.ok(Map.of("database", configuredDatabase, "count", count, "sample", sample));
    }

    @GetMapping("/debug-raw")
    public ResponseEntity<?> debugRaw() {
        if (mongoTemplate == null) {
            return ResponseEntity.status(500).body(Map.of("error", "MongoTemplate not available"));
        }
        List<Document> docs = mongoTemplate.findAll(Document.class, "users");
        long count = docs.size();
        List<Document> sample = docs.stream().limit(20).toList();
        return ResponseEntity.ok(Map.of("database", configuredDatabase, "count", count, "sample", sample));
    }

    // Unauthenticated debug endpoint (bypasses JwtFilter because it's not under /admin-api)
    @GetMapping(path = "/crud-debug", produces = "application/json")
    public ResponseEntity<?> crudDebug() {
        long count = repo.count();
        List<User> sample = repo.findAll().stream().limit(20).toList();
        return ResponseEntity.ok(Map.of("database", configuredDatabase, "count", count, "sample", sample));
    }

    @PostMapping("/users")
    public ResponseEntity<?> create(@RequestBody User user) {
        User saved = repo.save(user);

        boolean authCreated = false;
        // prefer using the shared MongoClient so we can target the 'authentication' database
        try {
            if (mongoClient != null && user.getEmail() != null) {
                MongoDatabase authDb = mongoClient.getDatabase("authentication");
                MongoCollection<org.bson.Document> coll = authDb.getCollection("auth");
                Bson filter = Filters.eq("email", user.getEmail());
                org.bson.Document existing = coll.find(filter).first();
                if (existing == null) {
                    String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
                    String password = "";
                    if ("PROFF".equals(role)) password = user.getStaffId() != null ? user.getStaffId() : "";
                    else if ("STUDENT".equals(role)) password = user.getRollNumber() != null ? user.getRollNumber() : "";

                    org.bson.Document authDoc = new org.bson.Document();
                    authDoc.put("email", user.getEmail());
                    authDoc.put("password", password);
                    authDoc.put("role", user.getRole());
                    authDoc.put("isActive", true);
                    authDoc.put("createdAt", Instant.now().toString());
                    authDoc.put("username", user.getEmail());

                    coll.insertOne(authDoc);
                    authCreated = true;
                }
            } else if (mongoTemplate != null && user.getEmail() != null) {
                // fallback: previous behavior (but this writes into the 'users' DB)
                Query q = new Query(Criteria.where("email").is(user.getEmail()));
                boolean exists = mongoTemplate.exists(q, "auth");
                if (!exists) {
                    String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
                    String password = "";
                    if ("PROFF".equals(role)) password = user.getStaffId() != null ? user.getStaffId() : "";
                    else if ("STUDENT".equals(role)) password = user.getRollNumber() != null ? user.getRollNumber() : "";
                    org.bson.Document authDoc = new org.bson.Document();
                    authDoc.put("email", user.getEmail());
                    authDoc.put("password", password);
                    authDoc.put("role", user.getRole());
                    authDoc.put("isActive", true);
                    authDoc.put("createdAt", Instant.now().toString());
                    authDoc.put("username", user.getEmail());
                    mongoTemplate.insert(authDoc, "auth");
                    authCreated = true;
                }
            }
        } catch (Exception ex) {
            System.out.println("Warning: failed to create auth record: " + ex.getMessage());
        }

        return ResponseEntity.ok(Map.of("user", saved, "authCreated", authCreated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> search(@RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "role", required = false) String role) {
        if (q != null && !q.isEmpty() && role != null && !role.isEmpty()) {
            // search by role + name
            var results = repo.findByRoleIgnoreCaseAndNameContainingIgnoreCase(role, q);
            return ResponseEntity.ok(Map.of("users", results));
        } else if (q != null && !q.isEmpty()) {
            // search by name only
            var byName = repo.findByNameContainingIgnoreCase(q);
            return ResponseEntity.ok(Map.of("users", byName));
        } else if (role != null && !role.isEmpty()) {
            return ResponseEntity.ok(Map.of("users", repo.findByRoleIgnoreCase(role)));
        } else {
            return ResponseEntity.ok(Map.of("users", repo.findAll()));
        }
    }
}
