package com.cinemoa.api;

import com.cinemoa.dto.MovieDto;
import com.cinemoa.entity.Member;
import com.cinemoa.service.MovieService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/movies")
public class MovieApiController {

    private final MovieService movieService;

    @Autowired
    public MovieApiController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<MovieDto>> getAllMovies() {
        List<MovieDto> movies = movieService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<MovieDto>> getMoviesPaginated(@PageableDefault(size = 10) Pageable pageable, HttpSession session) {
        // 세션에서 로그인된 사용자 정보 가져오기
        Object sessionAttribute = session.getAttribute("loginMember");
        String currentMemberId = null;

        if (sessionAttribute instanceof Member) {
            Member loginMember = (Member) sessionAttribute;
            currentMemberId = loginMember.getMemberId();
        }

        Page<MovieDto> movies = movieService.getMoviesPaginated(pageable, currentMemberId);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDto> getMovieById(@PathVariable("id") Long id, HttpSession session) {

        // 세션에서 로그인된 사용자 정보 가져오기
        Object sessionAttribute = session.getAttribute("loginMember");
        String currentMemberId = null;

        if (sessionAttribute instanceof Member) {
            Member loginMember = (Member) sessionAttribute;
            currentMemberId = loginMember.getMemberId();
        }

        return movieService.getMovieById(id, currentMemberId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MovieDto> createMovie(@RequestBody MovieDto movieDto) {
        MovieDto createdMovie = movieService.saveMovie(movieDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMovie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieDto> updateMovie(@PathVariable("id") Long id, @RequestBody MovieDto movieDto) {
        MovieDto updatedMovie = movieService.updateMovie(id, movieDto);
        return updatedMovie != null ?
                ResponseEntity.ok(updatedMovie) :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{movieId}/like")
    public ResponseEntity<Map<String, Object>> toggleMovieLike(@PathVariable("movieId") Long movieId, HttpSession session) {
        // 세션에서 로그인된 사용자 정보 가져오기
        Object sessionAttribute = session.getAttribute("loginMember");
        String currentMemberId = null;

        if (sessionAttribute instanceof Member) {
            Member loginMember = (Member) sessionAttribute;
            currentMemberId = loginMember.getMemberId();
        } else {
            // 테스트용 임시 ID (실제 서비스에서는 로그인 필요 메시지를 반환하는 것이 좋음)
            currentMemberId = "test2";
        }

        try {
            // toggleLike 메서드가 Map을 반환하도록 수정된 경우
            Map<String, Object> result = movieService.toggleLike(movieId, currentMemberId);
            return ResponseEntity.ok(result);

        } catch (NoSuchElementException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}