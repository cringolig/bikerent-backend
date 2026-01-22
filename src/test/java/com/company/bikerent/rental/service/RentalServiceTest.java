package com.company.bikerent.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.domain.BicycleStatus;
import com.company.bikerent.bicycle.domain.BicycleType;
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
import com.company.bikerent.user.domain.Role;
import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

  @Mock private RentalRepository rentalRepository;

  @Mock private UserRepository userRepository;

  @Mock private BicycleRepository bicycleRepository;

  @Mock private StationRepository stationRepository;

  @Mock private RentalMapper rentalMapper;

  @InjectMocks private RentalService rentalService;

  private User testUser;
  private Bicycle testBicycle;
  private Station testStation;
  private Rental testRental;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .password("password")
            .balance(100L)
            .debt(0L)
            .role(Role.USER)
            .build();

    testStation = new Station();
    testStation.setId(1L);
    testStation.setName("Test Station");

    testBicycle = new Bicycle();
    testBicycle.setId(1L);
    testBicycle.setModel("Test Model");
    testBicycle.setType(BicycleType.UNIVERSAL);
    testBicycle.setStatus(BicycleStatus.AVAILABLE);
    testBicycle.setStation(testStation);
    testBicycle.setMileage(0L);

    testRental =
        Rental.builder()
            .id(1L)
            .user(testUser)
            .bicycle(testBicycle)
            .startStation(testStation)
            .status(RentalStatus.ACTIVE)
            .rentalStartedAt(LocalDateTime.now().minusMinutes(30))
            .cost(0.0)
            .build();
  }

  @Nested
  @DisplayName("Create Rental Tests")
  class CreateRentalTests {

    @Test
    @DisplayName("Should create rental successfully when all conditions are met")
    void shouldCreateRentalSuccessfully() {
      // Given
      CreateRentalRequest request = new CreateRentalRequest(1L, 1L, 1L);
      RentalDto expectedDto = new RentalDto(1L, 1L, 1L, 1L, null, "ACTIVE", null, null, 0.0);

      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(rentalRepository.existsByBicycleIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(false);
      when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
      when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
      when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
      when(rentalMapper.toDto(any(Rental.class))).thenReturn(expectedDto);

      // When
      RentalDto result = rentalService.create(request);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo("ACTIVE");
      verify(bicycleRepository).save(any(Bicycle.class));
      verify(rentalRepository).save(any(Rental.class));
    }

    @Test
    @DisplayName("Should throw exception when bicycle is not found")
    void shouldThrowWhenBicycleNotFound() {
      // Given
      CreateRentalRequest request = new CreateRentalRequest(1L, 999L, 1L);
      when(bicycleRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> rentalService.create(request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Bicycle");
    }

    @Test
    @DisplayName("Should throw exception when bicycle is already rented")
    void shouldThrowWhenBicycleAlreadyRented() {
      // Given
      CreateRentalRequest request = new CreateRentalRequest(1L, 1L, 1L);
      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(rentalRepository.existsByBicycleIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> rentalService.create(request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("already rented");
    }

    @Test
    @DisplayName("Should throw exception when user has zero balance")
    void shouldThrowWhenUserHasZeroBalance() {
      // Given
      testUser.setBalance(0L);
      CreateRentalRequest request = new CreateRentalRequest(1L, 1L, 1L);

      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(rentalRepository.existsByBicycleIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(false);
      when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
      when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

      // When & Then
      assertThatThrownBy(() -> rentalService.create(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("cannot rent");
    }

    @Test
    @DisplayName("Should throw exception when user has debt")
    void shouldThrowWhenUserHasDebt() {
      // Given
      testUser.setDebt(50L);
      CreateRentalRequest request = new CreateRentalRequest(1L, 1L, 1L);

      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(rentalRepository.existsByBicycleIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(false);
      when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
      when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

      // When & Then
      assertThatThrownBy(() -> rentalService.create(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("cannot rent");
    }
  }

  @Nested
  @DisplayName("Complete Rental Tests")
  class CompleteRentalTests {

    @Test
    @DisplayName("Should complete rental successfully")
    void shouldCompleteRentalSuccessfully() {
      // Given
      testBicycle.setStatus(BicycleStatus.RENTED);
      CompleteRentalRequest request = new CompleteRentalRequest(2L);
      Station endStation = new Station();
      endStation.setId(2L);
      endStation.setName("End Station");

      RentalDto expectedDto = new RentalDto(1L, 1L, 1L, 1L, 2L, "ENDED", null, null, 180.0);

      when(rentalRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRental));
      when(stationRepository.findById(2L)).thenReturn(Optional.of(endStation));
      when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
      when(rentalMapper.toDto(any(Rental.class))).thenReturn(expectedDto);

      // When
      RentalDto result = rentalService.complete(1L, request);

      // Then
      assertThat(result).isNotNull();
      verify(userRepository).save(testUser);
      verify(bicycleRepository).save(testBicycle);
      verify(rentalRepository).save(testRental);
    }

    @Test
    @DisplayName("Should throw exception when rental is not active")
    void shouldThrowWhenRentalNotActive() {
      // Given
      testRental.setStatus(RentalStatus.ENDED);
      CompleteRentalRequest request = new CompleteRentalRequest(2L);

      when(rentalRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRental));

      // When & Then
      assertThatThrownBy(() -> rentalService.complete(1L, request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("Should throw exception when rental is not found")
    void shouldThrowWhenRentalNotFound() {
      // Given
      CompleteRentalRequest request = new CompleteRentalRequest(2L);
      when(rentalRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> rentalService.complete(999L, request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Rental");
    }
  }

  @Nested
  @DisplayName("Cancel Rental Tests")
  class CancelRentalTests {

    @Test
    @DisplayName("Should cancel rental successfully")
    void shouldCancelRentalSuccessfully() {
      // Given
      testBicycle.setStatus(BicycleStatus.RENTED);
      RentalDto expectedDto = new RentalDto(1L, 1L, 1L, 1L, null, "CANCELLED", null, null, 0.0);

      when(rentalRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRental));
      when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
      when(rentalMapper.toDto(any(Rental.class))).thenReturn(expectedDto);

      // When
      RentalDto result = rentalService.cancel(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo("CANCELLED");
      verify(bicycleRepository).save(testBicycle);
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-active rental")
    void shouldThrowWhenCancellingNonActiveRental() {
      // Given
      testRental.setStatus(RentalStatus.ENDED);
      when(rentalRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRental));

      // When & Then
      assertThatThrownBy(() -> rentalService.cancel(1L))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not active");
    }
  }
}
