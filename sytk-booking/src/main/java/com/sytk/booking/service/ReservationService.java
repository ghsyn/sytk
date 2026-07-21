package com.sytk.booking.service;

import com.sytk.booking.domain.Reservation;
import com.sytk.booking.exception.SeatNotFoundException;
import com.sytk.booking.repository.ReservationRepository;
import com.sytk.booking.repository.SeatRepository;
import com.sytk.booking.request.*;
import com.sytk.booking.response.ReservationCreateResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    /**
     * 예매 생성
     */
    @Transactional
    public ReservationCreateResponse reserve(ReservationCreateRequest request) {
        Reservation reservation = reservationRepository.save(request.toEntity());

        seatRepository.findById(request.seatId())
                .orElseThrow(SeatNotFoundException::new)
                .hold();

        return ReservationCreateResponse.from(reservation);
    }

    /**
     * 예매 취소
     */
    public void cancel(ReservationCancelRequest request) {

    }

    /**
     * 예매 확정
     */
    public void confirm(ReservationConfirmRequest request) {

    }

    /**
     * 예매 만료(타임아웃)
     */
    public void expire(ReservationExpireRequest request) {

    }

    /**
     * 예매 환불
     */
    public void refund(ReservationRefundRequest request) {

    }
}
