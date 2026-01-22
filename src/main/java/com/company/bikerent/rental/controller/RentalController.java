package com.company.bikerent.rental.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.company.bikerent.rental.dto.CompleteRentalRequest;
import com.company.bikerent.rental.dto.CreateRentalRequest;
import com.company.bikerent.rental.dto.RentalDto;
import com.company.bikerent.rental.service.RentalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
@Tag(name = "Rentals", description = "Rental management endpoints")
@Validated
public class RentalController {

  private final RentalService rentalService;

  @GetMapping
  @Operation(summary = "Get all rentals")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Rentals retrieved successfully")})
  public ResponseEntity<Page<RentalDto>> findAll(@PageableDefault Pageable pageable) {
    return ResponseEntity.ok(rentalService.findAll(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get rental by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Rental found"),
        @ApiResponse(responseCode = "404", description = "Rental not found")
      })
  public ResponseEntity<RentalDto> findById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(rentalService.findById(id));
  }

  @GetMapping("/user/{userId}")
  @Operation(summary = "Get rentals by user ID")
  public ResponseEntity<Page<RentalDto>> findByUserId(
      @PathVariable @Positive Long userId, @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(rentalService.findByUserId(userId, pageable));
  }

  @PostMapping
  @Operation(summary = "Start a new rental")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Rental started"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User, bicycle or station not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Bicycle already rented or user cannot rent"),
        @ApiResponse(responseCode = "422", description = "Business rule violation")
      })
  public ResponseEntity<RentalDto> create(@Valid @RequestBody CreateRentalRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(rentalService.create(request));
  }

  @PutMapping("/{id}/complete")
  @Operation(summary = "Complete an active rental")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Rental completed"),
        @ApiResponse(responseCode = "404", description = "Rental or station not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Rental already completed or concurrent modification"),
        @ApiResponse(responseCode = "422", description = "Rental is not active")
      })
  public ResponseEntity<RentalDto> complete(
      @PathVariable @Positive Long id, @Valid @RequestBody CompleteRentalRequest request) {
    return ResponseEntity.ok(rentalService.complete(id, request));
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel an active rental")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Rental cancelled"),
        @ApiResponse(responseCode = "404", description = "Rental not found"),
        @ApiResponse(responseCode = "422", description = "Rental is not active")
      })
  public ResponseEntity<RentalDto> cancel(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(rentalService.cancel(id));
  }
}
