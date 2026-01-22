package com.company.bikerent.maintenance.repository;

import com.company.bikerent.maintenance.domain.Repair;
import com.company.bikerent.maintenance.domain.RepairStatus;
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
public interface RepairRepository extends JpaRepository<Repair, Long> {
    
    Page<Repair> findAllByStatus(RepairStatus status, Pageable pageable);
    
    Page<Repair> findAllByTechnicianId(Long technicianId, Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repair r WHERE r.id = :id")
    Optional<Repair> findByIdWithLock(@Param("id") Long id);
    
    boolean existsByBicycleIdAndStatus(Long bicycleId, RepairStatus status);
}
