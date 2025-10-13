package in.nitk.crud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import in.nitk.crud.filter.JwtFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import in.nitk.crud.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class CrudApplication {
    private static final Logger log = LoggerFactory.getLogger(CrudApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(CrudApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/admin-api/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public CommandLineRunner logMongoInfo(@Value("${spring.data.mongodb.uri:}") String mongoUri,
                                          @Value("${spring.data.mongodb.database:}") String mongoDb,
                                          UserRepository repo) {
        return args -> {
            String masked = mongoUri;
            try {
                if (mongoUri != null && mongoUri.contains("://")) {
                    // mask password
                    masked = mongoUri.replaceAll("(//[^:]+:)([^@]+)(@)", "$1****$3");
                }
            } catch (Exception e) {
                // ignore
            }
            log.info("CRUD service starting. Mongo URI={} DB={}", masked, mongoDb);
            try {
                long count = repo.count();
                log.info("Users collection count={}", count);
            } catch (Throwable t) {
                log.error("Failed to query users collection: {}", t.toString());
            }
        };
    }
}
