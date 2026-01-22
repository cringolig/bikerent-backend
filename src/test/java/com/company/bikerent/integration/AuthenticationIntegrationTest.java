package com.company.bikerent.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.company.bikerent.auth.dto.LoginRequest;
import com.company.bikerent.auth.dto.RegisterRequest;
import com.company.bikerent.auth.dto.TokenResponse;
import com.company.bikerent.user.domain.Role;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuthenticationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("Should register new user successfully")
  void shouldRegisterNewUser() throws Exception {
    RegisterRequest request = new RegisterRequest("newuser1", "password123");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.refresh_token").exists())
        .andExpect(jsonPath("$.username").value("newuser1"));

    assertThat(userRepository.existsByUsername("newuser1")).isTrue();
  }

  @Test
  @DisplayName("Should reject duplicate username registration")
  void shouldRejectDuplicateUsername() throws Exception {
    // First registration
    RegisterRequest request = new RegisterRequest("duplicate", "password123");
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Second registration with same username
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Should login with valid credentials")
  void shouldLoginWithValidCredentials() throws Exception {
    // Create user first
    User user =
        User.builder()
            .username("logintest")
            .password(passwordEncoder.encode("password123"))
            .role(Role.USER)
            .balance(0L)
            .debt(0L)
            .build();
    userRepository.save(user);

    LoginRequest request = new LoginRequest("logintest", "password123");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.refresh_token").exists())
        .andExpect(jsonPath("$.username").value("logintest"));
  }

  @Test
  @DisplayName("Should reject login with invalid password")
  void shouldRejectInvalidPassword() throws Exception {
    // Create user first
    User user =
        User.builder()
            .username("invalidpwd")
            .password(passwordEncoder.encode("correctpassword"))
            .role(Role.USER)
            .balance(0L)
            .debt(0L)
            .build();
    userRepository.save(user);

    LoginRequest request = new LoginRequest("invalidpwd", "wrongpassword");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should get user info with valid token")
  void shouldGetUserInfoWithValidToken() throws Exception {
    // Register user and get token
    RegisterRequest registerRequest = new RegisterRequest("infotest", "password123");
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    TokenResponse tokenResponse =
        objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);

    // Get user info with token
    mockMvc
        .perform(
            get("/api/v1/auth/info")
                .header("Authorization", "Bearer " + tokenResponse.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("infotest"))
        .andExpect(jsonPath("$.balance").value(0))
        .andExpect(jsonPath("$.debt").value(0));
  }

  @Test
  @DisplayName("Should reject request without token")
  void shouldRejectRequestWithoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/auth/info")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should validate registration request")
  void shouldValidateRegistrationRequest() throws Exception {
    RegisterRequest request = new RegisterRequest("ab", "pw"); // Too short

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors").exists());
  }
}
