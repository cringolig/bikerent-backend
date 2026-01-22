package com.company.bikerent.billing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.billing.domain.Payment;
import com.company.bikerent.billing.dto.CreatePaymentRequest;
import com.company.bikerent.billing.dto.PaymentDto;
import com.company.bikerent.billing.mapper.PaymentMapper;
import com.company.bikerent.billing.repository.PaymentRepository;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final PaymentMapper paymentMapper;

  @Transactional(readOnly = true)
  public Page<PaymentDto> findAll(Pageable pageable) {
    return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<PaymentDto> findAllByUser(String username, Pageable pageable) {
    if (username != null && !username.isBlank()) {
      User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new EntityNotFoundException(User.class, "username", username));
      return paymentRepository.findAllByUser(user, pageable).map(paymentMapper::toDto);
    }
    return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
  }

  @Transactional(readOnly = true)
  public Page<PaymentDto> findByUserId(Long userId, Pageable pageable) {
    return paymentRepository.findAllByUserId(userId, pageable).map(paymentMapper::toDto);
  }

  /** Create a payment and add balance to user account */
  @Transactional
  public PaymentDto create(CreatePaymentRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    log.info("Creating payment for user: {}, amount: {}", username, request.amount());

    User user =
        userRepository
            .findByIdWithLock(getCurrentUserId())
            .orElseThrow(() -> new EntityNotFoundException(User.class, "username", username));

    // Use domain method to create payment with balance update
    Payment payment = Payment.createPayment(user, request.amount());

    userRepository.save(user);
    Payment saved = paymentRepository.save(payment);

    log.info(
        "Payment created: id={}, user balance updated to: {}", saved.getId(), user.getBalance());
    return paymentMapper.toDto(saved);
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof User) {
      return ((User) authentication.getPrincipal()).getId();
    }
    throw new IllegalStateException("User not authenticated");
  }
}
