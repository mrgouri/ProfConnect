package com.nitk.appointments.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.nitk.appointments.model.CalendarToken;
import com.nitk.appointments.repository.CalendarTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:3000")
public class CalendarController {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${google.oauth.clientId}")
    private String clientId;
    @Value("${google.oauth.clientSecret}")
    private String clientSecret;
    @Value("${google.oauth.redirectUri}")
    private String redirectUri;

    private final CalendarTokenRepository tokenRepo;

    public CalendarController(CalendarTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @GetMapping("/auth-url")
    public Map<String, String> authUrl(@RequestParam String email) throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientId,
                clientSecret,
                List.of("https://www.googleapis.com/auth/calendar.events", "https://www.googleapis.com/auth/calendar.readonly")
        ).setDataStoreFactory(new MemoryDataStoreFactory()).setAccessType("offline").build();

        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(email);
        return Map.of("url", url.build());
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam String code, @RequestParam String state) throws Exception {
        String email = state; // we passed email in state
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, JSON_FACTORY, clientId, clientSecret, code, redirectUri
        ).execute();

        long expiry = Instant.now().toEpochMilli() + (tokenResponse.getExpiresInSeconds() != null ? tokenResponse.getExpiresInSeconds() * 1000L : 0L);
        CalendarToken token = tokenRepo.findByEmail(email).orElse(new CalendarToken());
        token.setEmail(email);
        token.setAccessToken(tokenResponse.getAccessToken());
        token.setRefreshToken(tokenResponse.getRefreshToken());
        token.setExpiryMillis(expiry);
        tokenRepo.save(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/events")
    public ResponseEntity<?> listEvents(@RequestParam String email) throws Exception {
        Calendar service = buildService(email);
        if (service == null) return ResponseEntity.status(401).body(Map.of("message", "Not authorized"));
        Events events = service.events().list("primary").setMaxResults(10).setOrderBy("startTime").setSingleEvents(true)
                .setTimeMin(new com.google.api.client.util.DateTime(System.currentTimeMillis())).execute();
        return ResponseEntity.ok(events.getItems());
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestParam String email, @RequestBody Map<String, String> body) throws Exception {
        Calendar service = buildService(email);
        if (service == null) return ResponseEntity.status(401).body(Map.of("message", "Not authorized"));
        Event event = new Event();
        event.setSummary(body.getOrDefault("summary", "Meeting"));
        event.setDescription(body.getOrDefault("description", ""));
        String startIso = body.get("start");
        String endIso = body.get("end");
        event.setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startIso)));
        event.setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endIso)));
        Event created = service.events().insert("primary", event).execute();
        return ResponseEntity.ok(created);
    }

    private Calendar buildService(String email) throws Exception {
        Optional<CalendarToken> opt = tokenRepo.findByEmail(email);
        if (opt.isEmpty()) return null;
        CalendarToken t = opt.get();
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = new com.google.api.client.auth.oauth2.Credential.Builder(com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientAuthentication(new com.google.api.client.auth.oauth2.ClientParametersAuthentication(clientId, clientSecret))
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .build()
                .setAccessToken(t.getAccessToken())
                .setRefreshToken(t.getRefreshToken());
        if (t.getExpiryMillis() != null) credential.setExpirationTimeMilliseconds(t.getExpiryMillis());
        // Refresh if needed and persist
        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
            if (credential.refreshToken()) {
                t.setAccessToken(credential.getAccessToken());
                t.setExpiryMillis(credential.getExpirationTimeMilliseconds());
                tokenRepo.save(t);
            }
        }
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("appointment-system")
                .build();
    }
}


