package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long seatGradeId;

    @Column(length = 4, nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Version
    private Integer version;

    @OneToOne(mappedBy = "seat")
    private Reservation reservation;

    @Builder
    public Seat(Long seatGradeId, String number, SeatStatus status) {
        this.seatGradeId = seatGradeId;
        this.number = number;
        this.status = status;
    }
}
