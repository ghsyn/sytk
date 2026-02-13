package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.response.ConcertDetailsResponse;
import com.sytk.booking.response.ConcertListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @InjectMocks
    private ConcertService concertService;

    @Mock
    private ConcertRepository concertRepository;

    @Test
    @DisplayName("공연 ID로 단건 조회 시 ConcertDetailsResponse 반환")
    void getDetails_Success() {

        // given
        Concert concert = Concert.builder()
                .title("foo")
                .build();
        given(concertRepository.findById(1L)).willReturn(Optional.of(concert));

        // when
        ConcertDetailsResponse response = concertService.getDetails(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("foo");
        verify(concertRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 공연 ID 조회 시 ConcertNotFoundException 발생")
    void getDetails_Fail_NotFound() {

        // when & then
        assertThatThrownBy(() -> concertService.getDetails(1L))
                .isInstanceOf(ConcertNotFoundException.class);

        // then
        verify(concertRepository).findById(1L);
    }

    @Test
    @DisplayName("공연 목록 조회 시 ConcertListResponse 반환")
    void getList_Success() {

        // given
        List<Concert> concertList = IntStream.range(1, 21)
                .mapToObj(i -> createConcert("title " + i))
                .toList();
        given(concertRepository.findAll()).willReturn(concertList);

        // when
        List<ConcertListResponse> response = concertService.getList();

        // then
        assertThat(response).hasSize(20);
        assertThat(response.get(0).title()).isEqualTo("title 1");
        assertThat(response.get(19).title()).isEqualTo("title 20");

        then(concertRepository).should().findAll();
    }

    private Concert createConcert(String title) {
        return Concert.builder()
                .title(title)
                .startAt(OffsetDateTime.now())
                .location("foo")
                .build();
    }
}