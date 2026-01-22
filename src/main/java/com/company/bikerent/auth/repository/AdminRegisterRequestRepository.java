package com.company.bikerent.auth.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.company.bikerent.auth.domain.AdminRegisterRequest;
import com.company.bikerent.auth.domain.RequestStatus;

@Repository
public interface AdminRegisterRequestRepository extends JpaRepository<AdminRegisterRequest, Long> {

  Page<AdminRegisterRequest> findByStatus(
      @NonNull RequestStatus status, @NonNull Pageable pageable);

  Optional<AdminRegisterRequest> findByUserId(Long userId);

  Optional<AdminRegisterRequest> findByUserIdAndStatus(Long userId, RequestStatus status);

  boolean existsByUserIdAndStatus(Long userId, RequestStatus status);
}
