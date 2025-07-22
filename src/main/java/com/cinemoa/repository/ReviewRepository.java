package com.cinemoa.repository;

import com.cinemoa.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {


    // 1. 영화별 리뷰 목록
    List<Review> findByMovie_MovieIdOrderByCreatedAtDesc(Long movieId);

    // 2. 영화별 긍정/부정 리뷰 수
    @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.movieId = :movieId AND r.isPositive = true")
    long countPositiveReviewsByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.movieId = :movieId AND r.isPositive = false")
    long countNegativeReviewsByMovieId(@Param("movieId") Long movieId);

    // 3. 전체 리뷰 수 (메서드 명 정확히 명시)
    long countByMovie_MovieId(Long movieId);

    // 4. 리뷰 존재 여부
    boolean existsByMovie_MovieIdAndUser_MemberId(Long movieId, String memberId);

    // 5. 영화ID + 유저ID 로 리뷰 단건 조회
    Optional<Review> findByMovie_MovieIdAndUser_MemberId(Long movieId, String memberId);
}