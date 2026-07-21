package com.sytk.booking.repository;

import com.sytk.booking.config.QuerydslConfig;
import com.sytk.booking.domain.Concert;
import com.sytk.booking.domain.Reservation;
import com.sytk.booking.domain.Seat;
import com.sytk.booking.domain.SeatGrade;
import com.sytk.booking.domain.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({QuerydslConfig.class, ReservationQueryRepository.class})
class ReservationQueryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ReservationQueryRepository reservationQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("공연에 속한 좌석에 예매 내역이 존재하면 true를 반환한다")
    void existsByConcertId_returnsTrue_whenReservationExists() {
        // given
        Seat seat = persistSeat();
        Reservation reservation = Reservation.builder()
                .userId(1L)
                .seatId(seat.getId())
                .build();
        entityManager.persist(reservation);
        entityManager.flush();
        entityManager.clear();

        // when
        boolean exists = reservationQueryRepository.existsByConcertId(seat.getSeatGrade().getConcert().getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("공연에 속한 좌석에 예매 내역이 없으면 false를 반환한다")
    void existsByConcertId_returnsFalse_whenReservationNotExists() {
        // given
        Seat seat = persistSeat();
        entityManager.flush();
        entityManager.clear();

        // when
        boolean exists = reservationQueryRepository.existsByConcertId(seat.getSeatGrade().getConcert().getId());

        // then
        assertThat(exists).isFalse();
    }

    private Seat persistSeat() {
        Concert concert = Concert.builder()
                .title("싸이 흠뻑쇼")
                .startAt(OffsetDateTime.now().plusDays(30))
                .venue("잠실종합운동장")
                .build();
        entityManager.persist(concert);

        SeatGrade seatGrade = SeatGrade.builder()
                .concert(concert)
                .name("VIP")
                .price(BigDecimal.valueOf(150000))
                .totalSeatCount(1)
                .build();
        entityManager.persist(seatGrade);

        Seat seat = Seat.builder()
                .seatGrade(seatGrade)
                .number(1)
                .status(SeatStatus.AVAILABLE)
                .build();
        entityManager.persist(seat);

        return seat;
    }
}
