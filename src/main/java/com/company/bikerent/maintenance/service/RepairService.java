package com.company.bikerent.maintenance.service;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.bicycle.mapper.BicycleMapper;
import com.company.bikerent.bicycle.repository.BicycleRepository;
import com.company.bikerent.common.exception.BusinessException;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.maintenance.domain.Repair;
import com.company.bikerent.maintenance.domain.RepairStatus;
import com.company.bikerent.maintenance.domain.Technician;
import com.company.bikerent.maintenance.dto.CreateRepairRequest;
import com.company.bikerent.maintenance.dto.RepairDto;
import com.company.bikerent.maintenance.mapper.RepairMapper;
import com.company.bikerent.maintenance.repository.RepairRepository;
import com.company.bikerent.maintenance.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairService {

    private static final Long SERVICE_THRESHOLD_MILEAGE = 50L;

    private final RepairRepository repairRepository;
    private final BicycleRepository bicycleRepository;
    private final TechnicianRepository technicianRepository;
    private final RepairMapper repairMapper;
    private final BicycleMapper bicycleMapper;

    @Transactional(readOnly = true)
    public Page<RepairDto> findAll(Pageable pageable) {
        return repairRepository.findAll(pageable).map(repairMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RepairDto findById(Long id) {
        Repair repair = repairRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Repair.class, id));
        return repairMapper.toDto(repair);
    }

    @Transactional(readOnly = true)
    public Page<BicycleDto> findScheduledForMaintenance(Pageable pageable) {
        return bicycleRepository.findBicyclesNeedingService(SERVICE_THRESHOLD_MILEAGE, pageable)
                .map(bicycleMapper::toDto);
    }

    /**
     * Start a new repair with pessimistic locking to prevent concurrent repairs
     */
    @Transactional
    public RepairDto create(CreateRepairRequest request) {
        log.info("Starting repair: bicycle={}, technician={}", 
                request.bicycleId(), request.technicianId());

        // Use pessimistic locking to prevent concurrent repairs
        Bicycle bicycle = bicycleRepository.findByIdWithLock(request.bicycleId())
                .orElseThrow(() -> new EntityNotFoundException(Bicycle.class, request.bicycleId()));

        // Check if bicycle already has an active repair
        if (repairRepository.existsByBicycleIdAndStatus(bicycle.getId(), RepairStatus.IN_PROGRESS)) {
            throw new BusinessException("Bicycle already has an active repair");
        }

        Technician technician = technicianRepository.findById(request.technicianId())
                .orElseThrow(() -> new EntityNotFoundException(Technician.class, request.technicianId()));

        // Use domain method to create repair with validation
        Repair repair = Repair.startRepair(bicycle, technician, request.description());

        // Save entities
        bicycleRepository.save(bicycle);
        Repair saved = repairRepository.save(repair);

        log.info("Repair created: id={}", saved.getId());
        return repairMapper.toDto(saved);
    }

    /**
     * Complete an active repair with pessimistic locking
     */
    @Transactional
    public RepairDto complete(Long repairId) {
        log.info("Completing repair: id={}", repairId);

        Repair repair = repairRepository.findByIdWithLock(repairId)
                .orElseThrow(() -> new EntityNotFoundException(Repair.class, repairId));

        if (!repair.isInProgress()) {
            throw new BusinessException("Repair is not in progress");
        }

        // Complete repair using domain method
        repair.complete();

        // Save bicycle and repair
        bicycleRepository.save(repair.getBicycle());
        Repair saved = repairRepository.save(repair);

        log.info("Repair completed: id={}", saved.getId());
        return repairMapper.toDto(saved);
    }
}
