package com.doors.crawler.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findAllByOrderByRankAsc();

    // native upsert keeps first_seen_at from the original insert
    @Modifying
    @Query(value = """
      INSERT INTO story (id, hn_rank, title, points, comment_count, first_seen_at, last_seen_at)
      VALUES (:id, :hn_rank, :title, :points, :commentCount, :now, :now)
      ON CONFLICT (id) DO UPDATE SET
        hn_rank = EXCLUDED.hn_rank,
        title = EXCLUDED.title,
        points = EXCLUDED.points,
        comment_count = EXCLUDED.comment_count,
        last_seen_at = EXCLUDED.last_seen_at
      """, nativeQuery = true)
    void upsert(Long id, int hn_rank, String title,
                Integer points, Integer commentCount, Instant now);

    // anything not seen in the latest scan has left the front page
    @Modifying
    @Query("delete from Story s where s.lastSeenAt < :scanStart")
    void deleteNotSeenSince(Instant scanStart);
}
