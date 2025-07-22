package com.cinemoa.api;

import com.cinemoa.dto.ReviewDto;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Movie;
import com.cinemoa.entity.Review;
import com.cinemoa.repository.MemberRepository;
import com.cinemoa.repository.MovieRepository;
import com.cinemoa.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;
    private final MemberRepository memberRepository;
    private final MovieRepository movieRepository;

    // 영화별 리뷰 목록 조회
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByMovieId(@PathVariable Long movieId, @RequestParam(required = false) String currentUserId) {
        List<ReviewDto> reviews = reviewService.getReviewsByMovieId(movieId, currentUserId);
        return ResponseEntity.ok(reviews);
    }

    // 영화별 긍정 평가 비율 조회
    @GetMapping("/movie/{movieId}/positive-percentage")
    public ResponseEntity<Integer> getPositivePercentage(@PathVariable Long movieId) {
        int percentage = reviewService.getPositivePercentage(movieId);
        return ResponseEntity.ok(percentage);
    }

    // 리뷰 저장
    @PostMapping
    public ResponseEntity<ReviewDto> saveReview(@RequestBody ReviewDto reviewDto) {
        try {
            // 1. ReviewDto에서 userId와 movieId를 사용하여 Member 및 Movie 엔티티 조회
            Member member = memberRepository.findByMemberId(reviewDto.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + reviewDto.getUserId()));

            Movie movie = movieRepository.findById(reviewDto.getMovieId())
                    .orElseThrow(() -> new NoSuchElementException("영화를 찾을 수 없습니다: " + reviewDto.getMovieId()));

            // 2. ReviewDto의 정보와 조회한 Member, Movie 엔티티를 이용해 Review 엔티티 생성
            Review review = Review.builder()
                    .content(reviewDto.getContent())
                    .isPositive(reviewDto.getIsPositive())
                    .user(member) // 조회한 Member 엔티티 설정
                    .movie(movie) // 조회한 Movie 엔티티 설정
                    .build();

            // 3. Review 엔티티를 Service에 전달하여 저장
            ReviewDto savedReview = reviewService.saveReview(review); // 이제 saveReview는 Review 엔티티를 받습니다.
            return ResponseEntity.status(HttpStatus.CREATED).body(savedReview); // 성공 시 201 Created 응답
        } catch (NoSuchElementException e) {
            // 사용자를 찾을 수 없거나 영화를 찾을 수 없을 때 404 Not Found 응답
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            // 그 외 예외 발생 시 500 Internal Server Error 응답
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, @RequestParam String currentUserId) {
        try {
            // 리뷰 조회
            ReviewDto reviewDto = reviewService.getReviewById(reviewId)
                    .orElseThrow(() -> new NoSuchElementException("리뷰를 찾을 수 없습니다."));

            // 리뷰 작성자 확인
            if (!reviewDto.getUserId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰만 삭제할 수 있습니다.");
            }

            // 리뷰 삭제
            reviewService.deleteReview(reviewId);
            return ResponseEntity.noContent().build(); // 삭제 후 No Content 응답
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
