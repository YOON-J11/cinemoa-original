// src/main/java/com/cinemoa/repository/ShowtimeRepository.java
package com.cinemoa.repository;

import com.cinemoa.entity.Showtime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // 1) 특정 영화 + 영화관 (날짜 필터링 없음)
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    List<Showtime> findByMovie_MovieIdAndScreen_Cinema_CinemaIdOrderByStartTimeAsc(
            Long movieId, Long cinemaId);

    // 2) 특정 영화 + 영화관 + 시작시간 범위
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    List<Showtime> findByMovie_MovieIdAndScreen_Cinema_CinemaIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long movieId, Long cinemaId, LocalDateTime startTime, LocalDateTime endTime);

    // 3) 특정 영화 + 영화관의 상영 '날짜' 목록
    @Query("SELECT DISTINCT FUNCTION('DATE', s.startTime) FROM Showtime s " +
            "WHERE s.movie.movieId = :movieId AND s.screen.cinema.cinemaId = :cinemaId " +
            "ORDER BY FUNCTION('DATE', s.startTime) ASC")
    List<LocalDate> findDistinctDatesByMovieAndCinema(
            @Param("movieId") Long movieId,
            @Param("cinemaId") Long cinemaId);

    // 4) 상영 ID로 조회 (연관 로딩)
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    Optional<Showtime> findByShowtimeId(Long showtimeId);

    // ====== 미래 윈도우 자동 생성 (JDK8+ 문자열로 작성) ======
    @Modifying
    @Query(value =
            "INSERT INTO showtimes ( " +
                    "  movie_id, screen_id, start_time, end_time, created_at, updated_at, available_seats, price " +
                    ") " +
                    "SELECT s.movie_id, " +
                    "       s.screen_id, " +
                    "       s.start_time + INTERVAL k WEEK, " +
                    "       s.end_time   + INTERVAL k WEEK, " +
                    "       NOW(), NOW(), " +
                    "       s.available_seats, " +
                    "       s.price " +
                    "FROM showtimes s " +
                    "JOIN (SELECT 0 AS k UNION ALL SELECT 1 UNION ALL SELECT 2) ks " +
                    "WHERE s.start_time >= CURDATE() - INTERVAL 21 DAY " +
                    "  AND (s.start_time + INTERVAL k WEEK) BETWEEN CURDATE() AND CURDATE() + INTERVAL 14 DAY " +
                    "  AND NOT EXISTS ( " +
                    "      SELECT 1 FROM showtimes x " +
                    "      WHERE x.screen_id = s.screen_id " +
                    "        AND x.start_time = s.start_time + INTERVAL k WEEK " +
                    "  )",
            nativeQuery = true)
    int refillFutureWindow14dFromRecent3w();
}
