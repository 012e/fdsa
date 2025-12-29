package huyphmnat.fdsa.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtHelper {

    /**
     * Get the user ID (sub claim) from the current JWT token
     * @return user ID from the JWT's sub claim
     * @throws IllegalStateException if authentication is not available or not JWT-based
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authentication is not JWT-based");
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalStateException("JWT does not contain a valid 'sub' claim");
        }

        log.debug("Current user ID from JWT: {}", sub);
        return sub;
    }

    /**
     * Get a specific claim from the current JWT token
     * @param claimName the name of the claim
     * @return the claim value as String, or null if not present
     */
    public String getClaim(String claimName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt.getClaimAsString(claimName);
    }

    /**
     * Get the preferred username from the JWT token
     * @return preferred username, or null if not available
     */
    public String getPreferredUsername() {
        return getClaim("preferred_username");
    }

    /**
     * Get the email from the JWT token
     * @return email, or null if not available
     */
    public String getEmail() {
        return getClaim("email");
    }
}

