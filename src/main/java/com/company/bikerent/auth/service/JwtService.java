package com.company.bikerent.auth.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.company.bikerent.user.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for JWT token generation and validation. Supports short-lived access tokens and
 * long-lived refresh tokens.
 */
@Slf4j
@Service
public class JwtService {

  private final SecretKey signingKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtService(
      @Value("${jwt.secret}") String secretKey,
      @Value("${jwt.access-expiration:900000}") long accessTokenExpiration,
      @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {

    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException(
          "JWT secret key must be configured via JWT_SECRET environment variable");
    }

    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;

    log.info(
        "JwtService initialized with access token expiration: {}ms, refresh token expiration: {}ms",
        accessTokenExpiration,
        refreshTokenExpiration);
  }

  /** Extract username from token. */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /** Extract a specific claim from token. */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /** Generate an access token for user. */
  public String generateToken(UserDetails userDetails) {
    return generateAccessToken(userDetails);
  }

  /** Generate an access token for user. */
  public String generateAccessToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();

    if (userDetails instanceof User user) {
      claims.put("userId", user.getId());
      claims.put("role", user.getRole().name());
    }

    return buildToken(claims, userDetails, accessTokenExpiration);
  }

  /** Generate an access token with extra claims. */
  public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>(extraClaims);

    if (userDetails instanceof User user) {
      claims.put("userId", user.getId());
      claims.put("role", user.getRole().name());
    }

    return buildToken(claims, userDetails, accessTokenExpiration);
  }

  /** Validate token against user details. */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (ExpiredJwtException e) {
      log.debug("Token is expired for user: {}", userDetails.getUsername());
      return false;
    } catch (JwtException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /** Check if token is expired. */
  public boolean isTokenExpired(String token) {
    try {
      return extractExpiration(token).before(new Date());
    } catch (ExpiredJwtException e) {
      return true;
    }
  }

  /** Get access token expiration time in milliseconds. */
  public long getAccessTokenExpiration() {
    return accessTokenExpiration;
  }

  /** Get refresh token expiration time in milliseconds. */
  public long getRefreshTokenExpiration() {
    return refreshTokenExpiration;
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  private String buildToken(Map<String, Object> claims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(signingKey)
        .compact();
  }
}
