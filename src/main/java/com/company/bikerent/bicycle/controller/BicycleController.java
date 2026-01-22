package com.company.bikerent.bicycle.controller;

import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.bicycle.dto.CreateBicycleRequest;
import com.company.bikerent.bicycle.service.BicycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bicycles")
@RequiredArgsConstructor
@Tag(name = "Bicycles", description = "Bicycle management endpoints")
@Validated
public class BicycleController {

    private final BicycleService bicycleService;

    @GetMapping
    @Operation(summary = "Get all bicycles with optional filtering by model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bicycles retrieved successfully")
    })
    public ResponseEntity<Page<BicycleDto>> findAll(
            @RequestParam(required = false) String model,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(bicycleService.findAllWithFilters(model, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bicycle by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bicycle found"),
            @ApiResponse(responseCode = "404", description = "Bicycle not found")
    })
    public ResponseEntity<BicycleDto> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(bicycleService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new bicycle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bicycle created"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Station not found")
    })
    public ResponseEntity<BicycleDto> create(@Valid @RequestBody CreateBicycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bicycleService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bicycle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bicycle deleted"),
            @ApiResponse(responseCode = "404", description = "Bicycle not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete rented bicycle")
    })
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        bicycleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/needs-service")
    @Operation(summary = "Get bicycles that need maintenance")
    public ResponseEntity<Page<BicycleDto>> findNeedingService(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(bicycleService.findBicyclesNeedingService(pageable));
    }
}
