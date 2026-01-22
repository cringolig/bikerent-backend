package com.company.bikerent.rental.repository;

import com.company.bikerent.rental.domain.Rental;
import com.company.bikerent.rental.domain.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    
    Page<Rental> findAllByUserId(Long userId, Pageable pageable);
    
    Page<Rental> findAllByStatus(RentalStatus status, Pageable pageable);
    
    Optional<Rental> findByBicycleIdAndStatus(Long bicycleId, RentalStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rental r WHERE r.id = :id")
    Optional<Rental> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT r FROM Rental r WHERE r.user.id = :userId AND r.status = 'ACTIVE'")
    Optional<Rental> findActiveRentalByUserId(@Param("userId") Long userId);
    
    boolean existsByBicycleIdAndStatus(Long bicycleId, RentalStatus status);
}
