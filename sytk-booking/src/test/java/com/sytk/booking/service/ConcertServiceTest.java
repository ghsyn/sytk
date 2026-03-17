package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.exception.DuplicateConcertException;
import com.sytk.booking.exception.ErrorCode;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @InjectMocks
    private ConcertService concertService;

    @Mock
    private ConcertRepository concertRepository;

    @Test
    @DisplayName("새로운 공연 등록 시 생성된 공연의 ID 및 제목 반환")
    void create_success() {

        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("new")
                .startAt(OffsetDateTime.now())
                .venue("foo")
                .build();

        /* Mockito는 `@GeneratedValue` 불가하므로 "id = null"
              -> 테스트를 위해 서비스 내부의 save()가 호출되었을 때 "id = 1L" 반환되도록 강제 지정 */
        Concert concert = createConcert(request.title(), request.startAt(), request.venue());
        ReflectionTestUtils.setField(concert, "id", 1L);
        given(concertRepository.save(any(Concert.class))).willReturn(concert);

        // when
        ConcertCreateResponse response = concertService.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("new");

        then(concertRepository).should().save(any(Concert.class));
    }

    @Test
    @DisplayName("이미 존재하는 공연 제목 등록 시 DUPLICATE_CONCERT 에러 발생")
    void create_fail_duplicateTitle() {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("공연 제목입니다.")
                .startAt(OffsetDateTime.now())
                .venue("공연 장소입니다.")
                .build();

        given(concertRepository.existsByTitle(request.title())).willReturn(true);

        // when
        DuplicateConcertException exception = assertThrows(DuplicateConcertException.class, () -> {
            concertService.create(request);
        });

        // then
        ErrorCode errorCode = exception.getErrorCode();
        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, errorCode.getStatus()),
                () -> assertEquals("이미 존재하는 공연입니다.", errorCode.getMessage()),
                () -> assertEquals(ErrorCode.DUPLICATE_CONCERT, errorCode)
        );

        // verify
        then(concertRepository).should(times(1)).existsByTitle(request.title());
        then(concertRepository).should(never()).save(any());
    }

    /**
     * Helper Method
     */
    private Concert createConcert(String title, OffsetDateTime startAt, String venue) {
        return Concert.builder()
                .title(title)
                .startAt(startAt)
                .venue(venue)
                .build();
    }
}