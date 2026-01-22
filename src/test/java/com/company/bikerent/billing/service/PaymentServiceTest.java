package com.company.bikerent.billing.service;

import com.company.bikerent.billing.domain.Payment;
import com.company.bikerent.billing.dto.CreatePaymentRequest;
import com.company.bikerent.billing.dto.PaymentDto;
import com.company.bikerent.billing.mapper.PaymentMapper;
import com.company.bikerent.billing.repository.PaymentRepository;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.user.domain.Role;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Payment testPayment;
    private PaymentDto testPaymentDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .balance(100L)
                .debt(0L)
                .role(Role.USER)
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .user(testUser)
                .amount(50L)
                .paymentDate(new Date())
                .build();

        testPaymentDto = new PaymentDto(1L, 1L, 50L, new Date());
    }

    @Nested
    @DisplayName("Find Payments Tests")
    class FindPaymentsTests {

        @Test
        @DisplayName("Should find all payments")
        void shouldFindAllPayments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> paymentPage = new PageImpl<>(List.of(testPayment));
            
            when(paymentRepository.findAll(pageable)).thenReturn(paymentPage);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(testPaymentDto);

            // When
            Page<PaymentDto> result = paymentService.findAll(pageable);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).amount()).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should find payments by username")
        void shouldFindPaymentsByUsername() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Payment> paymentPage = new PageImpl<>(List.of(testPayment));
            
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findAllByUser(testUser, pageable)).thenReturn(paymentPage);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(testPaymentDto);

            // When
            Page<PaymentDto> result = paymentService.findAllByUser("testuser", pageable);

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should throw exception when user not found by username")
        void shouldThrowWhenUserNotFoundByUsername() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.findAllByUser("unknown", pageable))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("Create Payment Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should create payment and add balance to user")
        void shouldCreatePaymentSuccessfully() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(100L);
            Long initialBalance = testUser.getBalance();

            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("testuser");
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(testPaymentDto);

            // When
            PaymentDto result = paymentService.create(request);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(testUser);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should throw exception when payment amount is invalid")
        void shouldThrowWhenAmountIsInvalid() {
            // Given - This would be caught by validation before service
            // Testing domain validation
            assertThatThrownBy(() -> Payment.createPayment(testUser, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should correctly update user balance")
        void shouldCorrectlyUpdateUserBalance() {
            // Given
            testUser.setBalance(100L);
            
            // When
            Payment.createPayment(testUser, 50L);

            // Then
            assertThat(testUser.getBalance()).isEqualTo(150L);
        }
    }

    @Nested
    @DisplayName("User Domain Tests")
    class UserDomainTests {

        @Test
        @DisplayName("Should add balance correctly")
        void shouldAddBalanceCorrectly() {
            // Given
            testUser.setBalance(100L);

            // When
            testUser.addBalance(50L);

            // Then
            assertThat(testUser.getBalance()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Should throw when adding negative balance")
        void shouldThrowWhenAddingNegativeBalance() {
            // Given
            testUser.setBalance(100L);

            // When & Then
            assertThatThrownBy(() -> testUser.addBalance(-50L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should charge for rental from balance")
        void shouldChargeForRentalFromBalance() {
            // Given
            testUser.setBalance(100L);
            testUser.setDebt(0L);

            // When
            testUser.chargeForRental(30.0);

            // Then
            assertThat(testUser.getBalance()).isEqualTo(70L);
            assertThat(testUser.getDebt()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should create debt when balance is insufficient")
        void shouldCreateDebtWhenBalanceInsufficient() {
            // Given
            testUser.setBalance(50L);
            testUser.setDebt(0L);

            // When
            testUser.chargeForRental(80.0);

            // Then
            assertThat(testUser.getBalance()).isEqualTo(0L);
            assertThat(testUser.getDebt()).isEqualTo(30L);
        }
    }
}
