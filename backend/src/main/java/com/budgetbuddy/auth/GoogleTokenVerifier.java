package com.budgetbuddy.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.budgetbuddy.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Verifies Google ID tokens server-side: signature against Google's public
 * JWKS, issuer, audience (our client ID) and expiry - as required by
 * TECHNICAL_SPEC section 6. A browser-submitted email is never accepted
 * as proof of identity.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> VALID_ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final RestClient restClient;
    private final ReentrantLock jwksLock = new ReentrantLock();

    private volatile JwkSet jwkSet;

    public GoogleTokenVerifier(@Value("${budgetbuddy.google.client-id:}") String clientId,
                               RestClient.Builder restClientBuilder) {
        this.clientId = clientId;
        this.restClient = restClientBuilder.build();
    }

    public GoogleIdentity verify(String credential) {
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    com.budgetbuddy.common.error.ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Google Sign-In is not configured on the server.");
        }
        try {
            JwkSet keys = fetchJwks();
            DecodedJWT jwt = JWT.require(Algorithm.RSA256((RSAPublicKey) keys.keyById(jwtDecode(credential).getKeyId())))
                    .withIssuer(VALID_ISSUERS.toArray(String[]::new))
                    .acceptLeeway(60)
                    .build()
                    .verify(credential);
            verifyAudience(jwt);
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                throw ApiException.badRequest("Google credential is missing a subject.");
            }
            String email = jwt.getClaim("email").asString();
            Boolean emailVerified = jwt.getClaim("email_verified").asBoolean();
            if (email == null || !Boolean.TRUE.equals(emailVerified)) {
                throw ApiException.badRequest("Google account email is not verified.");
            }
            String name = jwt.getClaim("name").asString();
            return new GoogleIdentity(subject, email, name != null ? name : email);
        } catch (JWTVerificationException e) {
            throw ApiException.badRequest("Google credential verification failed.");
        }
    }

    private DecodedJWT jwtDecode(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            throw ApiException.badRequest("Google credential is malformed.");
        }
    }

    private void verifyAudience(DecodedJWT jwt) {
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(clientId)) {
            throw ApiException.badRequest("Google credential was issued for a different client.");
        }
    }

    private JwkSet fetchJwks() {
        JwkSet cached = jwkSet;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        jwksLock.lock();
        try {
            if (jwkSet == null || jwkSet.isEmpty()) {
                Map<String, Object> response = restClient.get()
                        .uri(URI.create(JWKS_URL))
                        .retrieve()
                        .body(Map.class);
                if (response == null || !response.containsKey("keys")) {
                    throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                            com.budgetbuddy.common.error.ErrorCode.CHAT_UPSTREAM_ERROR,
                            "Unable to load Google signing keys.");
                }
                jwkSet = JwkSet.from(response);
            }
            return jwkSet;
        } finally {
            jwksLock.unlock();
        }
    }

    /**
     * Verified Google identity claims.
     */
    public record GoogleIdentity(String subject, String email, String name) {
    }
}
