package com.company.bikerent.bicycle.service;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.bicycle.dto.CreateBicycleRequest;
import com.company.bikerent.bicycle.mapper.BicycleMapper;
import com.company.bikerent.bicycle.repository.BicycleRepository;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BicycleService {

    private final BicycleRepository bicycleRepository;
    private final StationRepository stationRepository;
    private final BicycleMapper bicycleMapper;

    @Transactional(readOnly = true)
    public Page<BicycleDto> findAllWithFilters(String model, Pageable pageable) {
        if (model != null && !model.isBlank()) {
            return bicycleRepository.findAllByModel(model, pageable).map(bicycleMapper::toDto);
        }
        return bicycleRepository.findAll(pageable).map(bicycleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BicycleDto> findAllByStationId(Long stationId, Pageable pageable) {
        return bicycleRepository.findAllByStationId(stationId, pageable).map(bicycleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public BicycleDto findById(Long id) {
        Bicycle bicycle = bicycleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Bicycle.class, id));
        return bicycleMapper.toDto(bicycle);
    }

    @Transactional
    public BicycleDto create(CreateBicycleRequest request) {
        log.info("Creating new bicycle: model={}, station={}", request.model(), request.stationId());
        
        Station station = stationRepository.findById(request.stationId())
                .orElseThrow(() -> new EntityNotFoundException(Station.class, request.stationId()));
        
        Bicycle bicycle = bicycleMapper.toEntity(request);
        bicycle.setStation(station);
        bicycle.setMileage(0L);
        
        Bicycle saved = bicycleRepository.save(bicycle);
        log.info("Bicycle created with ID: {}", saved.getId());
        
        return bicycleMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting bicycle with ID: {}", id);
        
        Bicycle bicycle = bicycleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Bicycle.class, id));
        
        if (bicycle.isRented()) {
            throw new IllegalStateException("Cannot delete a rented bicycle");
        }
        
        bicycleRepository.delete(bicycle);
        log.info("Bicycle deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<BicycleDto> findBicyclesNeedingService(Pageable pageable) {
        return bicycleRepository.findBicyclesNeedingService(50L, pageable)
                .map(bicycleMapper::toDto);
    }
}
