package com.sytk.booking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SeatStatus {
    CLOSED("미판매"),
    AVAILABLE("예약 가능"),
    OCCUPIED("선점"),
    SOLD("판매 완료");

    private final String description;

    public boolean canChangeTo(SeatStatus next) {
        return switch (this) {
            case CLOSED -> next == AVAILABLE;                       // 개시
            case AVAILABLE -> next == OCCUPIED || next == CLOSED;   // 선점 || 미판매
            case OCCUPIED -> next == SOLD || next == AVAILABLE;     // 결제 완료 || 취소/만료
            case SOLD -> false;                                     // 판매 완료 좌석 상태 변경 불가
        };
    }
}
