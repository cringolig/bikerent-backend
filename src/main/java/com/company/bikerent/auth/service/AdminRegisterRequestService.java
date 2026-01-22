package com.company.bikerent.auth.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.bikerent.auth.domain.AdminRegisterRequest;
import com.company.bikerent.auth.domain.RequestStatus;
import com.company.bikerent.auth.dto.AdminRegisterRequestDto;
import com.company.bikerent.auth.dto.AdminRegisterRequestRequest;
import com.company.bikerent.auth.dto.RoleResponse;
import com.company.bikerent.auth.mapper.AdminRegisterRequestMapper;
import com.company.bikerent.auth.repository.AdminRegisterRequestRepository;
import com.company.bikerent.common.exception.BusinessException;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.user.domain.Role;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegisterRequestService {

  private final UserRepository userRepository;
  private final AdminRegisterRequestRepository adminRequestRepository;
  private final AdminRegisterRequestMapper adminRegisterRequestMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @Transactional(readOnly = true)
  public Page<AdminRegisterRequestDto> getPendingRequests(Pageable pageable) {
    return adminRequestRepository
        .findByStatus(RequestStatus.PENDING, pageable)
        .map(adminRegisterRequestMapper::toDto);
  }

  @Transactional(readOnly = true)
  public AdminRegisterRequestDto getRequestByUserId(Long userId) {
    AdminRegisterRequest request =
        adminRequestRepository
            .findByUserId(userId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Admin request not found for user ID: " + userId));
    return adminRegisterRequestMapper.toDto(request);
  }

  @Transactional(readOnly = true)
  public RoleResponse getRoleResponse() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    String role =
        authentication.getAuthorities().stream()
            .findFirst()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .orElse("USER");

    return new RoleResponse(username, role);
  }

  @Transactional(readOnly = true)
  public boolean isRequestPendingByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .map(
            user ->
                adminRequestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING))
        .orElse(false);
  }

  @Transactional
  public AdminRegisterRequestDto createAdminRequest(AdminRegisterRequestRequest requestDto) {
    log.info("Creating admin request for user: {}", requestDto.username());

    User user =
        userRepository
            .findByUsername(requestDto.username())
            .orElseThrow(
                () -> new EntityNotFoundException(User.class, "username", requestDto.username()));

    // Check if there are no admins - auto-approve first admin
    if (!hasRegisteredAdmins()) {
      return registerFirstAdminAutomatically(user, requestDto);
    }

    AdminRegisterRequest adminRequest =
        AdminRegisterRequest.builder()
            .user(user)
            .description(requestDto.description())
            .status(RequestStatus.PENDING)
            .createdDate(LocalDateTime.now())
            .build();

    AdminRegisterRequest saved = adminRequestRepository.save(adminRequest);
    messagingTemplate.convertAndSend(
        "/topic/newAdminRequest", adminRegisterRequestMapper.toDto(saved));

    log.info("Admin request created with ID: {}", saved.getId());
    return adminRegisterRequestMapper.toDto(saved);
  }

  @Transactional
  public void approveAdminRequest(Long requestId) {
    log.info("Approving admin request: {}", requestId);

    AdminRegisterRequest adminRequest =
        adminRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException(AdminRegisterRequest.class, requestId));

    if (adminRequest.isProcessed()) {
      throw new BusinessException("Cannot modify a processed request");
    }

    adminRequest.setStatus(RequestStatus.APPROVED);
    adminRequest.setUpdatedDate(LocalDateTime.now());
    adminRequestRepository.save(adminRequest);

    User user = adminRequest.getUser();
    user.setRole(Role.ADMIN);
    userRepository.save(user);

    log.info("Admin request {} approved. User {} is now ADMIN", requestId, user.getUsername());
  }

  @Transactional
  public void rejectAdminRequest(Long requestId) {
    log.info("Rejecting admin request: {}", requestId);

    AdminRegisterRequest adminRequest =
        adminRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException(AdminRegisterRequest.class, requestId));

    if (adminRequest.isProcessed()) {
      throw new BusinessException("Cannot modify a processed request");
    }

    adminRequest.setStatus(RequestStatus.REJECTED);
    adminRequest.setUpdatedDate(LocalDateTime.now());
    adminRequestRepository.save(adminRequest);

    log.info("Admin request {} rejected", requestId);
  }

  private boolean hasRegisteredAdmins() {
    return userRepository.existsByRole(Role.ADMIN);
  }

  private AdminRegisterRequestDto registerFirstAdminAutomatically(
      User user, AdminRegisterRequestRequest requestDto) {
    log.info("Auto-approving first admin: {}", user.getUsername());

    user.setRole(Role.ADMIN);
    userRepository.save(user);

    AdminRegisterRequest adminRequest =
        AdminRegisterRequest.builder()
            .user(user)
            .description(requestDto.description())
            .status(RequestStatus.APPROVED)
            .createdDate(LocalDateTime.now())
            .updatedDate(LocalDateTime.now())
            .build();

    AdminRegisterRequest saved = adminRequestRepository.save(adminRequest);
    return adminRegisterRequestMapper.toDto(saved);
  }
}
