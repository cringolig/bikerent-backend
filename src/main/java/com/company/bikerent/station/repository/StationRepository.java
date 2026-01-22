package com.company.bikerent.station.repository;

import com.company.bikerent.station.domain.Station;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    
    Page<Station> findAllById(@NonNull Long id, @NonNull Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Station s WHERE s.id = :id")
    Optional<Station> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT s FROM Station s LEFT JOIN FETCH s.bicycles WHERE s.id = :id")
    Optional<Station> findByIdWithBicycles(@Param("id") Long id);
}
