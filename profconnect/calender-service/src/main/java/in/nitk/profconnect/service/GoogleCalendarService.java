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
        .setDataStoreFactory(new MemoryDataStoreFactory())
        .setAccessType("offline")     // ✅ ensures refresh token is returned
        .setApprovalPrompt("force")   // ✅ forces consent screen again
        .build();

        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(email);

        return url.build();
    }

    /** Step 2: Check if a user has linked their Google Calendar */
    public boolean isCalendarLinked(String email) {
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
        token.setRefreshToken(tokenResponse.getRefreshToken());
        token.setExpiryTime(expiry);

        System.out.println("✅ Saving token for: " + email);
        tokenRepo.save(token);
        System.out.println("✅ Token saved successfully!");
    }

    /** Step 4: Build authorized Calendar client for a user */
    private Calendar buildService(String email) throws Exception {
        GoogleToken t = tokenRepo.findByUserEmail(email);
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

        // 🔄 Refresh if expired or about to expire
        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
            System.out.println("🔄 Token about to expire, trying to refresh...");
            boolean refreshed = credential.refreshToken();
            if (refreshed) {
                System.out.println("✅ Token refreshed successfully!");
                t.setAccessToken(credential.getAccessToken());
                t.setExpiryTime(credential.getExpirationTimeMilliseconds());
                tokenRepo.save(t);
            } else {
                System.out.println("❌ Token refresh failed. Stored refresh token may be invalid or missing.");
            }
        }

        // 🌐 Build Calendar service
        Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("profconnect-calendar")
                .build();

        // 🧩 Verify which Google account this token belongs to
        try {
            Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("profconnect-calendar")
                    .build();
            Userinfo userInfo = oauth2.userinfo().get().execute();
            System.out.println("🔍 Token belongs to Google account: " + userInfo.getEmail());
        } catch (Exception e) {
            System.out.println("⚠️ Could not verify token owner: " + e.getMessage());
        }

        // 📅 List all calendars
        CalendarList calendarList = service.calendarList().list().execute();
        System.out.println("📅 Calendars linked to this account:");
        for (CalendarListEntry entry : calendarList.getItems()) {
            System.out.println("→ " + entry.getSummary() + " | ID: " + entry.getId());
        }

        // 🗓️ Fetch events from all calendars (past + future)
        for (CalendarListEntry entry : calendarList.getItems()) {
            System.out.println("\n🗓️ Checking calendar: " + entry.getSummary() + " (" + entry.getId() + ")");
            try {
                Events events = service.events().list(entry.getId())
                        .setMaxResults(100)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .setTimeMin(new DateTime("2000-01-01T00:00:00Z"))
                        .execute();

                if (events.getItems().isEmpty()) {
                    System.out.println("⚪ No events found in this calendar.");
                } else {
                    for (Event event : events.getItems()) {
                        String start = (event.getStart().getDateTime() != null)
                                ? event.getStart().getDateTime().toStringRfc3339()
                                : event.getStart().getDate().toStringRfc3339();
                        System.out.println("📌 Event: " + event.getSummary() + " | Start: " + start);
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not fetch events for calendar " + entry.getSummary() + ": " + e.getMessage());
            }
        }

        return service;
    }

    /** Step 5: List events from user's primary calendar */
    public List<Map<String, String>> listEvents(String email, int maxResults) throws Exception {
        System.out.println("🔧 Building service for email: " + email);
        Calendar service = buildService(email);

        if (service == null) {
            System.out.println("❌ No calendar service for: " + email);
            return Collections.emptyList();
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
        List<Map<String, String>> myEvents = new ArrayList<>();

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

            myEvents.add(Map.of(
                    "title", event.getSummary() != null ? event.getSummary() : "(No Title)",
                    "start", start,
                    "end", end != null ? end : start
            ));
        }

        System.out.println("✅ Total events fetched: " + myEvents.size());
        return myEvents;
    }



    /** Step 6: Create new event in user's primary calendar */
    public Event createEvent(String email, Event event) throws Exception {
        Calendar service = buildService(email);
        if (service == null) {
            System.out.println("❌ No calendar service for: " + email);
            return null;
        }

        Event created = service.events().insert("primary", event).execute();
        System.out.println("✅ Event created: " + created.getHtmlLink());
        return created;
    }
}
