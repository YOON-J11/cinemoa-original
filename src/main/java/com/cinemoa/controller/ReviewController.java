package com.cinemoa.controller;

import com.cinemoa.dto.ReviewDto;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Movie;
import com.cinemoa.entity.Review;
import com.cinemoa.repository.MemberRepository;
import com.cinemoa.repository.MovieRepository;
import com.cinemoa.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException; // 예외 처리 임포트

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final MemberRepository memberRepository;
    private final MovieRepository movieRepository;


    // 리뷰 작성 폼 표시 (풀 페이지 반환)
    // @GetMapping("/new")
    // public String showNewReviewForm(@RequestParam("movieId") Long movieId, Model model) {
    //     model.addAttribute("movieId", movieId);
    //     model.addAttribute("review", new ReviewDto());
    //     return "reviews/new"; // 풀 페이지 템플릿 반환
    // }

    //리뷰 작성 폼 Fragment 반환 (AJAX 모달용)
    @GetMapping("/new-fragment")
    public String showNewReviewFormFragment(@RequestParam("movieId") Long movieId, Model model) {
        model.addAttribute("movieId", movieId);
        model.addAttribute("review", new ReviewDto());
        return "reviews/new";
    }

    @GetMapping("/edit-fragment")
    public String showEditReviewForm(@RequestParam Long reviewId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/member/login";
        }

        ReviewDto reviewDto = reviewService.getReviewById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("리뷰를 찾을 수 없습니다."));

        if (!reviewDto.getUserId().equals(loginMember.getMemberId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인만 수정할 수 있습니다.");
            return "redirect:/movies/" + reviewDto.getMovieId();
        }

        model.addAttribute("review", reviewDto);
        return "reviews/edit"; // 수정 폼 템플릿 (모달 내용)
    }

    // 리뷰 저장
    @PostMapping("/new")
    public String saveReview(@ModelAttribute ReviewDto reviewDto,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            // 세션에서 로그인한 Member 객체 가져오기
            Member loginMember = (Member) session.getAttribute("loginMember");

            if (loginMember == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
                return "redirect:/member/login";
            }

            // Movie 조회
            Movie movie = movieRepository.findById(reviewDto.getMovieId())
                    .orElseThrow(() -> new NoSuchElementException("영화를 찾을 수 없습니다."));

            // Review 저장
            Review review = Review.builder()
                    .content(reviewDto.getContent())
                    .isPositive(reviewDto.getIsPositive())
                    .user(loginMember)  // 바로 세션에서 가져온 Member 사용
                    .movie(movie)
                    .build();

            reviewService.saveReview(review);
            redirectAttributes.addFlashAttribute("message", "리뷰가 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 등록 중 오류: " + e.getMessage());
        }

        return "redirect:/movies/" + reviewDto.getMovieId();
    }

    // 리뷰 삭제
    @GetMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId, @RequestParam Long movieId, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Member loginMember = (Member) session.getAttribute("loginMember");

            if (loginMember == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
                return "redirect:/member/login";
            }

            ReviewDto reviewDto = reviewService.getReviewById(reviewId)
                    .orElseThrow(() -> new NoSuchElementException("리뷰를 찾을 수 없습니다."));

            // 리뷰 작성자 확인
            if (!reviewDto.getUserId().equals(loginMember.getMemberId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "본인이 작성한 리뷰만 삭제할 수 있습니다.");
                return "redirect:/movies/" + movieId;
            }

            reviewService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("message", "리뷰가 삭제되었습니다.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 삭제 중 오류: " + e.getMessage());
        }

        return "redirect:/movies/" + movieId;
    }

    // 리뷰 수정
    @PostMapping("/edit")
    public String updateReview(@ModelAttribute ReviewDto reviewDto, HttpSession session, RedirectAttributes redirectAttributes) {
        Member loginMember = (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/member/login";
        }

        try {
            reviewService.updateReview(reviewDto, loginMember.getMemberId());
            redirectAttributes.addFlashAttribute("message", "리뷰가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 수정 실패: " + e.getMessage());
        }

        return "redirect:/movies/" + reviewDto.getMovieId();
    }

    // 리뷰 유무 확인
    @GetMapping("/check-review")
    @ResponseBody
    public ResponseEntity<ReviewDto> checkUserReview(@RequestParam Long movieId, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return ResponseEntity.ok().body(null);

        return reviewService.getReviewByUserAndMovie(loginMember.getMemberId(), movieId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().body(null));
    }


}