package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.exception.DuplicateConcertException;
import com.sytk.booking.exception.ErrorCode;
import com.sytk.booking.exception.NotChangedException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static java.time.OffsetDateTime.now;
import static java.time.OffsetDateTime.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    /**
     * 공연 등록 테스트
     */
    @Test
    @DisplayName("[성공케이스] 새로운 공연 등록 시 생성된 공연의 ID 및 제목 반환")
    void create_success() {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("new")
                .startAt(now())
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

    // TODO: JUnit5(Assertions) -> AssertJ(assertThatThrownBy)
    @Test
    @DisplayName("[실패케이스 - 중복 검증] 이미 존재하는 공연 제목 등록 시 DUPLICATE_CONCERT 에러 발생")
    void create_fail_duplicateTitle() {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("공연 제목입니다.")
                .startAt(now())
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
     * 공연 수정 테스트
     */
    @Test
    @DisplayName("[성공케이스] 공연 제목 수정 시 해당 공연의 ID 및 수정된 제목 반환")
    void edit_success() {
        // given
        Long concertId = 1L;
        Concert concert = createConcert("foo", now(), "10글자 이상 bar");
        ReflectionTestUtils.setField(concert, "id", concertId);

        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("new title")
                .build();

        given(concertRepository.findById(concertId)).willReturn(Optional.of(concert));

        // when
        ConcertEditResponse response = concertService.edit(concertId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(concertId);
        assertThat(response.title()).isEqualTo("new title");

        // verify
        then(concertRepository).should(times(1)).findById(concertId);
    }

    @Test
    @DisplayName("[실패케이스 - 필드 검증] 공연 수정 시 변경 사항이 없다면 NotChangedException 발생")
    void edit_fail_notChanged() {
        // given
        Long concertId = 1L;
        String title = "foo";
        OffsetDateTime startAt = parse("2026-04-14T00:00:00+09:00");
        String venue = "10글자 이상 bar";

        Concert concert = createConcert(title, startAt, venue);
        ReflectionTestUtils.setField(concert, "id", concertId);

        ConcertEditRequest request = ConcertEditRequest.builder()
                .title(title)
                .startAt(startAt)
                .venue(venue)
                .build();

        given(concertRepository.findById(concertId)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> concertService.edit(concertId, request))
                .isInstanceOf(NotChangedException.class);

        // verify
        then(concertRepository).should(times(1)).findById(concertId);
        then(concertRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 공연 수정 시 ConcertNotFoundException 발생")
    void edit_fail_notFound() {
        // given
        Long notExistId = 1L;
        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("new title")
                .startAt(now())
                .venue("10글자 이상의 공연 장소")
                .build();

        given(concertRepository.findById(notExistId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> concertService.edit(notExistId, request))
                .isInstanceOf(ConcertNotFoundException.class);

        // verify
        then(concertRepository).should(times(1)).findById(notExistId);
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