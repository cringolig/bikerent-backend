package com.company.bikerent.station.service;

import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.station.dto.CreateStationRequest;
import com.company.bikerent.station.dto.StationDto;
import com.company.bikerent.station.mapper.StationMapper;
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
public class StationService {

    private final StationMapper stationMapper;
    private final StationRepository stationRepository;

    @Transactional(readOnly = true)
    public Page<StationDto> findAllWithFilters(Long id, Pageable pageable) {
        if (id != null) {
            return stationRepository.findAllById(id, pageable).map(stationMapper::toDto);
        }
        return stationRepository.findAll(pageable).map(stationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public StationDto findById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Station.class, id));
        return stationMapper.toDto(station);
    }

    @Transactional
    public StationDto create(CreateStationRequest request) {
        log.info("Creating new station: {}", request.name());
        
        Station station = stationMapper.toEntity(request);
        station.setAvailableBicycles(0L);
        
        Station saved = stationRepository.save(station);
        log.info("Station created with ID: {}", saved.getId());
        
        return stationMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting station with ID: {}", id);
        
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Station.class, id));
        
        stationRepository.delete(station);
        log.info("Station deleted: {}", id);
    }

    @Transactional
    public void updateAvailableBicycles(Long stationId) {
        Station station = stationRepository.findByIdWithBicycles(stationId)
                .orElseThrow(() -> new EntityNotFoundException(Station.class, stationId));
        station.updateAvailableBicyclesCount();
        stationRepository.save(station);
    }
}
