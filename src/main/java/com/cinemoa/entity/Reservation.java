package com.cinemoa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // JPA 엔티티 클래스임을 명시 (DB 테이블과 매핑됨)
@Table(name = "reservation") // 매핑할 테이블 이름을 명시적으로 지정 (member 테이블과 연결됨)
@Getter
@Setter
@NoArgsConstructor //파라미터 없는 기본 생성자 생성
@AllArgsConstructor //모든 필드를 파라미터로 받는 생성자 생성
@Builder //객체 생성시 빌더 패턴 사용가능
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;                    // ★ Integer -> Long

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", insertable = false, updatable = false)
    private Screen screen;                    // FK 객체 접근용 (그대로)

    private String seatInfo;

    private LocalDateTime reservationTime;

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Payment payment;

    private String paymentMethod;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

}
