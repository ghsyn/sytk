package com.sytk.booking.domain;

import com.sytk.booking.exception.InvalidReservationStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Version
    private Long version;

    @Builder
    public Reservation(Long userId, Seat seat, OffsetDateTime expiredAt) {
        if (userId == null) {
            throw new IllegalArgumentException("유저 ID는 필수입니다.");
        }
        if (seat == null) {
            throw new IllegalArgumentException("좌석은 필수입니다.");
        }
        if (expiredAt == null) {
            throw new IllegalArgumentException("만료 시간은 필수입니다.");
        }
        this.userId = userId;
        this.seat = seat;
        this.expiredAt = expiredAt;
        this.status = ReservationStatus.RESERVING;
    }

    public boolean isExpiredAt(OffsetDateTime now) {
        return this.expiredAt.isBefore(now);
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
}
