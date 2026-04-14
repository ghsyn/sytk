package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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

    @OneToMany(mappedBy = "concert")
    private List<SeatGrade> seatGrades = new ArrayList<>();

    @Builder
    public Concert(String title, OffsetDateTime startAt, Integer runningTime, String venue,
                   OffsetDateTime ticketOpenAt, OffsetDateTime ticketCloseAt) {
        this.title = title;
        this.startAt = startAt;
        this.runningTime = runningTime;
        this.venue = venue;
        this.ticketOpenAt = ticketOpenAt;
        this.ticketCloseAt = ticketCloseAt;
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
    }
}
