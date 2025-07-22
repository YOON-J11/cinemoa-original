package com.cinemoa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long reviewId;
    private Long movieId;  // DTO 변환 시 Review.movie.movieId
    private String userId;  // Review.user.memberId
    private String content;
    private Boolean isPositive; // true: 좋았어요, false: 별로였어요
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String userName;  // Review.user.nickname

    private boolean deletable;  // 삭제 권한 여부
}