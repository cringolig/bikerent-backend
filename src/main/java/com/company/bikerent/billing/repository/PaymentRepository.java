package com.company.bikerent.billing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.company.bikerent.billing.domain.Payment;
import com.company.bikerent.user.domain.User;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Page<Payment> findAllByUser(@NonNull User user, @NonNull Pageable pageable);

  Page<Payment> findAllByUserId(@NonNull Long userId, @NonNull Pageable pageable);
}
