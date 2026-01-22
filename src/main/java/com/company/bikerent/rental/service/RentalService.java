package com.company.bikerent.rental.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.repository.BicycleRepository;
import com.company.bikerent.common.exception.BusinessException;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.rental.domain.Rental;
import com.company.bikerent.rental.domain.RentalStatus;
import com.company.bikerent.rental.dto.CompleteRentalRequest;
import com.company.bikerent.rental.dto.CreateRentalRequest;
import com.company.bikerent.rental.dto.RentalDto;
import com.company.bikerent.rental.mapper.RentalMapper;
import com.company.bikerent.rental.repository.RentalRepository;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.station.repository.StationRepository;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

  private final RentalRepository rentalRepository;
  private final UserRepository userRepository;
  private final StationRepository stationRepository;
  private final BicycleRepository bicycleRepository;
  private final RentalMapper rentalMapper;

  @Transactional(readOnly = true)
  public Page<RentalDto> findAll(Pageable pageable) {
    return rentalRepository.findAll(pageable).map(rentalMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<RentalDto> findByUserId(Long userId, Pageable pageable) {
    return rentalRepository.findAllByUserId(userId, pageable).map(rentalMapper::toDto);
  }

  @Transactional(readOnly = true)
  public RentalDto findById(Long id) {
    Rental rental =
        rentalRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Rental.class, id));
    return rentalMapper.toDto(rental);
  }

  /** Start a new rental with pessimistic locking to prevent double rentals */
  @Transactional
  public RentalDto create(CreateRentalRequest request) {
    log.info(
        "Starting rental: user={}, bicycle={}, station={}",
        request.userId(),
        request.bicycleId(),
        request.startStationId());

    // Use pessimistic locking to prevent concurrent rental of the same bicycle
    Bicycle bicycle =
        bicycleRepository
            .findByIdWithLock(request.bicycleId())
            .orElseThrow(() -> new EntityNotFoundException(Bicycle.class, request.bicycleId()));

    // Check if bicycle is already in an active rental
    if (rentalRepository.existsByBicycleIdAndStatus(bicycle.getId(), RentalStatus.ACTIVE)) {
      throw new BusinessException("Bicycle is already rented");
    }

    User user =
        userRepository
            .findByIdWithLock(request.userId())
            .orElseThrow(() -> new EntityNotFoundException(User.class, request.userId()));

    Station startStation =
        stationRepository
            .findById(request.startStationId())
            .orElseThrow(
                () -> new EntityNotFoundException(Station.class, request.startStationId()));

    // Use domain method to create rental with validation
    Rental rental = Rental.startRental(user, bicycle, startStation);

    // Save entities
    bicycleRepository.save(bicycle);
    Rental saved = rentalRepository.save(rental);

    log.info("Rental created: id={}", saved.getId());
    return rentalMapper.toDto(saved);
  }

  /** Complete an active rental with pessimistic locking */
  @Transactional
  public RentalDto complete(Long rentalId, CompleteRentalRequest request) {
    log.info("Completing rental: id={}, endStation={}", rentalId, request.endStationId());

    // Lock the rental to prevent concurrent modifications
    Rental rental =
        rentalRepository
            .findByIdWithLock(rentalId)
            .orElseThrow(() -> new EntityNotFoundException(Rental.class, rentalId));

    if (!rental.isActive()) {
      throw new BusinessException("Rental is not active");
    }

    Station endStation =
        stationRepository
            .findById(request.endStationId())
            .orElseThrow(() -> new EntityNotFoundException(Station.class, request.endStationId()));

    // Update mileage before completing
    rental.updateMileage();

    // Complete rental using domain method
    rental.complete(endStation);

    // Charge user for the rental
    User user = rental.getUser();
    user.chargeForRental(rental.getCost());
    userRepository.save(user);

    // Save bicycle and rental
    bicycleRepository.save(rental.getBicycle());
    Rental saved = rentalRepository.save(rental);

    log.info("Rental completed: id={}, cost={}", saved.getId(), saved.getCost());
    return rentalMapper.toDto(saved);
  }

  /** Cancel an active rental */
  @Transactional
  public RentalDto cancel(Long rentalId) {
    log.info("Cancelling rental: id={}", rentalId);

    Rental rental =
        rentalRepository
            .findByIdWithLock(rentalId)
            .orElseThrow(() -> new EntityNotFoundException(Rental.class, rentalId));

    if (!rental.isActive()) {
      throw new BusinessException("Rental is not active");
    }

    rental.cancel();

    bicycleRepository.save(rental.getBicycle());
    Rental saved = rentalRepository.save(rental);

    log.info("Rental cancelled: id={}", saved.getId());
    return rentalMapper.toDto(saved);
  }
}
