package com.company.bikerent.station.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.bicycle.service.BicycleService;
import com.company.bikerent.station.dto.CreateStationRequest;
import com.company.bikerent.station.dto.StationDto;
import com.company.bikerent.station.service.StationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@Tag(name = "Stations", description = "Station management endpoints")
@Validated
public class StationController {

  private final StationService stationService;
  private final BicycleService bicycleService;

  @GetMapping
  @Operation(summary = "Get all stations with optional filtering")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Stations retrieved successfully")})
  public ResponseEntity<Page<StationDto>> findAll(
      @RequestParam(required = false) @Positive Long id, @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(stationService.findAllWithFilters(id, pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get station by ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Station found"),
        @ApiResponse(responseCode = "404", description = "Station not found")
      })
  public ResponseEntity<StationDto> findById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(stationService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create a new station")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Station created"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
      })
  public ResponseEntity<StationDto> create(@Valid @RequestBody CreateStationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stationService.create(request));
  }

  @GetMapping("/{id}/bicycles")
  @Operation(summary = "Get all bicycles at a station")
  public ResponseEntity<Page<BicycleDto>> findBicycles(
      @PathVariable @Positive Long id, @PageableDefault Pageable pageable) {
    return ResponseEntity.ok(bicycleService.findAllByStationId(id, pageable));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a station")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Station deleted"),
        @ApiResponse(responseCode = "404", description = "Station not found")
      })
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    stationService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
