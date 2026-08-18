package com.mcm.craft.prereg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "preregistrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customerId", "productId"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Preregistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String nameEnc;

    @Column(nullable = false)
    private String phoneEnc;

    private LocalDateTime consentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Preregistration(UUID customerId, Long productId, String nameEnc, String phoneEnc, LocalDateTime consentAt) {
        this.customerId = customerId;
        this.productId = productId;
        this.nameEnc = nameEnc;
        this.phoneEnc = phoneEnc;
        this.consentAt = consentAt;
        this.createdAt = LocalDateTime.now();
    }
}
