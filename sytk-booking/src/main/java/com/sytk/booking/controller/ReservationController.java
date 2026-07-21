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
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예매 생성
     */
    @PostMapping("/api/v1/reservations")
    public ResponseEntity<ReservationCreateResponse> create(@RequestBody @Valid ReservationCreateRequest request) {
        ReservationCreateResponse response = reservationService.reserve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 예매 취소
     */
    @PatchMapping("/api/v1/reservations/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예매 확정
     */
    @PatchMapping("/api/v1/reservations/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id) {
        reservationService.confirm(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예매 만료
     */
    @PatchMapping("/api/v1/reservations/{id}/expire")
    public ResponseEntity<Void> expire(@PathVariable Long id) {
        reservationService.expire(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예매 환불
     */
    @PatchMapping("/api/v1/reservations/{id}/refund")
    public ResponseEntity<Void> refund(@PathVariable Long id) {
        reservationService.refund(id);
        return ResponseEntity.noContent().build();
    }
}
