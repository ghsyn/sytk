package com.sytk.booking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    RESERVING("대기"),
    CONFIRMED("확정"),
    CANCELED("취소"),
    EXPIRED("만료");

    private final String description;

    public boolean canChangeTo(ReservationStatus next) {
        return switch (this) {
            case RESERVING -> next == CONFIRMED || next == CANCELED || next == EXPIRED; // 결제 완료 || 예매 취소 || 타임아웃
            case CONFIRMED, CANCELED, EXPIRED -> false;
        };
    }
}
