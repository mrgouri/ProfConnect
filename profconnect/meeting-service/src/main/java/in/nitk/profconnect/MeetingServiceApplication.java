package com.nitk.meeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.nitk.meeting.filter.JwtFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nitk.meeting.repository.MeetingBookingRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class MeetingServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(MeetingServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MeetingServiceApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/booking-api/*"); // protect booking APIs
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public CommandLineRunner logMongoInfo(
            @Value("${spring.data.mongodb.uri:}") String mongoUri,
            @Value("${spring.data.mongodb.database:}") String mongoDb,
            MeetingBookingRepository repo) {
        return args -> {
            String masked = mongoUri;
            try {
                if (mongoUri != null && mongoUri.contains("://")) {
                    masked = mongoUri.replaceAll("(//[^:]+:)([^@]+)(@)", "$1****$3");
                }
            } catch (Exception e) {
                // ignore
            }
            log.info("Meeting service starting. Mongo URI={} DB={}", masked, mongoDb);
            try {
                long count = repo.count();
                log.info("Events collection count={}", count);
            } catch (Throwable t) {
                log.error("Failed to query events collection: {}", t.toString());
            }
        };
    }
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3001") // your frontend origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}