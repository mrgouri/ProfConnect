package in.nitk.crud.controller;

import in.nitk.crud.model.User;
import in.nitk.crud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DebugController {

    @Autowired
    private UserRepository repo;

    @Value("${spring.data.mongodb.database:}")
    private String configuredDatabase;

    @GetMapping(path = "/crud-debug", produces = "application/json")
    public ResponseEntity<?> crudDebug() {
        try {
            long count = repo.count();
            List<User> sample = repo.findAll().stream().limit(20).toList();
            return ResponseEntity.ok(Map.of("database", configuredDatabase, "count", count, "sample", sample));
        } catch (Throwable t) {
            // capture stacktrace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            String stack = sw.toString();
            return ResponseEntity.status(500).body(Map.of("error", t.toString(), "message", t.getMessage(), "stack", stack));
        }
    }
}
