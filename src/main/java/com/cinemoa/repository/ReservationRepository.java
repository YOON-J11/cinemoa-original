package com.cinemoa.repository;

import com.cinemoa.entity.Movie;
import com.cinemoa.entity.Payment;
import com.cinemoa.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //특정 회원(memberId)의 예매 내역 중 가장 최근 5건을 가져옴.
    List<Reservation> findTop5ByMember_MemberIdOrderByReservationTimeDesc(String memberId);

    //특정 상영시간(showtimeId)에 해당하는 예매 목록을 모두 가져옴.
    List<Reservation> findByShowtime_ShowtimeId(Long showtimeId);

    //특정 회원(memberId)의 모든 예매 내역을 가져옴.
    List<Reservation> findByMember_MemberId(String memberId);

    List<Reservation> findByMember_MemberIdAndStatus(String memberId, String status);

    boolean existsByMovie_MovieIdAndMember_MemberId(Long movieId, String memberId);

    // 특정 영화의 예매 수
    long countByMovie(Movie movie);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.movie.movieId = :movieId AND r.payment.status = :status")
    long countConfirmedAudience(@Param("movieId") Long movieId, @Param("status") Payment.PaymentStatus status);

    @Query("SELECT COUNT(r) FROM Reservation r")
    long countAll();

}
