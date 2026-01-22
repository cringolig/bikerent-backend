package com.company.bikerent.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.company.bikerent.auth.domain.RefreshToken;

/** Repository for RefreshToken entity operations. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** Find a valid (non-revoked, non-expired) refresh token by token string. */
  @Query(
      "SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiresAt > :now")
  Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") Instant now);

  /** Find a refresh token by token string. */
  Optional<RefreshToken> findByToken(String token);

  /** Revoke all tokens for a specific user. */
  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revoked = false")
  int revokeAllUserTokens(@Param("userId") Long userId, @Param("now") Instant now);

  /** Delete all expired or revoked tokens (cleanup). */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
  int deleteExpiredTokens(@Param("now") Instant now);

  /** Check if a user has any active tokens. */
  boolean existsByUserIdAndRevokedFalseAndExpiresAtAfter(Long userId, Instant now);
}
