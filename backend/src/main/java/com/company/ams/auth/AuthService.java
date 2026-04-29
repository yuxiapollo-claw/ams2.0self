package com.company.ams.auth;

import com.company.ams.common.persistence.UserRepository;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Authentication authenticate(LoginRequest request) {
        UserRepository.AuthUserRecord user = userRepository.findByLoginName(request.loginName())
                .filter(candidate -> "ENABLED".equals(candidate.accountStatus()))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        List<String> roles = user.systemAdmin()
                ? List.of("SYS_ADMIN", "USER")
                : List.of("USER");
        UserPrincipal principal = new UserPrincipal(
                user.id(),
                user.loginName(),
                user.userName(),
                user.systemAdmin(),
                roles);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.roles().stream().map(SimpleGrantedAuthority::new).toList());
    }

    static class InvalidCredentialsException extends RuntimeException {
        InvalidCredentialsException() {
            super("Bad credentials");
        }
    }
}
