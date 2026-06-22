package com.sytk.booking.domain;

import com.sytk.booking.exception.InvalidSeatStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.CLOSED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Builder
    public Seat(SeatGrade seatGrade, Integer number, SeatStatus status) {
        if (seatGrade == null) {
            throw new IllegalArgumentException("좌석 등급은 필수입니다.");
        }
        if (number == null || number < 1) {
            throw new IllegalArgumentException("좌석 번호는 1 이상이어야 합니다.");
        }
        this.seatGrade = seatGrade;
        this.number = number;
        this.status = (status != null) ? status : SeatStatus.CLOSED;
    }

    // ==========================================
    // 상태 전이 비즈니스 메서드
    // ==========================================
    private void changeStatus(SeatStatus before, SeatStatus after) {
        if (!this.status.equals(before) || !this.status.canChangeTo(after)) {
            throw new InvalidSeatStatusTransitionException(this.status, after);
        }
        this.status = after;
    }

    // 1. 개시(CLOSED -> AVAILABLE)
    public void open() {
        changeStatus(SeatStatus.CLOSED, SeatStatus.AVAILABLE);
    }

    // 2. 선점(AVAILABLE -> OCCUPIED)
    public void hold() {
        changeStatus(SeatStatus.AVAILABLE, SeatStatus.OCCUPIED);
    }

    // 3. 점유 해제(OCCUPIED -> AVAILABLE)
    public void release() {
        changeStatus(SeatStatus.OCCUPIED, SeatStatus.AVAILABLE);
    }

    // 4. 결제 완료(OCCUPIED -> SOLD)
    public void sell() {
        changeStatus(SeatStatus.OCCUPIED, SeatStatus.SOLD);
    }

    // 5. 미판매(AVAILABLE -> CLOSED)
    public void close() {
        changeStatus(SeatStatus.AVAILABLE, SeatStatus.CLOSED);
    }
}
