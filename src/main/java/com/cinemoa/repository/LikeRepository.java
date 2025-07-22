package com.cinemoa.repository;

import com.cinemoa.entity.Like;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByMovieAndUser(Movie movie, Member user);
    Optional<Like> findByMovieAndUser(Movie movie, Member user);
}
