package com.sytk.booking.repository;

import com.sytk.booking.domain.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
}
