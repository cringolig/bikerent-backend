package com.company.bikerent.billing.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.company.bikerent.billing.dto.CreatePaymentRequest;
import com.company.bikerent.billing.dto.PaymentDto;
import com.company.bikerent.billing.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
@Validated
public class PaymentController {

  private final PaymentService paymentService;

  @GetMapping
  @Operation(summary = "Get all payments with optional username filter")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Payments retrieved successfully")})
  public ResponseEntity<Page<PaymentDto>> findAll(
      @RequestParam(required = false) String username, @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(paymentService.findAllByUser(username, pageable));
  }

  @GetMapping("/user/{userId}")
  @Operation(summary = "Get payments by user ID")
  public ResponseEntity<Page<PaymentDto>> findByUserId(
      @PathVariable @Positive Long userId, @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(paymentService.findByUserId(userId, pageable));
  }

  @PostMapping
  @Operation(summary = "Create a new payment (add balance)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<PaymentDto> create(@Valid @RequestBody CreatePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
  }
}
