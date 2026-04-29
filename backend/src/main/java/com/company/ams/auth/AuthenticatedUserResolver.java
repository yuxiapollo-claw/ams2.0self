package com.company.ams.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthenticatedUserResolver {
    private AuthenticatedUserResolver() {}

    public static UserPrincipal resolve(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        String loginName = extractLoginName(principal);
        if (loginName == null || loginName.isBlank()) {
            return null;
        }

        List<String> roles = normalizeRoles(authentication.getAuthorities());
        boolean systemAdmin = roles.contains("SYS_ADMIN") || "admin".equalsIgnoreCase(loginName);
        if (roles.isEmpty()) {
            roles = systemAdmin ? List.of("SYS_ADMIN", "USER") : List.of("USER");
        }
        return new UserPrincipal(0L, loginName, loginName, systemAdmin, roles);
    }

    public static boolean hasNativePrincipal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof UserPrincipal;
    }

    private static String extractLoginName(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String value && !"anonymousUser".equalsIgnoreCase(value)) {
            return value;
        }
        return null;
    }

    private static List<String> normalizeRoles(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .distinct()
                .toList();
    }
}
