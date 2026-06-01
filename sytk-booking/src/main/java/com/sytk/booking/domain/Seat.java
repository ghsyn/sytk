package com.sytk.booking.domain;

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

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Builder
    public Seat(SeatGrade seatGrade, Integer number, SeatStatus status) {
        this.seatGrade = seatGrade;
        this.number = number;
        this.status = (status != null) ? status : SeatStatus.CLOSED;
    }

    // ==========================================
    // 상태 전이 비즈니스 메서드
    // ==========================================
    private void changeStatus(SeatStatus next) {
        if (!this.status.canChangeTo(next)) {
            throw new IllegalStateException(
                    String.format("좌석 상태를 %s에서 %s로 변경할 수 없습니다.", this.status.getDescription(), next.getDescription())
            );
        }
        this.status = next;
    }

    // 1. 개시(CLOSED -> AVAILABLE)
    public void open() {
        changeStatus(SeatStatus.AVAILABLE);
    }

    // 2. 선점(AVAILABLE -> OCCUPIED)
    public void hold() {
        changeStatus(SeatStatus.OCCUPIED);
    }

    // 3. 점유 해제(OCCUPIED -> AVAILABLE)
    public void release() {
        changeStatus(SeatStatus.AVAILABLE);
    }

    // 4. 결제 완료(OCCUPIED -> SOLD)
    public void sell() {
        changeStatus(SeatStatus.SOLD);
    }

    // 5. 미판매(AVAILABLE -> CLOSED)
    public void close() {
        changeStatus(SeatStatus.CLOSED);
    }
}
