package com.cinemoa.repository;

import com.cinemoa.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    //showtimeId 기준으로 예약된 좌석 수 조회
    int countByShowtime_ShowtimeId(Long showtimeId);

    //특정 예매(reservationId)에 해당하는 좌석(ReservationSeat) 목록을 조회
    List<ReservationSeat> findByReservation_ReservationId(Long reservationId);

    boolean existsByShowtime_ShowtimeIdAndSeat_SeatId(Long showtimeId, Long seatId);
}
