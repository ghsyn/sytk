package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class SeatGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false)
    private String name;

    @Column(precision = 7, scale = 0, nullable = false)
    private BigDecimal price;

    @Builder
    public SeatGrade(Concert concert, String name, BigDecimal price) {
        this.concert = concert;
        this.name = name;
        this.price = price;
    }
}
