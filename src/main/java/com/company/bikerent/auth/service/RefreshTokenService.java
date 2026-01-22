package com.company.bikerent.auth.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.auth.domain.RefreshToken;
import com.company.bikerent.auth.repository.RefreshTokenRepository;
import com.company.bikerent.common.exception.BusinessException;
import com.company.bikerent.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for managing refresh tokens. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtService jwtService;

  /** Create a new refresh token for a user. */
  @Transactional
  public RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken = RefreshToken.create(user, jwtService.getRefreshTokenExpiration());
    RefreshToken saved = refreshTokenRepository.save(refreshToken);

    log.debug("Created refresh token for user: {}", user.getUsername());
    return saved;
  }

  /**
   * Validate a refresh token and return it if valid.
   *
   * @throws BusinessException if token is invalid or expired
   */
  @Transactional(readOnly = true)
  public RefreshToken validateRefreshToken(String token) {
    return refreshTokenRepository
        .findValidToken(token, Instant.now())
        .orElseThrow(() -> new BusinessException("Invalid or expired refresh token"));
  }

  /** Revoke a specific refresh token. */
  @Transactional
  public void revokeToken(String token) {
    refreshTokenRepository
        .findByToken(token)
        .ifPresent(
            refreshToken -> {
              refreshToken.revoke();
              refreshTokenRepository.save(refreshToken);
              log.debug("Revoked refresh token for user: {}", refreshToken.getUser().getUsername());
            });
  }

  /** Revoke all refresh tokens for a user (e.g., on logout from all devices). */
  @Transactional
  public void revokeAllUserTokens(Long userId) {
    int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
    log.debug("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
  }

  /** Scheduled task to clean up expired tokens. Runs every hour. */
  @Scheduled(fixedRate = 3600000)
  @Transactional
  public void cleanupExpiredTokens() {
    int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());
    if (deletedCount > 0) {
      log.info("Cleaned up {} expired/revoked refresh tokens", deletedCount);
    }
  }
}
