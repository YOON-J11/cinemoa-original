package com.cinemoa.repository;

import com.cinemoa.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findTop5ByMember_MemberIdOrderByReservationTimeDesc(String memberId);
    List<Reservation> findByShowtime_ShowtimeId(Long showtimeId);

}
