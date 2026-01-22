package com.company.bikerent.auth.controller;

import com.company.bikerent.auth.dto.LoginRequest;
import com.company.bikerent.auth.dto.RefreshTokenRequest;
import com.company.bikerent.auth.dto.RegisterRequest;
import com.company.bikerent.auth.dto.TokenResponse;
import com.company.bikerent.auth.service.AuthenticationService;
import com.company.bikerent.common.dto.ErrorResponse;
import com.company.bikerent.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns authentication tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TokenResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration details",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    value = "{\"username\": \"john_doe\", \"password\": \"SecurePass123!\"}"
                            )
                    )
            )
            @Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.registerUser(registerRequest));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user credentials and returns access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    value = "{\"username\": \"john_doe\", \"password\": \"SecurePass123!\"}"
                            )
                    )
            )
            @Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.authenticate(loginRequest));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Uses a valid refresh token to obtain new access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Refresh token expired or revoked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TokenResponse> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(
                                    value = "{\"refresh_token\": \"550e8400-e29b-41d4-a716-446655440000\"}"
                            )
                    )
            )
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Revokes the provided refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logged out successfully")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(
            summary = "Logout from all devices",
            description = "Revokes all refresh tokens for the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logged out from all devices"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> logoutAll() {
        authenticationService.logoutAll();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/info")
    @Operation(
            summary = "Get current user information",
            description = "Returns information about the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserResponse> info() {
        return ResponseEntity.ok(authenticationService.getInfo());
    }
}
