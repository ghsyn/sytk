package com.sytk.booking.domain;

import com.sytk.booking.exception.InvalidRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    private Integer runningTime;

    @Column(nullable = false)
    private String venue;

    private OffsetDateTime ticketOpenAt;

    private OffsetDateTime ticketCloseAt;

    @Builder
    public Concert(String title, OffsetDateTime startAt, Integer runningTime, String venue,
                   OffsetDateTime ticketOpenAt, OffsetDateTime ticketCloseAt) {
        this.title = title;
        this.startAt = startAt;
        this.runningTime = runningTime;
        this.venue = venue;
        this.ticketOpenAt = ticketOpenAt;
        this.ticketCloseAt = ticketCloseAt;

        validate();
    }

    public ConcertEditor.ConcertEditorBuilder toEditor() {
        return ConcertEditor.builder()
                .title(this.title)
                .startAt(this.startAt)
                .runningTime(this.runningTime)
                .venue(this.venue)
                .ticketOpenAt(this.ticketOpenAt)
                .ticketCloseAt(this.ticketCloseAt);
    }

    public void edit(ConcertEditor editor) {
        this.title = editor.title();
        this.startAt = editor.startAt();
        this.runningTime = editor.runningTime();
        this.venue = editor.venue();
        this.ticketOpenAt = editor.ticketOpenAt();
        this.ticketCloseAt = editor.ticketCloseAt();

        validate();
    }

    private void validate() {
        // 1. 티켓 오픈 시간은 마감 시간보다 빨라야 합니다.
        if (this.ticketOpenAt != null && this.ticketCloseAt != null &&
                this.ticketOpenAt.isAfter(this.ticketCloseAt)) {
            throw new InvalidRequestException();
        }

        // 2. 티켓 예매 마감 시간은 공연 시작 시간보다 빨라야 합니다.
        if (this.ticketCloseAt != null && this.startAt != null &&
                this.ticketCloseAt.isAfter(this.startAt)) {
            throw new InvalidRequestException();
        }
    }
}
