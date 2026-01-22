package com.company.bikerent.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.company.bikerent.auth.dto.AdminRegisterRequestDto;
import com.company.bikerent.auth.dto.AdminRegisterRequestRequest;
import com.company.bikerent.auth.dto.RoleResponse;
import com.company.bikerent.auth.service.AdminRegisterRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Requests", description = "Admin registration request management")
@Validated
public class AdminRegisterRequestController {

  private final AdminRegisterRequestService adminRegisterRequestService;

  @GetMapping("/pending")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all pending admin requests")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Pending requests retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied")
      })
  public ResponseEntity<Page<AdminRegisterRequestDto>> getPendingRequests(
      @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(adminRegisterRequestService.getPendingRequests(pageable));
  }

  @GetMapping("/pending/user/{userId}")
  @Operation(summary = "Get admin request by user ID")
  public ResponseEntity<AdminRegisterRequestDto> getRequestByUserId(
      @PathVariable @Positive Long userId) {
    return ResponseEntity.ok(adminRegisterRequestService.getRequestByUserId(userId));
  }

  @GetMapping("/role")
  @Operation(summary = "Get current user's role")
  public ResponseEntity<RoleResponse> getCurrentUserRoles() {
    return ResponseEntity.ok(adminRegisterRequestService.getRoleResponse());
  }

  @PostMapping("/create")
  @Operation(summary = "Create a new admin request")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Request created"),
        @ApiResponse(responseCode = "409", description = "Request already pending for this user")
      })
  public ResponseEntity<AdminRegisterRequestDto> createAdminRequest(
      @Valid @RequestBody AdminRegisterRequestRequest requestDto) {

    if (adminRegisterRequestService.isRequestPendingByUsername(requestDto.username())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    AdminRegisterRequestDto createdRequest =
        adminRegisterRequestService.createAdminRequest(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
  }

  @PatchMapping("/pending/{requestId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Approve an admin request")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Request approved"),
        @ApiResponse(responseCode = "404", description = "Request not found"),
        @ApiResponse(responseCode = "409", description = "Request already processed")
      })
  public ResponseEntity<Void> approveRequest(@PathVariable @Positive Long requestId) {
    adminRegisterRequestService.approveAdminRequest(requestId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/pending/{requestId}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Reject an admin request")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Request rejected"),
        @ApiResponse(responseCode = "404", description = "Request not found"),
        @ApiResponse(responseCode = "409", description = "Request already processed")
      })
  public ResponseEntity<Void> rejectRequest(@PathVariable @Positive Long requestId) {
    adminRegisterRequestService.rejectAdminRequest(requestId);
    return ResponseEntity.noContent().build();
  }
}
