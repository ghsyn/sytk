package com.sytk.booking.controller;

import com.sytk.booking.request.*;
import com.sytk.booking.response.ReservationCreateResponse;
import com.sytk.booking.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Reservation 도메인 API 제공
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예매 생성
     */
    @PostMapping()
    public ResponseEntity<ReservationCreateResponse> create(@RequestBody @Valid ReservationCreateRequest request) {
        ReservationCreateResponse response = reservationService.reserve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 예매 취소
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예매 확정
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id) {
        reservationService.confirm(id);
        return ResponseEntity.noContent().build();
    }

    // TODO: expire 요청 스케줄러로 이동 예정
    /**
     * 예매 만료
     */
    @PatchMapping("/{id}/expire")
    public ResponseEntity<Void> expire(@PathVariable Long id) {
        reservationService.expire(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예매 환불
     */
    @PatchMapping("/{id}/refund")
    public ResponseEntity<Void> refund(@PathVariable Long id) {
        reservationService.refund(id);
        return ResponseEntity.noContent().build();
    }
}
