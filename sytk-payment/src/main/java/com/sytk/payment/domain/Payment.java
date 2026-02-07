package com.sytk.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String pgId;

    public Payment(Long reservationId, BigDecimal amount, PaymentStatus status, String pgId) {
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.pgId = pgId;
    }
}
