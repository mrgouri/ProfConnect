package in.nitk.crud.filter;

import in.nitk.crud.util.JwtUtil;
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
            // Allow non-admin users to fetch their own profile via the by-email endpoint
            String requestPath = req.getRequestURI();
            if (requestPath != null && requestPath.startsWith("/admin-api/users/by-email")) {
                String requestedEmail = req.getParameter("email");
                String subject = jwtUtil.getSubject(token);
                if (requestedEmail != null && subject != null && requestedEmail.equalsIgnoreCase(subject)) {
                    // allow the request through for the owner
                    chain.doFilter(request, response);
                    return;
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
