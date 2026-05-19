package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    private OffsetDateTime expiredAt;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Builder
    public Reservation(ReservationStatus status, OffsetDateTime expiredAt, Seat seat, Long userId) {
        this.seat = seat;
        this.userId = userId;
        this.status = status;
        this.expiredAt = expiredAt;
    }
}
