package com.sytk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.request.SeatGradeCreateRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import com.sytk.booking.service.ConcertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConcertController.class)
@AutoConfigureRestDocs(uriScheme = "http", uriHost = "localhost", uriPort = 8080)
@ExtendWith(RestDocumentationExtension.class)
class ConcertControllerDocTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConcertService concertService; // 비즈니스 로직 모킹 처리

    @Test
    void concert_create() throws Exception {
        // given
        SeatGradeCreateRequest seatGradeCreateRequest = SeatGradeCreateRequest.builder()
                .name("VIP")
                .price(BigDecimal.valueOf(100000))
                .totalSeatCount(100)
                .build();

        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("공연 제목")
                .startAt(now())
                .venue("공연 장소")
                .runningTime(100)
                .ticketOpenAt(now().plusDays(1))
                .ticketCloseAt(now().plusDays(7))
                .seatGradeList(List.of(seatGradeCreateRequest))
                .build();

        ConcertCreateResponse response = ConcertCreateResponse.builder()
                .id(1L)
                .title("공연 제목")
                .build();
        given(concertService.create(any(ConcertCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("concert-create",
                        requestFields(
                                fieldWithPath("title").description("공연 제목"),
                                fieldWithPath("startAt").description("공연 시작 시간 (ISO 8601 형식)"),
                                fieldWithPath("venue").description("공연 장소"),
                                fieldWithPath("runningTime").description("공연 러닝타임 (분)").optional(),
                                fieldWithPath("ticketOpenAt").description("티켓 오픈 시간 (ISO 8601 형식)").optional(),
                                fieldWithPath("ticketCloseAt").description("티켓 마감 시간 (ISO 8601 형식)").optional(),
                                fieldWithPath("seatGradeList").description("좌석 등급 리스트").optional(),

                                fieldWithPath("seatGradeList").description("좌석 등급 리스트"),
                                fieldWithPath("seatGradeList[].name").description("좌석 등급명"),
                                fieldWithPath("seatGradeList[].price").description("좌석 가격"),
                                fieldWithPath("seatGradeList[].totalSeatCount").description("해당 등급의 총 좌석 수")
                        ),
                        responseFields(
                                fieldWithPath("id").description("생성된 공연 ID"),
                                fieldWithPath("title").description("생성된 공연 제목")
                        )
                ));
    }

    @Test
    void concert_edit() throws Exception {
        // given
        Long concertId = 1L;
        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("공연 제목 - 수정")
                .startAt(now())
                .venue("공연 장소")
                .runningTime(100)
                .ticketOpenAt(now().plusDays(1))
                .ticketCloseAt(now().plusDays(7))
                .build();

        ConcertEditResponse response = ConcertEditResponse.builder()
                .id(concertId)
                .title("공연 제목 - 수정")
                .build();
        given(concertService.edit(eq(concertId), any(ConcertEditRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/concert/{id}", concertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("concert-edit",
                        pathParameters(
                                parameterWithName("id").description("수정할 공연 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").description("수정할 공연 제목").optional(),
                                fieldWithPath("startAt").description("수정할 공연 시작 시간 (ISO 8601 형식)").optional(),
                                fieldWithPath("venue").description("수정할 공연 장소").optional(),
                                fieldWithPath("runningTime").description("수정할 공연 러닝타임 (분)").optional(),
                                fieldWithPath("ticketOpenAt").description("수정할 티켓 오픈 시간 (ISO 8601 형식)").optional(),
                                fieldWithPath("ticketCloseAt").description("수정할 티켓 마감 시간 (ISO 8601 형식)").optional()

                        ),
                        responseFields(
                                fieldWithPath("id").description("수정된 공연 ID"),
                                fieldWithPath("title").description("식별을 위한 공연 제목")
                        )
                ));
    }

    @Test
    void concert_delete() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/concert/{id}", 1L))
                .andExpect(status().isNoContent())
                .andDo(document("concert-delete",
                        pathParameters(
                                parameterWithName("id").description("삭제할 공연 ID")
                        )
                ));
    }
}