package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.ApplicationConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for token generation, validation, and management.
 * Handles access tokens and refresh tokens with proper security measures.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String USER_ID_CLAIM = "user_id";
    
    private final ApplicationConfig.AppProperties appProperties;

    public JwtService(ApplicationConfig.AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Generate access token for authenticated user.
     * 
     * @param userDetails User details
     * @return JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    /**
     * Generate access token with extra claims.
     * 
     * @param extraClaims Additional claims to include
     * @param userDetails User details
     * @return JWT access token
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, appProperties.getJwt().getExpiration(), ACCESS_TOKEN_TYPE);
    }

    /**
     * Generate refresh token for user.
     * 
     * @param userDetails User details
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return buildToken(claims, userDetails, appProperties.getJwt().getRefreshExpiration(), REFRESH_TOKEN_TYPE);
    }

    /**
     * Extract username from JWT token.
     * 
     * @param token JWT token
     * @return Username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from JWT token.
     * 
     * @param token JWT token
     * @return User ID
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(USER_ID_CLAIM, String.class));
    }

    /**
     * Extract authorities from JWT token.
     * 
     * @param token JWT token
     * @return Authorities string
     */
    public String extractAuthorities(String token) {
        return extractClaim(token, claims -> claims.get(AUTHORITIES_CLAIM, String.class));
    }

    /**
     * Extract token type from JWT token.
     * 
     * @param token JWT token
     * @return Token type
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    /**
     * Extract specific claim from JWT token.
     * 
     * @param token JWT token
     * @param claimsResolver Function to resolve claim
     * @param <T> Claim type
     * @return Extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Check if token is valid for user.
     * 
     * @param token JWT token
     * @param userDetails User details
     * @return True if valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is an access token.
     * 
     * @param token JWT token
     * @return True if access token
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return ACCESS_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is a refresh token.
     * 
     * @param token JWT token
     * @return True if refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired.
     * 
     * @param token JWT token
     * @return True if expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract expiration date from token.
     * 
     * @param token JWT token
     * @return Expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract all claims from JWT token.
     * 
     * @param token JWT token
     * @return Claims
     * @throws JwtException if token is invalid
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("JWT signature does not match: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact is invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Build JWT token with claims and expiration.
     * 
     * @param extraClaims Additional claims
     * @param userDetails User details
     * @param expiration Token expiration time
     * @param tokenType Type of token (access/refresh)
     * @return JWT token
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration,
            String tokenType
    ) {
        Date now = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);

        JwtBuilder builder = Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(AUTHORITIES_CLAIM, userDetails.getAuthorities().toString())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSignInKey(), Jwts.SIG.HS256);

        return builder.compact();
    }

    /**
     * Get signing key for JWT tokens.
     * 
     * @return Secret key for signing
     */
    private SecretKey getSignInKey() {
        try {
            String secret = appProperties.getJwt().getSecret();
            log.debug("JWT secret length: {}", secret.length());
            
            // Try to decode as base64 first, if it fails, use the secret directly as bytes
            byte[] keyBytes;
            try {
                keyBytes = Decoders.BASE64.decode(secret);
                log.debug("Successfully decoded JWT secret as base64");
            } catch (Exception e) {
                log.debug("JWT secret is not base64 encoded, using UTF-8 bytes: {}", e.getMessage());
                keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            
            // Ensure the key is at least 256 bits (32 bytes) for HS256
            if (keyBytes.length < 32) {
                log.debug("JWT secret too short ({}), padding to 32 bytes", keyBytes.length);
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                // Fill remaining bytes with the secret repeated
                for (int i = keyBytes.length; i < 32; i++) {
                    paddedKey[i] = keyBytes[i % keyBytes.length];
                }
                keyBytes = paddedKey;
            }
            
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            log.debug("Successfully created JWT signing key");
            return key;
        } catch (Exception e) {
            log.error("Error creating JWT signing key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create JWT signing key", e);
        }
    }

    /**
     * Get expiration time from token.
     * 
     * @param token JWT token
     * @return Expiration time in milliseconds
     */
    public long getExpirationTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime();
    }

    /**
     * Get remaining time until token expires.
     * 
     * @param token JWT token
     * @return Remaining time in milliseconds
     */
    public long getRemainingTime(String token) {
        long expirationTime = getExpirationTime(token);
        long currentTime = System.currentTimeMillis();
        return Math.max(0, expirationTime - currentTime);
    }
}
