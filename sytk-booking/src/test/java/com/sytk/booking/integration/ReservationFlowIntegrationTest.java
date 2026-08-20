package com.sytk.booking.integration;

import com.sytk.booking.domain.*;
import com.sytk.booking.exception.ErrorResponse;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.repository.ReservationRepository;
import com.sytk.booking.repository.SeatGradeRepository;
import com.sytk.booking.repository.SeatRepository;
import com.sytk.booking.request.ReservationCreateRequest;
import com.sytk.booking.response.ReservationCreateResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    ConcertRepository concertRepository;

    @Autowired
    SeatGradeRepository seatGradeRepository;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    ReservationRepository reservationRepository;

    /**
     * 실제 HTTP·DB를 사용하는 통합 테스트라 TestContainer 내에 서버 트랜잭션이 커밋됨
     * 테스트 간 격리를 위해 매 실행 전 데이터 정리(FK 역순 삭제)
     */
    @BeforeEach
    void cleanUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        seatGradeRepository.deleteAll();
        concertRepository.deleteAll();
    }

    @Nested
    class 정상_예매 {
        @Test
        @DisplayName("예매 가능한 좌석(AVAILABLE)에 예매를 요청하면 예매 생성(RESERVING) 및 좌석 선점(OCCUPIED) 후 201 반환")
        void createReservationOnAvailableSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            // when
            ResponseEntity<ReservationCreateResponse> response = testRestTemplate.postForEntity(
                    "/api/v1/reservations", request, ReservationCreateResponse.class);

            // then : 응답 상태·바디 검증 (201 Created + RESERVING)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(ReservationStatus.RESERVING);

            // then : DB 좌석 상태 검증 (AVAILABLE → OCCUPIED 선점)
            Seat persistedSeat = seatRepository.findById(seat.getId()).orElseThrow();
            assertThat(persistedSeat.getStatus()).isEqualTo(SeatStatus.OCCUPIED);

            // then : DB 예매 저장 검증 (RESERVING, 요청 값 일치)
            Reservation persistedReservation = reservationRepository.findById(response.getBody().id())
                    .orElseThrow();
            assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.RESERVING);
            assertThat(persistedReservation.getUserId()).isEqualTo(1L);
            assertThat(persistedReservation.getSeatId()).isEqualTo(seat.getId());
            assertThat(persistedReservation.getExpiredAt()).isAfter(now()).isBefore(now().plusMinutes(61));
        }

        @Test
        @DisplayName("선점된 좌석(OCCUPIED)의 예매 결제 시 예매 확정(CONFIRMED) 및 좌석 판매 완료(SOLD) 후 204 반환")
        void confirmReservationOnOccupiedSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            Long reservationId = Objects.requireNonNull(testRestTemplate.postForEntity(
                    "/api/v1/reservations", request, ReservationCreateResponse.class).getBody()).id();

            // when
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    "/api/v1/reservations/{id}/confirm", HttpMethod.PATCH, null, Void.class, reservationId);

            // then : 204 + 좌석 SOLD + 예매 CONFIRMED
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.SOLD);
            assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus())
                    .isEqualTo(ReservationStatus.CONFIRMED);
        }
    }

    @Nested
    class 중복_예매 {
        @Test
        @DisplayName("선점 중 좌석(OCCUPIED) 예매 요청 시 선점 상태(RESERVING/OCCUPIED) 유지 후 409 및 SEAT_ALREADY_OCCUPIED 예외 반환")
        void createReservationOnOccupiedSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest firstRequest = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            ResponseEntity<ReservationCreateResponse> firstResponse = testRestTemplate.postForEntity(
                    "/api/v1/reservations", firstRequest, ReservationCreateResponse.class);

            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(firstResponse.getBody()).isNotNull();
            assertThat(firstResponse.getBody().status()).isEqualTo(ReservationStatus.RESERVING);

            ReservationCreateRequest secondRequest = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            // when
            ResponseEntity<ErrorResponse> secondResponse = testRestTemplate.postForEntity(
                    "/api/v1/reservations", secondRequest, ErrorResponse.class);

            // then : 응답 상태·바디 검증 (409 Conflict + ErrorResponse: SEAT_ALREADY_OCCUPIED)
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(secondResponse.getBody()).isNotNull();
            assertThat(secondResponse.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(secondResponse.getBody().message()).isEqualTo("이미 예매된 좌석입니다.");

            // then : DB 좌석 상태 검증 (OCCUPIED 유지)
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.OCCUPIED);

            // then : DB 예매 상태 검증 (첫 번째 요청만 존재, 첫 번째 요청 예매 상태 RESERVING 유지)
            assertThat(reservationRepository.findBySeatId(seat.getId()))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getId()).isEqualTo(firstResponse.getBody().id());
                        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVING);
                    });
        }
    }

    @Nested
    class 재고_소진 {
        @Test
        @DisplayName("판매 확정된 좌석(SOLD) 예매 요청 시 판매 완료 상태(CONFIRMED/SOLD) 유지 후 409 및 SEAT_ALREADY_OCCUPIED 예외 반환")
        void createReservationOnSoldSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            Long reservationId = Objects.requireNonNull(testRestTemplate.postForEntity(
                    "/api/v1/reservations", request, ReservationCreateResponse.class).getBody()).id();

            ResponseEntity<Void> confirmResponse = testRestTemplate.exchange(
                    "/api/v1/reservations/{id}/confirm", HttpMethod.PATCH, null, Void.class, reservationId);

            assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.SOLD);

            ReservationCreateRequest lateRequest = ReservationCreateRequest.builder()
                    .userId(2L)
                    .seatId(seat.getId())
                    .build();

            // when
            ResponseEntity<ErrorResponse> lateResponse = testRestTemplate.postForEntity(
                    "/api/v1/reservations", lateRequest, ErrorResponse.class);

            // then : 응답 상태·바디 검증 (409 Conflict + ErrorResponse: SEAT_ALREADY_OCCUPIED)
            assertThat(lateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(lateResponse.getBody()).isNotNull();
            assertThat(lateResponse.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
            assertThat(lateResponse.getBody().message()).isEqualTo("이미 예매된 좌석입니다.");

            // then : DB 좌석 상태 검증 (SOLD 유지)
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.SOLD);

            // then : DB 예매 상태 검증 (첫 번째 요청만 존재, 첫 번째 요청 예매 상태 CONFIRMED 유지)
            assertThat(reservationRepository.findBySeatId(seat.getId()))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getId()).isEqualTo(reservationId);
                        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                    });
        }
    }

    @Nested
    class 취소_환불 {
        @Test
        @DisplayName("선점한 좌석(OCCUPIED) 취소 요청 시 상태 복구 (AVAILABLE/CANCELED) 후 204 반환")
        void cancelReservationOnSoldSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            Long reservationId = Objects.requireNonNull(testRestTemplate.postForEntity(
                    "/api/v1/reservations", request, ReservationCreateResponse.class).getBody()).id();

            // when
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    "/api/v1/reservations/{id}/cancel", HttpMethod.PATCH, null, Void.class, reservationId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }

        @Test
        @DisplayName("구매한 좌석(SOLD) 환불 요청 시 상태 복구 (AVAILABLE/REFUNDED) 후 204 반환")
        void refundReservationOnSeat() {
            // given
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            Long reservationId = Objects.requireNonNull(testRestTemplate.postForEntity(
                    "/api/v1/reservations", request, ReservationCreateResponse.class).getBody()).id();
            testRestTemplate.exchange(
                    "/api/v1/reservations/{id}/confirm", HttpMethod.PATCH, null, Void.class, reservationId);

            // when
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    "/api/v1/reservations/{id}/refund", HttpMethod.PATCH, null, Void.class, reservationId);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus()).isEqualTo(ReservationStatus.REFUNDED);
        }
    }

    /**
     * 예매 가능한(AVAILABLE) 좌석 1개를 실제 DB에 저장한다.
     * Concert → SeatGrade → Seat 순으로 FK 제약을 만족시킨다.
     */
    private Seat persistAvailableSeat() {
        Concert concert = concertRepository.save(Concert.builder()
                .title("통합 테스트 공연")
                .startAt(now().plusDays(30))
                .venue("올림픽공원 체조경기장")
                .build());

        SeatGrade seatGrade = seatGradeRepository.save(SeatGrade.builder()
                .name("VIP")
                .price(BigDecimal.valueOf(150_000))
                .totalSeatCount(1)
                .concert(concert)
                .build());

        return seatRepository.save(Seat.builder()
                .seatGrade(seatGrade)
                .number(1)
                .status(SeatStatus.AVAILABLE)
                .build());
    }
}
