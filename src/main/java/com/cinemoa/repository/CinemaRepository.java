package com.cinemoa.repository;

import com.cinemoa.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    // 지역으로 극장 목록 조회
    List<Cinema> findByRegion(String region);

    List<Cinema> findAllByOrderByCinemaIdAsc();

    // ID로 극장 찾기 (Optional로 반환)
    Optional<Cinema> findByCinemaId(Long cinemaId);
}
