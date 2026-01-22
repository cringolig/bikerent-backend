package com.company.bikerent.user.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus userStatus;

    @Min(value = 0, message = "Balance cannot be negative")
    @Max(value = 999999, message = "Balance cannot exceed 999999")
    @Column(name = "balance", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long balance = 0L;

    @Min(value = 0, message = "Debt cannot be negative")
    @Max(value = 999999, message = "Debt cannot exceed 999999")
    @Column(name = "debt", columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long debt = 0L;

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private Date registrationDate;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return userStatus != UserStatus.DISABLED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return userStatus != UserStatus.DISABLED;
    }

    // Domain methods
    public boolean canRentBicycle() {
        return balance > 0 && debt == 0;
    }

    public void addBalance(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance += amount;
    }

    public void chargeForRental(Double cost) {
        long costLong = cost.longValue();
        if (balance >= costLong) {
            balance -= costLong;
        } else {
            long remaining = costLong - balance;
            balance = 0L;
            debt += remaining;
        }
    }
}
