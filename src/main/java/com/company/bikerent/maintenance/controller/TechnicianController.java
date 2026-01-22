package com.company.bikerent.maintenance.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.company.bikerent.maintenance.dto.CreateTechnicianRequest;
import com.company.bikerent.maintenance.dto.TechnicianDto;
import com.company.bikerent.maintenance.service.TechnicianService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/technicians")
@RequiredArgsConstructor
@Tag(name = "Technicians", description = "Technician management endpoints")
@Validated
public class TechnicianController {

  private final TechnicianService technicianService;

  @GetMapping
  @Operation(summary = "Get all technicians")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Technicians retrieved successfully")
      })
  public ResponseEntity<Page<TechnicianDto>> findAll(@PageableDefault Pageable pageable) {
    return ResponseEntity.ok(technicianService.findAll(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get technician by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Technician found"),
        @ApiResponse(responseCode = "404", description = "Technician not found")
      })
  public ResponseEntity<TechnicianDto> findById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(technicianService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create a new technician")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Technician created"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
      })
  public ResponseEntity<TechnicianDto> create(@Valid @RequestBody CreateTechnicianRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(technicianService.create(request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a technician")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Technician deleted"),
        @ApiResponse(responseCode = "404", description = "Technician not found")
      })
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    technicianService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
