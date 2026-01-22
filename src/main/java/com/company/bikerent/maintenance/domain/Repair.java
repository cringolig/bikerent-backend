package com.company.bikerent.maintenance.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.company.bikerent.bicycle.domain.Bicycle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repair")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @NotNull(message = "Bicycle is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bicycle_id", nullable = false)
  private Bicycle bicycle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "technician_id")
  private Technician technician;

  @NotBlank(message = "Description is required")
  @Size(max = 500, message = "Description cannot exceed 500 characters")
  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @CreationTimestamp
  @Column(name = "repair_started_at", nullable = false, updatable = false)
  private LocalDateTime repairStartedAt;

  @UpdateTimestamp
  @Column(name = "repair_ended_at")
  private LocalDateTime repairEndedAt;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private RepairStatus status = RepairStatus.IN_PROGRESS;

  // Domain methods
  public boolean isInProgress() {
    return status == RepairStatus.IN_PROGRESS;
  }

  public boolean isCompleted() {
    return status == RepairStatus.COMPLETED;
  }

  public void complete() {
    if (!isInProgress()) {
      throw new IllegalStateException("Repair is not in progress");
    }

    this.status = RepairStatus.COMPLETED;
    this.repairEndedAt = LocalDateTime.now();
    this.bicycle.completeMaintenance();
  }

  public static Repair startRepair(Bicycle bicycle, Technician technician, String description) {
    if (!bicycle.isAvailable()) {
      throw new IllegalStateException("Bicycle is not available for maintenance");
    }

    bicycle.startMaintenance();

    return Repair.builder()
        .bicycle(bicycle)
        .technician(technician)
        .description(description)
        .status(RepairStatus.IN_PROGRESS)
        .build();
  }
}
