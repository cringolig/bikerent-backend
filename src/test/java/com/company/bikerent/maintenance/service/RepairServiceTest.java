package com.company.bikerent.maintenance.service;

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
import com.company.bikerent.bicycle.mapper.BicycleMapper;
import com.company.bikerent.bicycle.repository.BicycleRepository;
import com.company.bikerent.common.exception.BusinessException;
import com.company.bikerent.common.exception.EntityNotFoundException;
import com.company.bikerent.maintenance.domain.Repair;
import com.company.bikerent.maintenance.domain.RepairStatus;
import com.company.bikerent.maintenance.domain.Technician;
import com.company.bikerent.maintenance.dto.CreateRepairRequest;
import com.company.bikerent.maintenance.dto.RepairDto;
import com.company.bikerent.maintenance.mapper.RepairMapper;
import com.company.bikerent.maintenance.repository.RepairRepository;
import com.company.bikerent.maintenance.repository.TechnicianRepository;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

  @Mock private RepairRepository repairRepository;

  @Mock private BicycleRepository bicycleRepository;

  @Mock private TechnicianRepository technicianRepository;

  @Mock private RepairMapper repairMapper;

  @Mock private BicycleMapper bicycleMapper;

  @InjectMocks private RepairService repairService;

  private Bicycle testBicycle;
  private Technician testTechnician;
  private Repair testRepair;

  @BeforeEach
  void setUp() {
    testBicycle = new Bicycle();
    testBicycle.setId(1L);
    testBicycle.setModel("Test Model");
    testBicycle.setType(BicycleType.UNIVERSAL);
    testBicycle.setStatus(BicycleStatus.AVAILABLE);
    testBicycle.setMileage(60L);

    testTechnician = new Technician();
    testTechnician.setId(1L);
    testTechnician.setName("John Tech");
    testTechnician.setPhone("123456789");
    testTechnician.setSpecialization("General Repairs");

    testRepair =
        Repair.builder()
            .id(1L)
            .bicycle(testBicycle)
            .technician(testTechnician)
            .description("Fix brakes")
            .status(RepairStatus.IN_PROGRESS)
            .repairStartedAt(LocalDateTime.now())
            .build();
  }

  @Nested
  @DisplayName("Create Repair Tests")
  class CreateRepairTests {

    @Test
    @DisplayName("Should create repair successfully")
    void shouldCreateRepairSuccessfully() {
      // Given
      CreateRepairRequest request = new CreateRepairRequest(1L, 1L, "Fix brakes");
      RepairDto expectedDto = new RepairDto(1L, 1L, 1L, "Fix brakes", "IN_PROGRESS", null, null);

      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(repairRepository.existsByBicycleIdAndStatus(1L, RepairStatus.IN_PROGRESS))
          .thenReturn(false);
      when(technicianRepository.findById(1L)).thenReturn(Optional.of(testTechnician));
      when(repairRepository.save(any(Repair.class))).thenReturn(testRepair);
      when(repairMapper.toDto(any(Repair.class))).thenReturn(expectedDto);

      // When
      RepairDto result = repairService.create(request);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo("IN_PROGRESS");
      verify(bicycleRepository).save(any(Bicycle.class));
      verify(repairRepository).save(any(Repair.class));
    }

    @Test
    @DisplayName("Should throw exception when bicycle not found")
    void shouldThrowWhenBicycleNotFound() {
      // Given
      CreateRepairRequest request = new CreateRepairRequest(999L, 1L, "Fix brakes");
      when(bicycleRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> repairService.create(request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Bicycle");
    }

    @Test
    @DisplayName("Should throw exception when bicycle already under repair")
    void shouldThrowWhenBicycleAlreadyUnderRepair() {
      // Given
      CreateRepairRequest request = new CreateRepairRequest(1L, 1L, "Fix brakes");
      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(repairRepository.existsByBicycleIdAndStatus(1L, RepairStatus.IN_PROGRESS))
          .thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> repairService.create(request))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("active repair");
    }

    @Test
    @DisplayName("Should throw exception when bicycle is rented")
    void shouldThrowWhenBicycleIsRented() {
      // Given
      testBicycle.setStatus(BicycleStatus.RENTED);
      CreateRepairRequest request = new CreateRepairRequest(1L, 1L, "Fix brakes");

      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(repairRepository.existsByBicycleIdAndStatus(1L, RepairStatus.IN_PROGRESS))
          .thenReturn(false);
      when(technicianRepository.findById(1L)).thenReturn(Optional.of(testTechnician));

      // When & Then
      assertThatThrownBy(() -> repairService.create(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("Should throw exception when technician not found")
    void shouldThrowWhenTechnicianNotFound() {
      // Given
      CreateRepairRequest request = new CreateRepairRequest(1L, 999L, "Fix brakes");
      when(bicycleRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testBicycle));
      when(repairRepository.existsByBicycleIdAndStatus(1L, RepairStatus.IN_PROGRESS))
          .thenReturn(false);
      when(technicianRepository.findById(999L)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> repairService.create(request))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Technician");
    }
  }

  @Nested
  @DisplayName("Complete Repair Tests")
  class CompleteRepairTests {

    @Test
    @DisplayName("Should complete repair successfully")
    void shouldCompleteRepairSuccessfully() {
      // Given
      testBicycle.setStatus(BicycleStatus.UNAVAILABLE);
      RepairDto expectedDto =
          new RepairDto(1L, 1L, 1L, "Fix brakes", "COMPLETED", null, LocalDateTime.now());

      when(repairRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRepair));
      when(repairRepository.save(any(Repair.class))).thenReturn(testRepair);
      when(repairMapper.toDto(any(Repair.class))).thenReturn(expectedDto);

      // When
      RepairDto result = repairService.complete(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo("COMPLETED");
      verify(bicycleRepository).save(testBicycle);
    }

    @Test
    @DisplayName("Should throw exception when repair is not in progress")
    void shouldThrowWhenRepairNotInProgress() {
      // Given
      testRepair.setStatus(RepairStatus.COMPLETED);
      when(repairRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRepair));

      // When & Then
      assertThatThrownBy(() -> repairService.complete(1L))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("not in progress");
    }

    @Test
    @DisplayName("Should throw exception when repair not found")
    void shouldThrowWhenRepairNotFound() {
      // Given
      when(repairRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> repairService.complete(999L))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Repair");
    }

    @Test
    @DisplayName("Should reset bicycle mileage after repair completion")
    void shouldResetBicycleMileageAfterCompletion() {
      // Given
      testBicycle.setStatus(BicycleStatus.UNAVAILABLE);
      testBicycle.setMileage(100L);

      when(repairRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testRepair));
      when(repairRepository.save(any(Repair.class))).thenReturn(testRepair);
      when(repairMapper.toDto(any(Repair.class)))
          .thenReturn(new RepairDto(1L, 1L, 1L, "Fix brakes", "COMPLETED", null, null));

      // When
      repairService.complete(1L);

      // Then
      assertThat(testBicycle.getMileage()).isEqualTo(0L);
      assertThat(testBicycle.getStatus()).isEqualTo(BicycleStatus.AVAILABLE);
    }
  }

  @Nested
  @DisplayName("Bicycle Domain Tests")
  class BicycleDomainTests {

    @Test
    @DisplayName("Should correctly identify bicycle needing service")
    void shouldIdentifyBicycleNeedingService() {
      // Given
      testBicycle.setMileage(60L);

      // When & Then
      assertThat(testBicycle.needsService()).isTrue();
    }

    @Test
    @DisplayName("Should not need service when mileage is low")
    void shouldNotNeedServiceWhenMileageLow() {
      // Given
      testBicycle.setMileage(30L);

      // When & Then
      assertThat(testBicycle.needsService()).isFalse();
    }

    @Test
    @DisplayName("Should start maintenance correctly")
    void shouldStartMaintenanceCorrectly() {
      // Given
      testBicycle.setStatus(BicycleStatus.AVAILABLE);

      // When
      testBicycle.startMaintenance();

      // Then
      assertThat(testBicycle.getStatus()).isEqualTo(BicycleStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("Should throw when starting maintenance on non-available bicycle")
    void shouldThrowWhenStartingMaintenanceOnNonAvailable() {
      // Given
      testBicycle.setStatus(BicycleStatus.RENTED);

      // When & Then
      assertThatThrownBy(() -> testBicycle.startMaintenance())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not available");
    }
  }
}
