package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        if (totalSeatCount == null || totalSeatCount < 1) {
            throw new IllegalArgumentException("좌석 등급의 총 좌석 수는 1 이상이어야 합니다.");
        }

        this.concert = concert;
        this.name = name;
        this.price = price;
        this.totalSeatCount = totalSeatCount;
    }

    public List<Seat> createSeats() {
        List<Seat> seatList = new ArrayList<>(this.totalSeatCount);

        for (int i = 0; i < this.totalSeatCount; i++) {
            seatList.add(Seat.builder()
                    .seatGrade(this)
                    .number(i + 1)
                    .status(SeatStatus.CLOSED)
                    .build());
        }

        return seatList;
    }
}
