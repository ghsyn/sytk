package com.sytk.booking.repository;

import com.sytk.booking.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 예매 DB 접근 Repository
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findBySeatId(Long seatId);
}
