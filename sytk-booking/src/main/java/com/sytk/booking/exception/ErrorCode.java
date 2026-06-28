package com.sytk.booking.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    NOT_CHANGED(HttpStatus.BAD_REQUEST, "변경사항이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // Concert
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."),
    DUPLICATE_CONCERT(HttpStatus.CONFLICT, "이미 존재하는 공연입니다. 제목을 수정해주세요."),
    RESERVED_CONCERT(HttpStatus.CONFLICT, "예매된 내역이 있어 삭제할 수 없습니다."),

    // SeatGrade
    DUPLICATE_SEAT_GRADE(HttpStatus.CONFLICT,"좌석 등급명은 중복될 수 없습니다."),

    // Seat
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."),
    INVALID_SEAT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "변경할 수 없는 좌석 상태입니다."),

    // Reservation
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예매 내역을 찾을 수 없습니다."),
    INVALID_RESERVATION_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "변경할 수 없는 예매 상태입니다.");

    private final HttpStatus status;
    private final String message;
}
