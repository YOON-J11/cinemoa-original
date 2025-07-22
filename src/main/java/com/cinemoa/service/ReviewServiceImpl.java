package com.cinemoa.service;

import com.cinemoa.dto.ReviewDto;
import com.cinemoa.entity.Review;
import com.cinemoa.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ReviewDto saveReview(Review review) {
        Review savedReview = reviewRepository.save(review);
        return convertToDto(savedReview, null);  // 기본적으로 currentUserId는 null로 처리
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByMovieId(Long movieId, String currentUserId) {
        List<Review> reviews = reviewRepository.findByMovie_MovieIdOrderByCreatedAtDesc(movieId);
        return reviews.stream()
                .map(review -> convertToDto(review, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewDto> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(review -> convertToDto(review, null)); // 현재 로그인된 유저 정보가 없으면 null로 처리
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    @Override
    @Transactional
    public void updateReview(ReviewDto dto, String currentUserId) {
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new NoSuchElementException("리뷰 없음"));

        if (!review.getUser().getMemberId().equals(currentUserId)) {
            throw new IllegalStateException("본인만 수정 가능");
        }

        review.setContent(dto.getContent());
        review.setIsPositive(dto.getIsPositive());
    }

    @Override
    @Transactional(readOnly = true)
    public int getPositivePercentage(Long movieId) {
        long totalReviews = reviewRepository.countByMovie_MovieId(movieId);
        if (totalReviews == 0) {
            return 0;
        }
        long positiveReviews = reviewRepository.countPositiveReviewsByMovieId(movieId);
        return (int) ((positiveReviews * 100) / totalReviews);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewDto> getReviewByUserAndMovie(String memberId, Long movieId) {
        return reviewRepository.findByMovie_MovieIdAndUser_MemberId(movieId, memberId)
                .map(review -> convertToDto(review, memberId));
    }


    // Entity -> DTO 변환 (currentUserId를 전달하여 삭제 가능 여부 판단)
    private ReviewDto convertToDto(Review review, String currentUserId) {
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setReviewId(review.getReviewId());
        reviewDto.setContent(review.getContent());
        reviewDto.setIsPositive(review.getIsPositive());
        reviewDto.setCreatedAt(review.getCreatedAt());
        reviewDto.setUpdatedAt(review.getUpdatedAt());

        if (review.getMovie() != null) {
            reviewDto.setMovieId(review.getMovie().getMovieId());
        }

        if (review.getUser() != null) {
            reviewDto.setUserId(review.getUser().getMemberId());
            reviewDto.setUserName(review.getUser().getNickname());
        }

        // 삭제 가능 여부 확인
        if (currentUserId != null && currentUserId.equals(review.getUser().getMemberId())) {
            reviewDto.setDeletable(true);
        } else {
            reviewDto.setDeletable(false);
        }

        return reviewDto;
    }
}
