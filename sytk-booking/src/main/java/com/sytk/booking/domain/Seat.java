package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Builder
    public Seat(SeatGrade seatGrade, String number, SeatStatus status) {
        this.seatGrade = seatGrade;
        this.number = number;
        this.status = status;
    }
}
