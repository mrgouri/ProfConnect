Frontend for auth-service

Requirements:
- Node 18+ (or Node 16+ with node-fetch v3 requires --experimental-modules)

To run:

1. Start backend Spring Boot app (this project) on port 8084:
   - From project root: `./mvnw spring-boot:run` (on Windows use `mvnw.cmd`)

2. Start frontend:
   - cd frontend
   - npm install
   - To run against the default backend (http://localhost:8084):
     npm start
   - To run against a backend on a different URL/port (for example http://localhost:8080):
     # PowerShell
     $env:BACKEND_URL='http://localhost:8080'; npm start

The frontend listens on http://localhost:3001 and talks to backend at the URL set in BACKEND_URL (defaults to http://localhost:8084/api)
