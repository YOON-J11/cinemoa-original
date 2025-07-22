package com.cinemoa.service;

import com.cinemoa.dto.ReviewDto;
import com.cinemoa.entity.Review;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ReviewService {
    // 리뷰 저장 (Review 엔티티를 받고, ReviewDto를 반환)
    ReviewDto saveReview(Review review);

    // 영화별 리뷰 목록 조회
    List<ReviewDto> getReviewsByMovieId(Long movieId, String currentUserId);

    // 리뷰 ID로 리뷰 조회
    Optional<ReviewDto> getReviewById(Long reviewId);

    // 리뷰 삭제
    void deleteReview(Long reviewId);

    // 리뷰 수정
    public void updateReview(ReviewDto dto, String currentUserId);

    // 영화별 긍정 리뷰 비율 조회
    int getPositivePercentage(Long movieId);

    Optional<ReviewDto> getReviewByUserAndMovie(String memberId, Long movieId);


}
