package com.sytk.booking.service;

import com.sytk.booking.domain.Reservation;
import com.sytk.booking.domain.Seat;
import com.sytk.booking.exception.ReservationNotFoundException;
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
        Seat seat = seatRepository.findById(request.seatId())
                .orElseThrow(SeatNotFoundException::new);
        seat.hold();

        Reservation reservation = reservationRepository.save(request.toEntity());

        return ReservationCreateResponse.from(reservation);
    }

    /**
     * 예매 취소
     */
    @Transactional
    public void cancel(ReservationCancelRequest request) {
        Reservation reservation = getReservation(request.id());
        reservation.cancel();

        getSeat(reservation.getSeatId()).release();
    }

    /**
     * 예매 확정
     */
    @Transactional
    public void confirm(ReservationConfirmRequest request) {
        Reservation reservation = getReservation(request.id());
        reservation.confirm();

        getSeat(reservation.getSeatId()).sell();
    }

    /**
     * 예매 만료(타임아웃)
     */
    @Transactional
    public void expire(ReservationExpireRequest request) {
        Reservation reservation = getReservation(request.id());
        reservation.expire();

        getSeat(reservation.getSeatId()).release();
    }

    /**
     * 예매 환불
     */
    @Transactional
    public void refund(ReservationRefundRequest request) {
        Reservation reservation = getReservation(request.id());
        reservation.refund();

        getSeat(reservation.getSeatId()).refund();
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(ReservationNotFoundException::new);
    }

    private Seat getSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(SeatNotFoundException::new);
    }
}
