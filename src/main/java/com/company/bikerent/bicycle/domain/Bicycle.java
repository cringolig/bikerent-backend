package com.company.bikerent.bicycle.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import com.company.bikerent.maintenance.domain.Repair;
import com.company.bikerent.station.domain.Station;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bicycle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bicycle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @NotBlank(message = "Model is required")
  @Size(max = 100, message = "Model cannot exceed 100 characters")
  @Column(name = "model", nullable = false, length = 100)
  private String model;

  @NotNull(message = "Bicycle type is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private BicycleType type;

  @NotNull(message = "Bicycle status is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private BicycleStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "station_id")
  private Station station;

  @CreationTimestamp
  @Column(name = "last_service_date")
  private Date lastServiceDate;

  @Min(value = 0, message = "Mileage cannot be negative")
  @Column(name = "mileage")
  private Long mileage = 0L;

  @OneToMany(mappedBy = "bicycle", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Repair> repairs = new ArrayList<>();

  public boolean isAvailable() {
    return status == BicycleStatus.AVAILABLE;
  }

  public boolean isRented() {
    return status == BicycleStatus.RENTED;
  }

  public boolean isUnderMaintenance() {
    return status == BicycleStatus.UNAVAILABLE;
  }

  public boolean canBeRented() {
    return isAvailable();
  }

  public boolean needsService() {
    return mileage != null && mileage > 50L;
  }

  public void startRental() {
    if (!canBeRented()) {
      throw new IllegalStateException("Bicycle is not available for rental");
    }
    this.status = BicycleStatus.RENTED;
  }

  public void endRental(Station endStation) {
    if (!isRented()) {
      throw new IllegalStateException("Bicycle is not currently rented");
    }
    this.status = BicycleStatus.AVAILABLE;
    this.station = endStation;
  }

  public void startMaintenance() {
    if (!isAvailable()) {
      throw new IllegalStateException("Bicycle is not available for maintenance");
    }
    this.status = BicycleStatus.UNAVAILABLE;
  }

  public void completeMaintenance() {
    if (!isUnderMaintenance()) {
      throw new IllegalStateException("Bicycle is not under maintenance");
    }
    this.status = BicycleStatus.AVAILABLE;
    this.mileage = 0L;
    this.lastServiceDate = new Date();
  }

  public void addMileage(long additionalMileage) {
    if (additionalMileage < 0) {
      throw new IllegalArgumentException("Mileage cannot be negative");
    }
    this.mileage = (this.mileage != null ? this.mileage : 0L) + additionalMileage;
  }
}
