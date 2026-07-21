package com.sytk.booking.domain;

import com.sytk.booking.exception.InvalidReservationStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private OffsetDateTime expiredAt;

    @Column(nullable = false)
    private Long userId;

    @JoinColumn(name = "seat_id", nullable = false)
    private Long seatId;

    @Version
    private Long version;

    @Builder
    public Reservation(Long userId, Long seatId) {

        if (userId == null) {
            throw new IllegalArgumentException("유저 ID는 필수입니다.");
        }
        if (seatId == null) {
            throw new IllegalArgumentException("좌석은 필수입니다.");
        }

        this.status = ReservationStatus.RESERVING;
        this.expiredAt = now().plusMinutes(60);
        this.userId = userId;
        this.seatId = seatId;
    }

    public boolean isExpired(OffsetDateTime currentTime) {
        return !currentTime.isBefore(this.expiredAt);
    }

    // ==========================================
    // 상태 전이 비즈니스 메서드
    // ==========================================
    private void changeStatus(ReservationStatus next) {
        if (!this.status.canChangeTo(next)) {
            throw new InvalidReservationStatusTransitionException(this.status, next);
        }
        this.status = next;
    }

    // 결제 완료 (RESERVING → CONFIRMED)
    public void confirm() {
        changeStatus(ReservationStatus.CONFIRMED);
    }

    // 예매 취소 (RESERVING → CANCELED)
    public void cancel() {
        changeStatus(ReservationStatus.CANCELED);
    }

    // 점유 만료 (RESERVING → EXPIRED)
    public void expire() {
        changeStatus(ReservationStatus.EXPIRED);
    }

    // 환불 (CONFIRMED → REFUNDED)
    public void refund() {
        changeStatus(ReservationStatus.REFUNDED);
    }
}
