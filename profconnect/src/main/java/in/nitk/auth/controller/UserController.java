package in.nitk.auth.controller;

import in.nitk.auth.model.User;
import in.nitk.auth.repository.UserRepository;
import in.nitk.auth.service.AuthService;
import in.nitk.auth.service.MockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private MockDataService mockDataService;
    
    // Show login form at root URL
    @GetMapping("/")
    public String showLoginPage() {
        return "login";
    }

    // Handle login form submission
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {
        User user = authService.authenticate(email, password);
        if (user != null) {
            model.addAttribute("user", user);
            // populate model for index view (safe: handles DB failures and uses mock data)
            populateIndexModel(model);
            return "index";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/refresh")
    public String refresh(Model model) {
        // Re-populate index page data safely and return index
        populateIndexModel(model);
        return "index";
    }

    /**
     * Helper to populate model attributes for the index view.
     * Tries repository first, falls back to MockDataService on error.
     */
    private void populateIndexModel(Model model) {
        List<User> users;
        boolean isMock = false;
        String connectionStatus = "Connected to MongoDB Atlas!";
        String error = null;
        try {
            users = userRepository.findAll();
        } catch (Exception ex) {
            // fallback to mock data
            isMock = true;
            users = mockDataService.getMockUsers();
            connectionStatus = "Connection failed";
            error = ex.getMessage();
        }

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users != null ? users.size() : 0);
        model.addAttribute("connectionStatus", connectionStatus + (error != null ? ": " + error : ""));
        model.addAttribute("isMockData", isMock);
        if (error != null) {
            model.addAttribute("error", error);
        }
    }
}
