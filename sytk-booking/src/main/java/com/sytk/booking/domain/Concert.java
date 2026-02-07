package com.sytk.booking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
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

    private OffsetDateTime ticketOpenId;

    private OffsetDateTime ticketCloseId;

    @OneToMany(mappedBy = "concert")
    private List<SeatGrade> seatGrades = new ArrayList<>();

    public Concert(String title, OffsetDateTime startAt, Integer runningTime, String location,
                   OffsetDateTime ticketOpenId, OffsetDateTime ticketCloseId) {
        this.title = title;
        this.startAt = startAt;
        this.runningTime = runningTime;
        this.location = location;
        this.ticketOpenId = ticketOpenId;
        this.ticketCloseId = ticketCloseId;
    }
}
