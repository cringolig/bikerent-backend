package com.company.bikerent.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import com.company.bikerent.user.domain.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a refresh token for JWT authentication. Refresh tokens allow users to obtain
 * new access tokens without re-authenticating.
 */
@Entity
@Table(
    name = "refresh_token",
    indexes = {
      @Index(name = "idx_refresh_token_user", columnList = "user_id"),
      @Index(name = "idx_refresh_token_expires", columnList = "expires_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "User is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @NotBlank(message = "Token is required")
  @Column(name = "token", nullable = false, unique = true)
  private String token;

  @NotNull(message = "Expiration date is required")
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "revoked", nullable = false)
  @Builder.Default
  private boolean revoked = false;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  /** Check if the token is expired. */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /** Check if the token is valid (not expired and not revoked). */
  public boolean isValid() {
    return !isExpired() && !revoked;
  }

  /** Revoke this token. */
  public void revoke() {
    this.revoked = true;
    this.revokedAt = Instant.now();
  }

  /**
   * Create a new refresh token for a user.
   *
   * @param user the user to create the token for
   * @param expirationMs token expiration time in milliseconds
   * @return a new RefreshToken instance
   */
  public static RefreshToken create(User user, long expirationMs) {
    return RefreshToken.builder()
        .user(user)
        .token(UUID.randomUUID().toString())
        .expiresAt(Instant.now().plusMillis(expirationMs))
        .revoked(false)
        .build();
  }
}
