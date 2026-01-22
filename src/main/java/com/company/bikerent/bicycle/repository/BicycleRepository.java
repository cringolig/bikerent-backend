package com.company.bikerent.bicycle.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.domain.BicycleStatus;

@Repository
public interface BicycleRepository extends JpaRepository<Bicycle, Long> {

  Page<Bicycle> findAllByModel(@NonNull String model, @NonNull Pageable pageable);

  Page<Bicycle> findAllByStationId(@NonNull Long stationId, @NonNull Pageable pageable);

  Page<Bicycle> findAllByMileageGreaterThan(@NonNull Long mileage, @NonNull Pageable pageable);

  Page<Bicycle> findAllByStatus(@NonNull BicycleStatus status, @NonNull Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM Bicycle b WHERE b.id = :id")
  Optional<Bicycle> findByIdWithLock(@Param("id") Long id);

  @Query("SELECT b FROM Bicycle b WHERE b.mileage > :threshold AND b.status = 'AVAILABLE'")
  Page<Bicycle> findBicyclesNeedingService(@Param("threshold") Long threshold, Pageable pageable);
}
