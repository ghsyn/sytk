package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertDetailsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @InjectMocks
    private ConcertService concertService;

    @Mock
    private ConcertRepository concertRepository;

    @Test
    @DisplayName("새로운 공연 등록 시 생성된 공연의 상세 정보 반환")
    void create_Success() {

        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("new")
                .startAt(OffsetDateTime.now())
                .location("foo")
                .build();

        Concert concert = createConcert(request.title(), request.startAt(), request.location());
        ReflectionTestUtils.setField(concert, "id", 1L);    // Mockito는 ID 자동 생성 불가, 테스트를 위해 직접 지정
        given(concertRepository.save(any(Concert.class))).willReturn(concert);

        // when
        ConcertDetailsResponse response = concertService.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("new");

        then(concertRepository).should().save(any(Concert.class));
    }

    /**
     * Helper Method
     */
    private Concert createConcert(String title, OffsetDateTime startAt, String location) {
        return Concert.builder()
                .title(title)
                .startAt(startAt)
                .location(location)
                .build();
    }
}