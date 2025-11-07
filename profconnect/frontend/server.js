const express = require('express');
const path = require('path');
const proxy = require('express-http-proxy');
// Node 18+ provides a global fetch implementation

const app = express();
const PORT = process.env.PORT || 3001;
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const CALENDAR_URL = process.env.CALENDAR_URL || 'http://localhost:8084';
const MEETING_URL = process.env.MEETING_URL || 'http://localhost:8085';
const PROFILE_URL = process.env.PROFILE_URL || 'http://localhost:8083';
const EMAIL_URL = process.env.EMAIL_URL || 'http://localhost:8086';

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

app.post('/login', async (req, res) => {
  try {
  const incomingAuth = req.headers['authorization'];
  const headers = { 'Content-Type': 'application/json' };
  if (incomingAuth) headers['Authorization'] = incomingAuth;
  const resp = await fetch(`${BACKEND_URL}/api/login`, {
      method: 'POST',
      headers,
      body: JSON.stringify(req.body)
    });
    const json = await resp.json();
    res.status(resp.status).json(json);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/users', async (req, res) => {
  try {
  const incomingAuth = req.headers['authorization'];
  const headers = {};
  if (incomingAuth) headers['Authorization'] = incomingAuth;
  const resp = await fetch(`${BACKEND_URL}/api/users`, { headers });
    const status = resp.status;
    const json = await resp.json();
    res.status(status).json(json);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy admin-api to CRUD service on port 8081
app.use('/admin-api', async (req, res) => {
  try {
    const CRUD_URL = process.env.CRUD_URL || 'http://localhost:8081';
    const path = req.originalUrl; // includes /admin-api
    const target = `${CRUD_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    // copy content-type for POST/PUT
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const json = await resp.json();
    res.status(status).json(json);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy calendar-api to Calendar service on port 8084
app.use('/calendar-api', async (req, res) => {
  try {
    const path = req.originalUrl; // keep /calendar-api prefix
    const target = `${CALENDAR_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const text = await resp.text();
    // try to parse JSON, otherwise return text
    try {
      const json = JSON.parse(text);
      res.status(status).json(json);
    } catch (e) {
      res.status(status).send(text);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy booking-api to Meeting service on port 8085
app.use('/booking-api', async (req, res) => {
  try {
    const path = req.originalUrl; // keep /booking-api prefix
    const target = `${MEETING_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const text = await resp.text();
    try {
      const json = JSON.parse(text);
      res.status(status).json(json);
    } catch (e) {
      res.status(status).send(text);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy profiles-api to Profile service on port 8083
app.use('/profiles-api', async (req, res) => {
  try {
    const path = req.originalUrl.replace('/profiles-api', '/profiles'); // replace /profiles-api with /profiles
    const target = `${PROFILE_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const text = await resp.text();
    try {
      const json = JSON.parse(text);
      res.status(status).json(json);
    } catch (e) {
      res.status(status).send(text);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy email-api to Email service on port 8086
app.use('/email-api', async (req, res) => {
  try {
    const path = req.originalUrl; // keep /email-api prefix
    const target = `${EMAIL_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const text = await resp.text();
    try {
      const json = JSON.parse(text);
      res.status(status).json(json);
    } catch (e) {
      res.status(status).send(text);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Proxy API calls to backend auth service (port 8080)
app.use('/api', async (req, res) => {
  try {
    const path = req.originalUrl; // includes /api
    const target = `${BACKEND_URL}${path}`;
    const incomingAuth = req.headers['authorization'];
    const headers = {};
    if (incomingAuth) headers['Authorization'] = incomingAuth;
    if (req.headers['content-type']) headers['Content-Type'] = req.headers['content-type'];

    const options = {
      method: req.method,
      headers,
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      options.body = JSON.stringify(req.body);
    }

    const resp = await fetch(target, options);
    const status = resp.status;
    const text = await resp.text();
    // try to parse JSON, otherwise return text
    try {
      const json = JSON.parse(text);
      res.status(status).json(json);
    } catch (e) {
      res.status(status).send(text);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`Frontend server running at http://localhost:${PORT}`);
});
