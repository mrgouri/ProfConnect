package com.nitk.calendar.service;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import com.nitk.calendar.model.GoogleToken;
import com.nitk.calendar.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.util.DateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;


import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleCalendarService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Value("${google.oauth.clientId}")
    private String clientId;
    @Value("${google.oauth.clientSecret}")
    private String clientSecret;
    @Value("${google.oauth.redirectUri}")
    private String redirectUri;

    private final GoogleTokenRepository tokenRepo;

    public GoogleCalendarService(GoogleTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    /** Step 1: Generate the Google OAuth consent URL */
    public String getAuthUrl(String email) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport,
        JSON_FACTORY,
        clientId,
        clientSecret,
        List.of(
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/calendar.readonly",
            "https://www.googleapis.com/auth/userinfo.email"
        )
    )
    // Use in-memory store for the flow object only; persistent tokens are saved in MongoDB (tokenRepo)
    .setDataStoreFactory(new MemoryDataStoreFactory())
    .setAccessType("offline")     // ensures refresh token is requested
    // Use the modern 'prompt=consent' parameter so Google returns a refresh token reliably
    .build();

    GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
        .setRedirectUri(redirectUri)
        .setState(email);
    // force the consent screen to return a refresh token when needed
    url.set("prompt", "consent");

        return url.build();
    }

    /** Step 2: Check if a user has linked their Google Calendar */
    public boolean isCalendarLinked(String email) {
        try {
            String decoded = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8);
            GoogleToken token = tokenRepo.findByUserEmail(decoded);
            if (token != null) return true;
        } catch (Exception ex) {
            // ignore
        }
        GoogleToken token = tokenRepo.findByUserEmail(email);
        return token != null;
    }

    /** Step 3: Handle OAuth callback and store tokens */
    public void handleCallback(String code, String email) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, JSON_FACTORY, clientId, clientSecret, code, redirectUri
        ).execute();

        long expiry = Instant.now().toEpochMilli() + tokenResponse.getExpiresInSeconds() * 1000L;
        GoogleToken token = Optional.ofNullable(tokenRepo.findByUserEmail(email)).orElse(new GoogleToken());

        token.setUserEmail(email);
        token.setAccessToken(tokenResponse.getAccessToken());
        // Only overwrite refresh token when Google actually returns one.
        // Google may return null on subsequent authorizations unless prompt=consent is used.
        if (tokenResponse.getRefreshToken() != null && !tokenResponse.getRefreshToken().isBlank()) {
            token.setRefreshToken(tokenResponse.getRefreshToken());
        } else {
            // preserve existing refresh token if present
            if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
                System.out.println("⚠️ No refresh token returned by Google for " + email + " and none stored.");
            } else {
                System.out.println("ℹ️ No refresh token in callback; keeping previously stored refresh token for " + email);
            }
        }
        token.setExpiryTime(expiry);

        System.out.println("✅ Saving token for: " + email);
        tokenRepo.save(token);
        System.out.println("✅ Token saved successfully!");
    }

    /** Step 4: Build authorized Calendar client for a user */
    private Calendar buildService(String email) throws Exception {
        GoogleToken t = null;
        try {
            String decoded = java.net.URLDecoder.decode(email, java.nio.charset.StandardCharsets.UTF_8);
            t = tokenRepo.findByUserEmail(decoded);
            if (t != null) {
                email = decoded; // use decoded for logging and further actions
            }
        } catch (Exception ex) {
            // ignore
        }
        if (t == null) {
            t = tokenRepo.findByUserEmail(email);
        }
        if (t == null) {
            System.out.println("❌ No token found for: " + email);
            return null;
        }

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Build credential using stored tokens
        var credential = new com.google.api.client.auth.oauth2.Credential.Builder(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod()
        )
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .setClientAuthentication(new com.google.api.client.auth.oauth2.ClientParametersAuthentication(clientId, clientSecret))
        .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
        .build()
        .setAccessToken(t.getAccessToken())
        .setRefreshToken(t.getRefreshToken());

        if (t.getExpiryTime() != null)
            credential.setExpirationTimeMilliseconds(t.getExpiryTime());

        // 🔄 Refresh if expired or about to expire — use stored expiry time when available
        long now = Instant.now().toEpochMilli();
        boolean shouldRefresh = false;
        if (t.getExpiryTime() != null) {
            shouldRefresh = t.getExpiryTime() <= (now + 60L * 1000L);
        } else if (credential.getExpiresInSeconds() != null) {
            shouldRefresh = credential.getExpiresInSeconds() <= 60;
        }

        if (shouldRefresh) {
            System.out.println("🔄 Token about to expire for " + email + ", attempting refresh...");
            if (t.getRefreshToken() == null || t.getRefreshToken().isBlank()) {
                System.out.println("❌ Cannot refresh token because no refresh token is stored for: " + email);
            } else {
                boolean refreshed = credential.refreshToken();
                if (refreshed) {
                    System.out.println("✅ Token refreshed successfully!");
                    t.setAccessToken(credential.getAccessToken());
                    t.setExpiryTime(credential.getExpirationTimeMilliseconds());
                    tokenRepo.save(t);
                } else {
                    System.out.println("❌ Token refresh failed. Stored refresh token may be invalid.");
                }
            }
        }

        // 🌐 Build Calendar service
        Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("profconnect-calendar")
                .build();

        return service;
    }

    /** Step 5: List events from user's primary calendar */
    public List<Map<String, Object>> listEvents(String email, int maxResults) throws Exception {
        System.out.println("🔧 Building service for email: " + email);
        
        // Check if calendar is linked first
        if (!isCalendarLinked(email)) {
            System.out.println("❌ No calendar linked for: " + email);
            throw new Exception("No calendar linked for " + email + ". Please connect your Google Calendar first.");
        }
        
        Calendar service = buildService(email);

        if (service == null) {
            System.out.println("❌ No calendar service for: " + email);
            throw new Exception("Failed to build calendar service for " + email);
        }

        System.out.println("✅ Fetching events from primary calendar...");

        // Fetch all events, ordered by start time
        Events events = service.events().list("primary")
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setTimeMin(new DateTime("2000-01-01T00:00:00Z")) // get all future & past events
                .execute();

        List<Event> items = events.getItems();
        List<Map<String, Object>> myEvents = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            System.out.println("🚫 No events found for user: " + email);
            return myEvents;
        }

        for (Event event : items) {
            // handle both dateTime and all-day date
            String start = event.getStart() != null
                    ? (event.getStart().getDateTime() != null
                        ? event.getStart().getDateTime().toStringRfc3339()
                        : event.getStart().getDate() != null
                            ? event.getStart().getDate().toStringRfc3339()
                            : null)
                    : null;

            String end = event.getEnd() != null
                    ? (event.getEnd().getDateTime() != null
                        ? event.getEnd().getDateTime().toStringRfc3339()
                        : event.getEnd().getDate() != null
                            ? event.getEnd().getDate().toStringRfc3339()
                            : null)
                    : null;

            if (start == null) {
                System.out.println("⚠️ Skipping event with no start time: " + event.getSummary());
                continue;
            }

            System.out.println("📅 Event: " + event.getSummary() + " | Start: " + start);

            // Determine if event is all-day (date-only, no time)
            boolean isAllDay = event.getStart() != null && event.getStart().getDate() != null;
            
            Map<String, Object> eventMap = new java.util.HashMap<>();
            eventMap.put("title", event.getSummary() != null ? event.getSummary() : "(No Title)");
            eventMap.put("start", start);
            eventMap.put("end", end != null ? end : start);
            eventMap.put("allDay", isAllDay);
            // Include event ID - this is critical for cancel functionality
            if (event.getId() != null) {
                eventMap.put("id", event.getId());
            }
            if (event.getDescription() != null) {
                eventMap.put("description", event.getDescription());
            }
            if (event.getLocation() != null) {
                eventMap.put("location", event.getLocation());
            }
            
            myEvents.add(eventMap);
        }

        System.out.println("✅ Total events fetched: " + myEvents.size());
        return myEvents;
    }



    /** Step 6: Create new event in user's primary calendar */
    public Event createEvent(String email, Event event) throws Exception {
        System.out.println("📅 Creating event in Google Calendar for: " + email);
        System.out.println("   Event Summary: " + (event.getSummary() != null ? event.getSummary() : "N/A"));
        System.out.println("   Event Start: " + (event.getStart() != null && event.getStart().getDateTime() != null ? event.getStart().getDateTime().toStringRfc3339() : "N/A"));
        System.out.println("   Event End: " + (event.getEnd() != null && event.getEnd().getDateTime() != null ? event.getEnd().getDateTime().toStringRfc3339() : "N/A"));
        
        Calendar service = buildService(email);
        if (service == null) {
            System.out.println("❌ No calendar service for: " + email + " - Calendar may not be linked");
            throw new Exception("No calendar linked for " + email + ". Please connect your Google Calendar first.");
        }

        try {
            Event created = service.events().insert("primary", event).execute();
            if (created != null && created.getId() != null) {
                System.out.println("✅ Event created successfully in Google Calendar!");
                System.out.println("   Event ID: " + created.getId());
                System.out.println("   Event Link: " + (created.getHtmlLink() != null ? created.getHtmlLink() : "N/A"));
                return created;
            } else {
                System.err.println("❌ Event creation returned null or no ID");
                throw new Exception("Failed to create event in Google Calendar - no event ID returned");
            }
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            System.err.println("❌ Google Calendar API error: " + e.getMessage());
            System.err.println("   Status Code: " + e.getStatusCode());
            if (e.getDetails() != null) {
                System.err.println("   Error Details: " + e.getDetails());
            }
            throw new Exception("Failed to create event in Google Calendar: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Error creating event: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /** Step 7: Delete event from user's primary calendar */
    public boolean deleteEvent(String email, String eventId, String reason) throws Exception {
        Calendar service = buildService(email);
        if (service == null) {
            System.out.println("❌ No calendar service for: " + email);
            return false;
        }

        try {
            service.events().delete("primary", eventId).execute();
            System.out.println("✅ Event deleted: " + eventId + " for " + email);
            if (reason != null && !reason.isBlank()) {
                System.out.println("   Reason: " + reason);
            }
            return true;
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                System.out.println("⚠️ Event not found: " + eventId);
                return false;
            }
            throw e;
        }
    }
}
