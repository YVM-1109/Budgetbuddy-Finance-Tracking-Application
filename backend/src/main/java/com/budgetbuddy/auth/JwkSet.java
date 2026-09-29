package com.budgetbuddy.auth;

import com.budgetbuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minimal JWKS representation supporting RSA keys (what Google publishes).
 */
public record JwkSet(List<Jwk> keys) {

    public record Jwk(String kid, String n, String e) {
    }

    public static JwkSet from(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawKeys = (List<Map<String, Object>>) body.get("keys");
        if (rawKeys == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    com.budgetbuddy.common.error.ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Unable to load Google signing keys.");
        }
        List<Jwk> keys = rawKeys.stream()
                .filter(k -> "RSA".equals(k.get("kty")))
                .map(k -> new Jwk((String) k.get("kid"), (String) k.get("n"), (String) k.get("e")))
                .collect(Collectors.toList());
        return new JwkSet(keys);
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public PublicKey keyById(String kid) {
        return keys.stream()
                .filter(k -> k.kid().equals(kid))
                .findFirst()
                .map(JwkSet::toRsaPublicKey)
                .orElseThrow(() -> ApiException.badRequest("Google credential signed with an unknown key."));
    }

    private static RSAPublicKey toRsaPublicKey(Jwk jwk) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.e()));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    com.budgetbuddy.common.error.ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Unable to use Google signing keys.");
        }
    }
}
