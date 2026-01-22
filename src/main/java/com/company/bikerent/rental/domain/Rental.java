package com.company.bikerent.rental.domain;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    private static final double COST_PER_MINUTE = 6.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Bicycle is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bicycle_id", nullable = false)
    private Bicycle bicycle;

    @NotNull(message = "Start station is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_station_id", nullable = false)
    private Station startStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_station_id")
    private Station endStation;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RentalStatus status = RentalStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "rental_started_at", nullable = false, updatable = false)
    private LocalDateTime rentalStartedAt;

    @Column(name = "rental_ended_at")
    private LocalDateTime rentalEndedAt;

    @Column(name = "cost")
    @Builder.Default
    private Double cost = 0.0;

    // Domain methods
    public boolean isActive() {
        return status == RentalStatus.ACTIVE;
    }

    public boolean isEnded() {
        return status == RentalStatus.ENDED;
    }

    public boolean isCancelled() {
        return status == RentalStatus.CANCELLED;
    }

    public void complete(Station endStation) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot complete a rental that is not active");
        }
        
        this.endStation = endStation;
        this.rentalEndedAt = LocalDateTime.now();
        this.status = RentalStatus.ENDED;
        this.cost = calculateCost();
        
        // Update bicycle state
        this.bicycle.endRental(endStation);
    }

    public void cancel() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot cancel a rental that is not active");
        }
        
        this.rentalEndedAt = LocalDateTime.now();
        this.status = RentalStatus.CANCELLED;
        this.cost = 0.0;
        
        // Return bicycle to start station
        this.bicycle.endRental(this.startStation);
    }

    public Double calculateCost() {
        if (rentalStartedAt == null || rentalEndedAt == null) {
            return 0.0;
        }
        
        long durationMinutes = getDurationInMinutes();
        return durationMinutes * COST_PER_MINUTE;
    }

    public long getDurationInMinutes() {
        if (rentalStartedAt == null) {
            return 0;
        }
        
        LocalDateTime endTime = rentalEndedAt != null ? rentalEndedAt : LocalDateTime.now();
        return Duration.between(rentalStartedAt, endTime).toMinutes();
    }

    public void updateMileage() {
        long duration = getDurationInMinutes();
        bicycle.addMileage(duration);
    }

    public static Rental startRental(User user, Bicycle bicycle, Station startStation) {
        if (!user.canRentBicycle()) {
            throw new IllegalStateException("User cannot rent a bicycle with zero balance or non-zero debt");
        }
        
        if (!bicycle.canBeRented()) {
            throw new IllegalStateException("Bicycle is not available for rental");
        }
        
        bicycle.startRental();
        
        return Rental.builder()
                .user(user)
                .bicycle(bicycle)
                .startStation(startStation)
                .status(RentalStatus.ACTIVE)
                .cost(0.0)
                .build();
    }
}
