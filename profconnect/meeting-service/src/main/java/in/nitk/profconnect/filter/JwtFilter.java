package com.nitk.meeting.filter;

import com.nitk.meeting.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtFilter implements Filter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        // Allow preflight requests to pass through
         if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        // only protect admin-api endpoints
        if (path.startsWith("/admin-api")) {
            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.getWriter().write("{\"error\":\"Missing Authorization header\"}");
                return;
            }
            String token = auth.substring(7);
            if (!jwtUtil.validateToken(token)) {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
            String role = jwtUtil.getRole(token);
            // Allow requests to fetch a user by email for viewing profiles.
            // GET requests to /admin-api/users/by-email are allowed for any authenticated user (so students can view professors).
            // Also allow GET requests to list/search users when the client explicitly requests only professors (role=PROFF).
            String requestPath = req.getRequestURI();
            if (requestPath != null) {
                if (requestPath.startsWith("/admin-api/users/by-email")) {
                    if ("GET".equalsIgnoreCase(req.getMethod())) {
                        chain.doFilter(request, response);
                        return;
                    }
                    // For POST /admin-api/users/by-email/location, only allow if subject == email (owner)
                    if (requestPath.equals("/admin-api/users/by-email/location") && "POST".equalsIgnoreCase(req.getMethod())) {
                        String requestedEmail = req.getParameter("email");
                        String subject = jwtUtil.getSubject(token);
                        if (requestedEmail != null && subject != null && requestedEmail.equalsIgnoreCase(subject)) {
                            chain.doFilter(request, response);
                            return;
                        } else {
                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.getWriter().write("{\"error\":\"Only the profile owner can update location\"}");
                            return;
                        }
                    }
                    // For other non-GET, keep previous owner-only allowance
                    String requestedEmail = req.getParameter("email");
                    String subject = jwtUtil.getSubject(token);
                    if (requestedEmail != null && subject != null && requestedEmail.equalsIgnoreCase(subject)) {
                        chain.doFilter(request, response);
                        return;
                    }
                }

                // Allow GET /admin-api/users?role=PROFF (list professors) for authenticated non-admins
                if ("GET".equalsIgnoreCase(req.getMethod()) && requestPath.equals("/admin-api/users")) {
                    String roleParam = req.getParameter("role");
                    if (roleParam != null && "PROFF".equalsIgnoreCase(roleParam)) {
                        chain.doFilter(request, response);
                        return;
                    }
                }

                // Allow GET /admin-api/users/search?role=PROFF for authenticated non-admins
                if ("GET".equalsIgnoreCase(req.getMethod()) && requestPath.startsWith("/admin-api/users/search")) {
                    String roleParam = req.getParameter("role");
                    if (roleParam != null && "PROFF".equalsIgnoreCase(roleParam)) {
                        chain.doFilter(request, response);
                        return;
                    }
                }
            }

            if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
                res.setStatus(HttpStatus.FORBIDDEN.value());
                res.getWriter().write("{\"error\":\"Admin role required\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
