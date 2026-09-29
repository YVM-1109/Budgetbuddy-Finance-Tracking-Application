package com.budgetbuddy.auth;

import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication use cases. Enforces unique emails, hashes passwords with
 * BCrypt, and verifies Google credentials server-side before linking.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(User.AuthProvider.LOCAL);
        user = userRepository.save(user);
        return new AuthResponse(jwtService.generate(user.getId()), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.badRequest("Invalid credentials."));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Same error for unknown email and wrong password (no account enumeration).
            throw ApiException.badRequest("Invalid credentials.");
        }
        return new AuthResponse(jwtService.generate(user.getId()), UserResponse.from(user));
    }

    @Transactional
    public AuthResponse googleSignIn(String credential) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(credential);
        User user = userRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            // Link by verified email if the user already registered locally.
            user = userRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
            if (user != null) {
                user.setGoogleSubject(identity.subject());
                if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
                    user.setAuthProvider(User.AuthProvider.LINKED);
                }
            } else {
                user = new User();
                user.setName(identity.name());
                user.setEmail(identity.email().toLowerCase());
                user.setGoogleSubject(identity.subject());
                user.setAuthProvider(User.AuthProvider.GOOGLE);
            }
            user = userRepository.save(user);
        }
        return new AuthResponse(jwtService.generate(user.getId()), UserResponse.from(user));
    }
}
