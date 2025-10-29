// MongoDB initialization script for ProfConnect
// This script sets up the databases and initial collections

// Switch to admin database
db = db.getSiblingDB('admin');

// Create databases
db.getSiblingDB('authentication');
db.getSiblingDB('users');
db.getSiblingDB('calendar');

// Switch to authentication database
db = db.getSiblingDB('authentication');

// Create collections and indexes for authentication
db.createCollection('users');
db.users.createIndex({ "email": 1 }, { unique: true });

// Switch to users database
db = db.getSiblingDB('users');

// Create collections and indexes for users
db.createCollection('users');
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "role": 1 });
db.users.createIndex({ "department": 1 });

// Switch to calendar database
db = db.getSiblingDB('calendar');

// Create collections and indexes for calendar
db.createCollection('calendar_tokens');
db.calendar_tokens.createIndex({ "email": 1 }, { unique: true });

db.createCollection('appointments');
db.appointments.createIndex({ "professorEmail": 1 });
db.appointments.createIndex({ "studentEmail": 1 });
db.appointments.createIndex({ "startTime": 1 });
db.appointments.createIndex({ "status": 1 });
db.appointments.createIndex({ "professorEmail": 1, "startTime": 1 });
db.appointments.createIndex({ "studentEmail": 1, "startTime": 1 });

db.createCollection('availability');
db.availability.createIndex({ "professorEmail": 1 });
db.availability.createIndex({ "dayOfWeek": 1 });
db.availability.createIndex({ "professorEmail": 1, "dayOfWeek": 1 });
db.availability.createIndex({ "isActive": 1 });

print('MongoDB initialization completed successfully!');
print('Created databases: authentication, users, calendar');
print('Created collections and indexes for all services');
