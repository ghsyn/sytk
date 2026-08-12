package com.sytk.booking.integration;

import com.sytk.booking.domain.*;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.repository.ReservationRepository;
import com.sytk.booking.repository.SeatGradeRepository;
import com.sytk.booking.repository.SeatRepository;
import com.sytk.booking.request.ReservationCreateRequest;
import com.sytk.booking.response.ReservationCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

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
        /* 예매 가능한 좌석 → 예매 생성 → 201 + 좌석 선점(OCCUPIED) + 예매 RESERVING 검증 */

        @Test
        @DisplayName("예매 가능한 좌석에 예매를 요청하면 201과 함께 예매가 생성되고 좌석이 선점된다.")
        void createReservationOnAvailableSeat() {
            // given : 실제 DB에 예매 가능한(AVAILABLE) 좌석 저장
            Seat seat = persistAvailableSeat();
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .userId(1L)
                    .seatId(seat.getId())
                    .build();

            // when : 실제 HTTP 예매 요청
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
        }
    }

    @Nested
    class 중복_예매 {
        /* 같은 좌석 재요청 → 409, 재고 불변 */
    }

    @Nested
    class 재고_소진 {
        /* 매진 후 요청 → 예매 거부 */
    }

    @Nested
    class 취소_환불 {
        /* 예매 후 취소 → 좌석 반환, 상태 복구 */
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
