package com.sytk.booking.controller;

import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import com.sytk.booking.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Concert 도메인 API 제공
 */
@RestController
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    /**
     * 공연 등록
     */
    @PostMapping("/api/v1/concert")
    public ResponseEntity<ConcertCreateResponse> post(@RequestBody @Valid ConcertCreateRequest request) {
        ConcertCreateResponse response = concertService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 공연 수정
     * 명령 모델과 조회 모델 간의 결합도를 낮추기 위해 (CQRS 패턴 사용 중) ID 및 최소한의 식별정보(예: 제목)만 반환
     */
    @PatchMapping("/api/v1/concert/{id}")
    public ResponseEntity<ConcertEditResponse> edit(@PathVariable Long id, @RequestBody @Valid ConcertEditRequest request) {
        ConcertEditResponse response = concertService.edit(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 공연 삭제
     */
    @DeleteMapping("/api/v1/concert/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        concertService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
