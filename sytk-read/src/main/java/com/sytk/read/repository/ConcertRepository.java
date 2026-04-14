package com.sytk.read.repository;

import com.sytk.read.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공연 DB 접근 Repository
 */
public interface ConcertRepository extends JpaRepository<Concert, Long> {
}
