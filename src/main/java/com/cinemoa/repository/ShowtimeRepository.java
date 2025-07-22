// src/main/java/com/cinemoa/repository/ShowtimeRepository.java
package com.cinemoa.repository;

import com.cinemoa.entity.Showtime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // LocalDate 임포트
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // 1. 특정 영화와 영화관에 대한 상영 시간 목록 (날짜 필터링 없음)
    // @EntityGraph를 사용하여 Movie, Screen, Cinema를 즉시 로딩
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    List<Showtime> findByMovie_MovieIdAndScreen_Cinema_CinemaIdOrderByStartTimeAsc(
            Long movieId, Long cinemaId);

    // 2. 특정 영화, 영화관, 시작 시간 범위에 대한 상영 시간 목록
    // @EntityGraph를 사용하여 Movie, Screen, Cinema를 즉시 로딩
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    List<Showtime> findByMovie_MovieIdAndScreen_Cinema_CinemaIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long movieId, Long cinemaId, LocalDateTime startTime, LocalDateTime endTime);

    // 3. 특정 영화와 영화관에 대해 상영 가능한 날짜 목록 조회 (새로운 메서드)
    // DISTINCT와 FUNCTION('DATE')를 사용하여 날짜만 추출
    @Query("SELECT DISTINCT FUNCTION('DATE', s.startTime) FROM Showtime s " +
            "WHERE s.movie.movieId = :movieId AND s.screen.cinema.cinemaId = :cinemaId " +
            "ORDER BY FUNCTION('DATE', s.startTime) ASC")
    List<LocalDate> findDistinctDatesByMovieAndCinema(
            @Param("movieId") Long movieId,
            @Param("cinemaId") Long cinemaId);

    // 4. 특정 상영 시간 ID로 상영 정보를 조회하며, Movie, Screen, Cinema 정보도 함께 가져오는 쿼리
    // @EntityGraph를 사용하여 Movie, Screen, Cinema를 즉시 로딩
    @EntityGraph(attributePaths = {"movie", "screen.cinema"})
    Optional<Showtime> findByShowtimeId(Long showtimeId);
}