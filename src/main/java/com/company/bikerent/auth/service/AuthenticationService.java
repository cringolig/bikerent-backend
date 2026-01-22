package com.company.bikerent.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.auth.domain.RefreshToken;
import com.company.bikerent.auth.dto.LoginRequest;
import com.company.bikerent.auth.dto.RefreshTokenRequest;
import com.company.bikerent.auth.dto.RegisterRequest;
import com.company.bikerent.auth.dto.TokenResponse;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.common.exception.UniqueConstraintViolationException;
import com.company.bikerent.user.domain.Role;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.dto.UserResponse;
import com.company.bikerent.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for authentication operations including login, registration, and token refresh. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  /** Authenticate user and return tokens. */
  @Transactional
  public TokenResponse authenticate(LoginRequest request) {
    log.debug("Authenticating user: {}", maskUsername(request.username()));

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user = findUserByUsername(request.username());
    return generateTokens(user);
  }

  /** Get current user information. */
  @Transactional(readOnly = true)
  public UserResponse getInfo() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException(User.class, "username", username));

    return new UserResponse(username, user.getBalance(), user.getDebt());
  }

  /** Register a new user. */
  @Transactional
  public TokenResponse registerUser(RegisterRequest request) {
    log.info("Registering new user: {}", maskUsername(request.username()));

    if (userRepository.existsByUsername(request.username())) {
      throw new UniqueConstraintViolationException(User.class, "username");
    }

    User user = createUser(request);
    log.info("User registered successfully: {}", maskUsername(user.getUsername()));

    return generateTokens(user);
  }

  /** Refresh access token using refresh token. */
  @Transactional
  public TokenResponse refreshToken(RefreshTokenRequest request) {
    log.debug("Refreshing token");

    RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());
    User user = refreshToken.getUser();

    // Revoke old refresh token
    refreshTokenService.revokeToken(request.refreshToken());

    // Generate new tokens
    return generateTokens(user);
  }

  /** Logout user by revoking refresh token. */
  @Transactional
  public void logout(String refreshToken) {
    refreshTokenService.revokeToken(refreshToken);
    log.debug("User logged out successfully");
  }

  /** Logout user from all devices by revoking all tokens. */
  @Transactional
  public void logoutAll() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException(User.class, "username", username));

    refreshTokenService.revokeAllUserTokens(user.getId());
    log.info("User {} logged out from all devices", maskUsername(username));
  }

  private User createUser(RegisterRequest request) {
    User user =
        User.builder()
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .role(Role.USER)
            .balance(0L)
            .debt(0L)
            .build();

    return userRepository.save(user);
  }

  private TokenResponse generateTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return TokenResponse.of(
        accessToken,
        refreshToken.getToken(),
        jwtService.getAccessTokenExpiration(),
        user.getId(),
        user.getUsername());
  }

  private User findUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException(User.class, "username", username));
  }

  /** Mask username for logging (show first 2 and last 2 characters). */
  private String maskUsername(String username) {
    if (username == null || username.length() <= 4) {
      return "****";
    }
    return username.substring(0, 2) + "***" + username.substring(username.length() - 2);
  }
}
