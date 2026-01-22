package com.company.bikerent.maintenance.controller;

import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.maintenance.dto.CreateRepairRequest;
import com.company.bikerent.maintenance.dto.RepairDto;
import com.company.bikerent.maintenance.service.RepairService;
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
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
@Tag(name = "Repairs", description = "Repair management endpoints")
@Validated
public class RepairController {

    private final RepairService repairService;

    @GetMapping
    @Operation(summary = "Get all repairs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repairs retrieved successfully")
    })
    public ResponseEntity<Page<RepairDto>> findAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(repairService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get repair by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair found"),
            @ApiResponse(responseCode = "404", description = "Repair not found")
    })
    public ResponseEntity<RepairDto> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(repairService.findById(id));
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Get bicycles scheduled for maintenance")
    public ResponseEntity<Page<BicycleDto>> findScheduled(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(repairService.findScheduledForMaintenance(pageable));
    }

    @PostMapping
    @Operation(summary = "Start a new repair")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Repair started"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Bicycle or technician not found"),
            @ApiResponse(responseCode = "409", description = "Bicycle already under repair"),
            @ApiResponse(responseCode = "422", description = "Bicycle is not available")
    })
    public ResponseEntity<RepairDto> create(@Valid @RequestBody CreateRepairRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repairService.create(request));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete a repair")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repair completed"),
            @ApiResponse(responseCode = "404", description = "Repair not found"),
            @ApiResponse(responseCode = "422", description = "Repair is not in progress")
    })
    public ResponseEntity<RepairDto> complete(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(repairService.complete(id));
    }
}
