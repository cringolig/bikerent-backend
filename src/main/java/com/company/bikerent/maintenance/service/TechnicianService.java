package com.company.bikerent.maintenance.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.maintenance.domain.Technician;
import com.company.bikerent.maintenance.dto.CreateTechnicianRequest;
import com.company.bikerent.maintenance.dto.TechnicianDto;
import com.company.bikerent.maintenance.mapper.TechnicianMapper;
import com.company.bikerent.maintenance.repository.TechnicianRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicianService {

  private final TechnicianRepository technicianRepository;
  private final TechnicianMapper technicianMapper;

  @Transactional(readOnly = true)
  public Page<TechnicianDto> findAll(Pageable pageable) {
    return technicianRepository.findAll(pageable).map(technicianMapper::toDto);
  }

  @Transactional(readOnly = true)
  public TechnicianDto findById(Long id) {
    Technician technician =
        technicianRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Technician.class, id));
    return technicianMapper.toDto(technician);
  }

  @Transactional
  public TechnicianDto create(CreateTechnicianRequest request) {
    log.info("Creating new technician: {}", request.name());

    Technician technician = technicianMapper.toEntity(request);
    Technician saved = technicianRepository.save(technician);

    log.info("Technician created with ID: {}", saved.getId());
    return technicianMapper.toDto(saved);
  }

  @Transactional
  public void delete(Long id) {
    log.info("Deleting technician with ID: {}", id);

    Technician technician =
        technicianRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Technician.class, id));

    technicianRepository.delete(technician);
    log.info("Technician deleted: {}", id);
  }
}
