package com.company.bikerent.billing.domain;

import com.company.bikerent.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    @Max(value = 999999, message = "Amount cannot exceed 999999")
    @Column(name = "amount", nullable = false)
    private Long amount;

    @CreationTimestamp
    @Column(name = "payment_date", nullable = false, updatable = false)
    private Date paymentDate;

    public static Payment createPayment(User user, Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        user.addBalance(amount);
        
        return Payment.builder()
                .user(user)
                .amount(amount)
                .build();
    }
}
