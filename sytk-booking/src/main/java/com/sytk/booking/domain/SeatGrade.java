package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer totalSeatCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Builder
    public SeatGrade(String name, BigDecimal price, Integer totalSeatCount, Concert concert) {
        this.concert = concert;
        this.name = name;
        this.price = price;
        this.totalSeatCount = totalSeatCount;
    }
}
