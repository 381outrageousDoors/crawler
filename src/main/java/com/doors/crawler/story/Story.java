package com.doors.crawler.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "story")
public class Story {
    @Id
    Long id;
    @Column(name = "hn_rank") int rank;
    String title;
    String url;
    Integer points;
    Integer commentCount;
    Instant firstSeenAt;
    Instant lastSeenAt;
}
