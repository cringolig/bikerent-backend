package com.company.bikerent.station.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.geo.domain.Coordinates;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Station {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @NotBlank(message = "Station name is required")
  @Size(max = 100, message = "Station name cannot exceed 100 characters")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @NotNull(message = "Coordinates are required")
  @Embedded
  private Coordinates coordinates;

  @Column(name = "available_bicycles")
  private Long availableBicycles;

  @OneToMany(mappedBy = "station", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<Bicycle> bicycles = new ArrayList<>();

  public void updateAvailableBicyclesCount() {
    this.availableBicycles = bicycles.stream().filter(Bicycle::isAvailable).count();
  }
}
