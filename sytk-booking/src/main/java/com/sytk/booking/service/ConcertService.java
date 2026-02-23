package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertDetailsResponse;
import com.sytk.booking.response.ConcertListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 공연 단건 조회
     * 시스템용, 실제 사용자 접근 불가
     */
    public ConcertDetailsResponse getDetails(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        return ConcertDetailsResponse.from(concert);
    }

    /**
     * 공연 목록 조회
     * 시스템용, 실제 사용자 접근 불가
     */
    public List<ConcertListResponse> getList() {
        return concertRepository.findAll().stream()
                .map(ConcertListResponse::from)
                .toList();
    }

    /**
     * 공연 등록
     */
    public ConcertDetailsResponse create(ConcertCreateRequest request) {
        Concert concert = concertRepository.save(request.toEntity());

        // TODO: 트랜잭션에 따라 ID 조회 오류 발생 가능성 -> 생성 시 응답 DTO 생성 후 응답 포맷 분리하기
        return ConcertDetailsResponse.from(concert);
    }
}
