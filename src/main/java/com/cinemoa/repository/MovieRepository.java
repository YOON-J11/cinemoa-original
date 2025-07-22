package com.cinemoa.repository;

import com.cinemoa.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // 상영 상태별 영화 조회
    List<Movie> findByScreeningStatus(Movie.ScreeningStatus status);

    // 페이징
    Page<Movie> findByScreeningStatus(Movie.ScreeningStatus status, Pageable pageable);

    // 키워드로 영화 검색 (제목, 감독, 배우 중 하나라도 포함)
    Page<Movie> findByTitleContainingOrDirectorContainingOrActorsContaining(
            String titleKeyword, String directorKeyword, String actorsKeyword, Pageable pageable);

    // 키워드와 상영 상태로 영화 검색 (제목, 감독, 배우 중 하나라도 포함하고 특정 상영 상태인 경우)
    @Query("""
        SELECT m FROM Movie m 
        WHERE (m.title LIKE %:keyword% OR m.director LIKE %:keyword% OR m.actors LIKE %:keyword%) 
        AND m.screeningStatus = :status
    """)
    Page<Movie> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                         @Param("status") Movie.ScreeningStatus status,
                                         Pageable pageable);
    // 좋아요 수 증가/감소용
    @Modifying
    @Query("UPDATE Movie m SET m.likesCount = m.likesCount + 1 WHERE m.id = :movieId")
    void incrementLikes(@Param("movieId") Long movieId);

    @Modifying
    @Query("UPDATE Movie m SET m.likesCount = m.likesCount - 1 WHERE m.id = :movieId AND m.likesCount > 0")
    void decrementLikes(@Param("movieId") Long movieId);

}