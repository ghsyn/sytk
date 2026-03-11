package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 공연 등록
     */
    public ConcertDetailsResponse create(ConcertCreateRequest request) {
        Concert concert = concertRepository.save(request.toEntity());

        // TODO: 트랜잭션에 따라 ID 조회 오류 발생 가능성 -> 생성 시 응답 DTO 생성 후 응답 포맷 분리하기
        return ConcertDetailsResponse.from(concert);
    }
}
