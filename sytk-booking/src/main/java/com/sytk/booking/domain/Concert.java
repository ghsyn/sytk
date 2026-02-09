package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    private Integer runningTime;

    @Column(nullable = false)
    private String location;

    private OffsetDateTime ticketOpenAt;

    private OffsetDateTime ticketCloseAt;

    @OneToMany(mappedBy = "concert")
    private List<SeatGrade> seatGrades = new ArrayList<>();

    @Builder
    public Concert(String title, OffsetDateTime startAt, Integer runningTime, String location,
                   OffsetDateTime ticketOpenAt, OffsetDateTime ticketCloseAt) {
        this.title = title;
        this.startAt = startAt;
        this.runningTime = runningTime;
        this.location = location;
        this.ticketOpenAt = ticketOpenAt;
        this.ticketCloseAt = ticketCloseAt;
    }
}
