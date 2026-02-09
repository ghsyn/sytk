package com.sytk.booking.repository;

import com.sytk.booking.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공연 DB 접근 Repository
 */
public interface ConcertRepository extends JpaRepository<Concert, Long> {
}
