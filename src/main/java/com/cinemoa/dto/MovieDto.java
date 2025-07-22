package com.cinemoa.dto;

import com.cinemoa.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private Long movieId;
    private String title;
    private String content;
    private BigDecimal reservationRate;
    private BigDecimal rating;
    private String director;
    private String actors;
    private String genre;
    private LocalDate releaseDate;
    private Integer likesCount;
    private Integer reviewCount;
    private String mainImageUrl;
    private String subImageUrls; // 서브 이미지 URL 필드 추가
    private String detailImageUrls;
    private String videoUrl;
    private BigInteger audienceCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Movie.ScreeningStatus screeningStatus; // 상영 상태 추가

    // 관람등급과 러닝타임 필드 추가
    private String ageRating;  // 전체관람가, 12세, 15세, 청소년관람불가 등
    private Integer runningTime;  // 분 단위로 저장 (예: 120분)


    public boolean isNowShowing() {
        return this.screeningStatus != null && this.screeningStatus == Movie.ScreeningStatus.NOW_SHOWING;
    }

    public boolean isComingSoon() {
        return this.screeningStatus != null && this.screeningStatus == Movie.ScreeningStatus.COMING_SOON;
    }

    public boolean isNotShowing() {
        return this.screeningStatus != null && this.screeningStatus == Movie.ScreeningStatus.NOT_SHOWING;
    }

    public boolean isAll() {
        return "ALL".equals(this.ageRating);
    }

    public boolean is12() {
        return "12".equals(this.ageRating);
    }

    public boolean is15() {
        return "15".equals(this.ageRating);
    }

    public boolean is19() {
        return "19".equals(this.ageRating);
    }

    private boolean likedByCurrentUser;
    private Integer rank; // 예매율 기반 순위

}

// 목록 조회용 간소화된 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MovieListDto {
    private Long movieId;
    private String title;
    private BigDecimal reservationRate;
    private BigDecimal rating;
    private String director;
    private String genre;
    private LocalDate releaseDate;
    private Integer likesCount;
    private Integer reviewCount;
    private String mainImageUrl;
    private BigInteger audienceCount;
    private String ageRating;  // 관람등급 추가
    private Integer runningTime;  // 러닝타임 추가
}