package com.nitk.appointments.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "calendar_tokens")
public class CalendarToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String accessToken;
    private String refreshToken;
    private Long expiryMillis;

    public CalendarToken() {}

    public CalendarToken(String email, String accessToken, String refreshToken, Long expiryMillis) {
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiryMillis = expiryMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiryMillis() { return expiryMillis; }
    public void setExpiryMillis(Long expiryMillis) { this.expiryMillis = expiryMillis; }
}


