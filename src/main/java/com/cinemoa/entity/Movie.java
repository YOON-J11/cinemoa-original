package com.cinemoa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "reservation_rate")
    private BigDecimal reservationRate;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "director")
    private String director;

    @Column(name = "actors")
    private String actors;

    @Column(name = "genre")
    private String genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "likes_count")
    private Integer likesCount;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @Column(name = "sub_image_urls") // 서브 이미지 URL 컬럼 추가
    private String subImageUrls;

    @Column(name = "detail_image_urls")
    private String detailImageUrls;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "audience_count")
    private BigInteger audienceCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 관람등급
    @Column(name = "age_rating")
    private String ageRating;

    // 러닝타임 (분 단위)
    @Column(name = "running_time")
    private Integer runningTime;

    // 상영 상태를 위한 Enum 타입 필드 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "screening_status")
    private ScreeningStatus screeningStatus;

    // 상영 상태를 위한 Enum 타입 정의
    public enum ScreeningStatus {
        NOW_SHOWING("개봉중"),
        COMING_SOON("상영예정"),
        NOT_SHOWING("상영안함");

        private final String displayName;

        ScreeningStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}