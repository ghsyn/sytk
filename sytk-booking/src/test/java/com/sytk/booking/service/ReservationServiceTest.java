package com.sytk.booking.service;

import com.sytk.booking.domain.*;
import com.sytk.booking.exception.*;
import com.sytk.booking.repository.ReservationRepository;
import com.sytk.booking.repository.SeatRepository;
import com.sytk.booking.request.ReservationCancelRequest;
import com.sytk.booking.request.ReservationConfirmRequest;
import com.sytk.booking.request.ReservationCreateRequest;
import com.sytk.booking.request.ReservationExpireRequest;
import com.sytk.booking.request.ReservationRefundRequest;
import com.sytk.booking.response.ReservationCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    /**
     * 예매 생성 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예약 가능한(AVAILABLE) 좌석 선점 시 대기(RESERVING) 상태의 예매 생성 후 좌석 상태 선점(OCCUPIED)으로 변경")
    void reserve_success() {
        // given
        Long userId = 1L;
        Long seatId = 1L;
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .userId(userId)
                .seatId(seatId)
                .build();

        Seat seat = createSeat(seatId, SeatStatus.AVAILABLE);
        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        Reservation reservation = createReservation(1L, seatId);
        given(reservationRepository.save(any(Reservation.class))).willReturn(reservation);

        // when
        ReservationCreateResponse response = reservationService.reserve(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ReservationStatus.RESERVING);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.OCCUPIED);

        // verify
        then(seatRepository).should(times(1)).findById(seatId);
        then(reservationRepository).should(times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 좌석으로 예매 시 SeatNotFoundException 발생")
    void reserve_fail_seatNotFound() {
        // given
        Long notExistSeatId = 999L;
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .userId(1L)
                .seatId(notExistSeatId)
                .build();

        given(seatRepository.findById(notExistSeatId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.reserve(request))
                .isInstanceOf(SeatNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);

        // verify
        then(seatRepository).should(times(1)).findById(notExistSeatId);
        then(reservationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("[실패케이스 - 리소스 충돌] 이미 선점된(OCCUPIED) 좌석 예매 시 InvalidSeatStatusTransitionException 발생")
    void reserve_fail_seatAlreadyOccupied() {
        // given
        Long seatId = 1L;
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .userId(1L)
                .seatId(seatId)
                .build();

        Seat seat = createSeat(seatId, SeatStatus.OCCUPIED);
        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // when & then
        assertThatThrownBy(() -> reservationService.reserve(request))
                .isInstanceOf(InvalidSeatStatusTransitionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEAT_STATUS_TRANSITION);

        // verify
        then(seatRepository).should(times(1)).findById(seatId);
        then(reservationRepository).should(never()).save(any());
    }

    /**
     * 결제 전 예매 취소 테스트
     */
    @Test
    @DisplayName("[성공케이스] 대기(RESERVING) 상태 예매 취소 시 예매 상태 취소(CANCELED)로 변경 후 좌석 상태 예약 가능(AVAILABLE)으로 복구")
    void cancel_success() {
        // given
        Long reservationId = 1L;
        ReservationCancelRequest request = ReservationCancelRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.OCCUPIED);
        Reservation reservation = createReservation(reservationId, seat.getId());
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when
        reservationService.cancel(request);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 취소 시 ReservationNotFoundException 발생")
    void cancel_fail_notFound() {
        // given
        Long notExistReservationId = 999L;
        ReservationCancelRequest request = ReservationCancelRequest.builder()
                .id(notExistReservationId)
                .build();

        given(reservationRepository.findById(notExistReservationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.cancel(request))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);

        // verify
        then(reservationRepository).should(times(1)).findById(notExistReservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 상태 전이] 이미 취소된(CANCELED) 예매 재취소 시 InvalidReservationStatusTransitionException 발생")
    void cancel_fail_alreadyCanceled() {
        // given
        Long reservationId = 1L;
        ReservationCancelRequest request = ReservationCancelRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.AVAILABLE);
        Reservation reservation = createReservation(reservationId, seat.getId());
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CANCELED);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancel(request))
                .isInstanceOf(InvalidReservationStatusTransitionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    /**
     * 예매 완료(결제 완료) 테스트
     */
    @Test
    @DisplayName("[성공케이스] 대기(RESERVING) 상태 예매 확정 시 예매 상태 확정(CONFIRMED)으로 변경 후 좌석 상태 판매 완료(SOLD)로 변경")
    void confirm_success() {
        // given
        Long reservationId = 1L;
        ReservationConfirmRequest request = ReservationConfirmRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.OCCUPIED);
        Reservation reservation = createReservation(reservationId, seat.getId());
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when
        reservationService.confirm(request);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 확정 시 ReservationNotFoundException 발생")
    void confirm_fail_notFound() {
        // given
        Long notExistReservationId = 999L;
        ReservationConfirmRequest request = ReservationConfirmRequest.builder()
                .id(notExistReservationId)
                .build();

        given(reservationRepository.findById(notExistReservationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.confirm(request))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);

        // verify
        then(reservationRepository).should(times(1)).findById(notExistReservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 상태 전이] 이미 취소된(CANCELED) 예매 확정 시 InvalidReservationStatusTransitionException 발생")
    void confirm_fail_alreadyCanceled() {
        // given
        Long reservationId = 1L;
        ReservationConfirmRequest request = ReservationConfirmRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.AVAILABLE);
        Reservation reservation = createReservation(reservationId, seat.getId());
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CANCELED);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.confirm(request))
                .isInstanceOf(InvalidReservationStatusTransitionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    /**
     * 예매 만료 테스트
     */
    @Test
    @DisplayName("[성공케이스] 대기(RESERVING) 상태 타임아웃 시 예매 상태 만료(EXPIRED)로 변경 후 좌석 상태 예약 가능(AVAILABLE)으로 복구")
    void expire_success() {
        // given
        Long reservationId = 1L;
        ReservationExpireRequest request = ReservationExpireRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.OCCUPIED);
        Reservation reservation = createReservation(reservationId, seat.getId());
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when
        reservationService.expire(request);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 만료 처리 시 ReservationNotFoundException 발생")
    void expire_fail_notFound() {
        // given
        Long notExistId = 999L;
        ReservationExpireRequest request = ReservationExpireRequest.builder()
                .id(notExistId)
                .build();

        given(reservationRepository.findById(notExistId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.expire(request))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);

        // verify
        then(reservationRepository).should(times(1)).findById(notExistId);
    }

    @Test
    @DisplayName("[실패케이스 - 상태 전이] 이미 확정된(CONFIRMED) 예매 만료 처리 시 InvalidReservationStatusTransitionException 발생")
    void expire_fail_alreadyConfirmed() {
        // given
        Long reservationId = 1L;
        ReservationExpireRequest request = ReservationExpireRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.SOLD);
        Reservation reservation = createReservation(reservationId, seat.getId());
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.expire(request))
                .isInstanceOf(InvalidReservationStatusTransitionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    /**
     * 결제 후 환불 테스트
     */
    @Test
    @DisplayName("[성공케이스] 확정(CONFIRMED) 상태 예매 환불 시 예매 상태 환불(REFUNDED)로 변경 후 좌석 상태 예약 가능(AVAILABLE)으로 복구")
    void refund_success() {
        // given
        Long reservationId = 1L;
        ReservationRefundRequest request = ReservationRefundRequest.builder()
                .id(reservationId)
                .build();

        Seat seat = createSeat(1L, SeatStatus.SOLD);
        Reservation reservation = createReservation(reservationId, seat.getId());
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when
        reservationService.refund(request);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REFUNDED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 환불 시 ReservationNotFoundException 발생")
    void refund_fail_notFound() {
        // given
        Long notExistId = 999L;
        ReservationRefundRequest request = ReservationRefundRequest.builder()
                .id(notExistId)
                .build();

        given(reservationRepository.findById(notExistId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.refund(request))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);

        // verify
        then(reservationRepository).should(times(1)).findById(notExistId);
    }

    @Test
    @DisplayName("[실패케이스 - 상태 전이] 대기(RESERVING) 상태 예매 환불 시 InvalidReservationStatusTransitionException 발생")
    void refund_fail_notConfirmedYet() {
        // given
        Long reservationId = 1L;
        Seat seat = createSeat(1L, SeatStatus.OCCUPIED);
        Reservation reservation = createReservation(reservationId, seat.getId());

        ReservationRefundRequest request = ReservationRefundRequest.builder()
                .id(reservationId)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.refund(request))
                .isInstanceOf(InvalidReservationStatusTransitionException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);

        // verify
        then(reservationRepository).should(times(1)).findById(reservationId);
    }

    /**
     * Helper Method
     */
    private Seat createSeat(Long id, SeatStatus status) {
        Concert concert = Concert.builder()
                .title("테스트 공연")
                .startAt(now())
                .venue("테스트 공연 장소")
                .build();

        SeatGrade seatGrade = SeatGrade.builder()
                .name("VIP")
                .price(BigDecimal.valueOf(100000))
                .totalSeatCount(10)
                .concert(concert)
                .build();

        Seat seat = Seat.builder()
                .seatGrade(seatGrade)
                .number(1)
                .status(status)
                .build();

        if (id != null) {
            ReflectionTestUtils.setField(seat, "id", id);
        }
        return seat;
    }

    private Reservation createReservation(Long id, Long seatId) {
        Reservation reservation = Reservation.builder()
                .userId(1L)
                .seatId(seatId)
                .build();

        if (id != null) {
            ReflectionTestUtils.setField(reservation, "id", id);
        }
        return reservation;
    }
}
