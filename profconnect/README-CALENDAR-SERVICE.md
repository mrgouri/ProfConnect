# Calendar Service - Microservice Architecture

## Overview
The Calendar Service is a microservice that handles appointment booking, availability management, and Google Calendar integration for the ProfConnect platform.

## Architecture

### Service Details
- **Port**: 8084
- **Database**: MongoDB (separate database: `calendar`)
- **Authentication**: JWT-based with microservice communication

### Database Schema

#### Collections

1. **calendar_tokens**
   - Stores Google OAuth tokens for calendar integration
   - Fields: id, email, accessToken, refreshToken, expiryMillis

2. **appointments**
   - Stores appointment bookings between professors and students
   - Fields: id, professorEmail, studentEmail, studentName, title, description, startTime, endTime, status, location, meetingLink, type, notes, createdAt, updatedAt

3. **availability**
   - Stores professor availability schedules
   - Fields: id, professorEmail, dayOfWeek, startTime, endTime, location, type, isRecurring, maxBookingsPerSlot, durationMinutes, isActive, notes, createdAt, updatedAt

## API Endpoints

### Calendar Integration
- `GET /calendar-api/auth-url?email={email}` - Get Google Calendar authorization URL
- `GET /calendar-api/oauth2/callback` - Handle OAuth callback
- `GET /calendar-api/events` - List Google Calendar events
- `POST /calendar-api/events` - Create Google Calendar event

### Appointments
- `POST /api/appointments` - Create new appointment
- `GET /api/appointments/professor` - Get appointments for professor
- `GET /api/appointments/student` - Get appointments for student
- `GET /api/appointments/upcoming` - Get upcoming appointments
- `PUT /api/appointments/{id}/status` - Update appointment status
- `PUT /api/appointments/{id}/cancel` - Cancel appointment
- `GET /api/appointments/range?start={start}&end={end}` - Get appointments by date range

### Availability
- `POST /api/availability` - Create availability slot
- `GET /api/availability` - Get professor availability
- `GET /api/availability/active` - Get active availability
- `GET /api/availability/professor/{email}` - Get public availability for professor
- `GET /api/availability/day/{dayOfWeek}` - Get availability by day
- `PUT /api/availability/{id}` - Update availability
- `DELETE /api/availability/{id}` - Delete availability
- `PUT /api/availability/{id}/toggle` - Toggle availability status

## Frontend Integration

### Frontend Server (Port 3001)
The frontend server acts as a proxy and includes:
- `/calendar-api/*` - Proxies to Calendar Service (port 8084)
- `/admin-api/*` - Proxies to CRUD Service (port 8081)
- `/api/*` - Proxies to Auth Service (port 8080)

### Frontend Pages
- `calendar.html` - General calendar interface for all users
- `prof-calendar.html` - Professor-specific calendar dashboard

## Running the Services

### Prerequisites
- Java 17+
- Node.js 16+
- MongoDB (local or Atlas)
- Google Calendar API credentials

### Environment Variables
```bash
# Calendar Service
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/calendar
GOOGLE_OAUTH_CLIENT_ID=your_client_id
GOOGLE_OAUTH_CLIENT_SECRET=your_client_secret

# Frontend
CALENDAR_URL=http://localhost:8084
BACKEND_URL=http://localhost:8080
CRUD_URL=http://localhost:8081
```

### Starting Services

1. **Start Calendar Service**:
   ```bash
   cd profconnect/calender-service
   ./mvnw spring-boot:run
   ```

2. **Start Frontend**:
   ```bash
   cd profconnect/frontend
   npm start
   ```

## Microservice Communication

### JWT Token Flow
1. User authenticates with Auth Service (port 8080)
2. Frontend receives JWT token
3. Frontend forwards JWT to Calendar Service
4. Calendar Service validates JWT and extracts user email

### Service URLs
- Auth Service: http://localhost:8080
- CRUD Service: http://localhost:8081
- Profile Service: http://localhost:8083
- Calendar Service: http://localhost:8084
- Frontend: http://localhost:3001

## Google Calendar Integration

### Setup
1. Create Google Cloud Project
2. Enable Calendar API
3. Create OAuth 2.0 credentials
4. Update application.properties with credentials

### OAuth Flow
1. User clicks "Connect Google Calendar"
2. Redirected to Google OAuth
3. User grants permissions
4. Callback saves tokens to database
5. Service can now sync appointments

## Security

### JWT Configuration
- Secret: `changeitchangeitchangeitchangeitchangeit`
- Expiration: 3600000ms (1 hour)
- Algorithm: HS256

### CORS Configuration
- Allowed origins: `http://localhost:3000`, `http://localhost:8080`, `http://localhost:8081`, `http://localhost:8083`
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: true

## Database Configuration

### MongoDB Atlas
```properties
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/calendar?retryWrites=true&w=majority
```

### Local MongoDB
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/calendar
```

## Development Notes

### Package Structure
```
in.nitk.profconnect/
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── CalendarController.java
│   ├── AppointmentController.java
│   └── AvailabilityController.java
├── filter/
│   └── JwtFilter.java
├── model/
│   ├── CalendarToken.java
│   ├── Appointment.java
│   └── Availability.java
├── repository/
│   ├── CalendarTokenRepository.java
│   ├── AppointmentRepository.java
│   └── AvailabilityRepository.java
├── util/
│   └── JwtUtil.java
└── demo/
    └── CalendarServiceApplication.java
```

### Testing
- Use Postman or curl to test API endpoints
- Ensure JWT token is included in Authorization header
- Test Google Calendar integration with valid OAuth credentials

## Troubleshooting

### Common Issues
1. **JWT Token Invalid**: Check token expiration and secret configuration
2. **Google Calendar Not Working**: Verify OAuth credentials and redirect URI
3. **Database Connection**: Check MongoDB URI and network connectivity
4. **CORS Issues**: Verify allowed origins in SecurityConfig

### Logs
- Calendar Service logs: Check console output for Spring Boot logs
- Frontend logs: Check browser console and server logs
- Database logs: Check MongoDB logs for connection issues
