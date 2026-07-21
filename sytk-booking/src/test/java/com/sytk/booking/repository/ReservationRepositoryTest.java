package com.sytk.booking.repository;

import com.sytk.booking.domain.Reservation;
import com.sytk.booking.domain.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class ReservationRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("예매를 저장하면 예매 ID가 자동 생성되고 RESERVING 상태와 만료 시각이 함께 저장된다")
    void save_assignsIdAndPersistsInitialState() {
        // given
        Reservation reservation = Reservation.builder()
                .userId(1L)
                .seatId(1L)
                .build();

        // when
        Reservation saved = reservationRepository.save(reservation);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVING);
        assertThat(saved.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("저장된 예매를 findById로 조회하면 저장 시점의 값과 일치한다")
    void findById_returnsPersistedReservation() {
        // given
        Reservation reservation = Reservation.builder()
                .userId(1L)
                .seatId(2L)
                .build();
        Long savedId = reservationRepository.save(reservation).getId();
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Reservation> found = reservationRepository.findById(savedId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
        assertThat(found.get().getSeatId()).isEqualTo(2L);
        assertThat(found.get().getStatus()).isEqualTo(ReservationStatus.RESERVING);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    void findById_returnsEmpty_whenReservationNotExists() {
        // when
        Optional<Reservation> found = reservationRepository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }
}
