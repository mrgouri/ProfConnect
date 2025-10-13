package in.nitk.auth.service;

import in.nitk.auth.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockDataService {
    
    public List<User> getMockUsers() {
        List<User> users = new ArrayList<>();
        
        User user1 = new User();
        user1.setId("mock-1");
        user1.setUsername("john_doe");
        user1.setEmail("john.doe@example.com");
    user1.setFirstName("John");
    user1.setLastName("Doe");
    user1.setName("John Doe");
        user1.setRole("USER");
        user1.setIsActive(true);
        user1.setCreatedAt(LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        user1.setUpdatedAt(LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        users.add(user1);
        
        User user2 = new User();
        user2.setId("mock-2");
        user2.setUsername("jane_smith");
        user2.setEmail("jane.smith@example.com");
    user2.setFirstName("Jane");
    user2.setLastName("Smith");
    user2.setName("Jane Smith");
        user2.setRole("ADMIN");
        user2.setIsActive(true);
        user2.setCreatedAt(LocalDateTime.now().minusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        user2.setUpdatedAt(LocalDateTime.now().minusHours(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        users.add(user2);
        
        User user3 = new User();
        user3.setId("mock-3");
        user3.setUsername("bob_wilson");
        user3.setEmail("bob.wilson@example.com");
    user3.setFirstName("Bob");
    user3.setLastName("Wilson");
    user3.setName("Bob Wilson");
        user3.setRole("MODERATOR");
        user3.setIsActive(false);
        user3.setCreatedAt(LocalDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        user3.setUpdatedAt(LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        users.add(user3);
        
        User user4 = new User();
        user4.setId("mock-4");
        user4.setUsername("alice_brown");
        user4.setEmail("alice.brown@example.com");
    user4.setFirstName("Alice");
    user4.setLastName("Brown");
    user4.setName("Alice Brown");
        user4.setRole("USER");
        user4.setIsActive(true);
        user4.setCreatedAt(LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        user4.setUpdatedAt(LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        users.add(user4);
        
        return users;
    }
}
