package com.cinemoa.service;

import com.cinemoa.dto.MovieDto;
import com.cinemoa.entity.Like;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Movie;
import com.cinemoa.entity.Payment;
import com.cinemoa.repository.LikeRepository;
import com.cinemoa.repository.MemberRepository;
import com.cinemoa.repository.MovieRepository;
import com.cinemoa.repository.ReservationRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final LikeRepository likeRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    @Autowired
    public MovieServiceImpl(MovieRepository movieRepository, LikeRepository likeRepository, MemberRepository memberRepository, ReservationRepository reservationRepository) {
        this.movieRepository = movieRepository;
        this.likeRepository = likeRepository;
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<MovieDto> getMoviesPaginated(Pageable pageable, String memberId) {
        return movieRepository.findAll(pageable)
                .map(movie -> convertToDtoWithLikeStatus(movie, memberId));
    }

    @Override
    public Optional<MovieDto> getMovieById(Long id, String memberId) {
        Optional<Movie> movieOpt = movieRepository.findById(id);
        return movieOpt.map(movie -> {
            MovieDto dto = convertToDtoWithLikeStatus(movie, memberId);

            // 예매율, 누적 관객 수 직접 MovieDto 필드에 넣거나 컨트롤러에서 따로 조회할 수도 있음
            BigDecimal reservationRate = getReservationRate(movie);
            long audienceCount = countAudienceByMovie(id);

            dto.setReservationRate(reservationRate);
            dto.setAudienceCount(BigInteger.valueOf(audienceCount));

            return dto;
        });
    }

    @Override
    @Transactional
    public MovieDto saveMovie(MovieDto movieDto) {
        Movie movie = convertToEntity(movieDto);
        Movie savedMovie = movieRepository.save(movie);
        return convertToDto(savedMovie);
    }

    @Override
    @Transactional
    public MovieDto updateMovie(Long id, MovieDto movieDto) {
        Optional<Movie> existingMovieOpt = movieRepository.findById(id);

        if (existingMovieOpt.isPresent()) {
            Movie existingMovie = existingMovieOpt.get();

            Movie movie = convertToEntity(movieDto);
            movie.setMovieId(id);
            movie.setLikesCount(existingMovie.getLikesCount());
            movie.setCreatedAt(existingMovie.getCreatedAt());

            Movie updatedMovie = movieRepository.save(movie);
            return convertToDto(updatedMovie);
        }

        return null; // 또는 예외 처리
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieDto> getMoviesByScreeningStatus(Movie.ScreeningStatus status, Pageable pageable, String memberId) {
        Page<Movie> moviePage = movieRepository.findByScreeningStatus(status, pageable);
        return moviePage.map(movie -> convertToDtoWithLikeStatus(movie, memberId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieDto> searchMoviesByKeyword(String keyword, Pageable pageable, String memberId) {
        // 제목, 감독, 배우만으로 키워드 검색
        Page<Movie> moviePage = movieRepository.findByTitleContainingOrDirectorContainingOrActorsContaining(
                keyword, keyword, keyword, pageable);
        return moviePage.map(movie -> convertToDtoWithLikeStatus(movie, memberId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieDto> searchMoviesByKeywordAndStatus(String keyword, Movie.ScreeningStatus status, Pageable pageable, String memberId) {
        Page<Movie> moviePage = movieRepository.searchByKeywordAndStatus(keyword, status, pageable);
        return moviePage.map(movie -> convertToDtoWithLikeStatus(movie, memberId));
    }


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> toggleLike(Long movieId, String memberId) {
        try {
            return doToggleLike(movieId, memberId);
        } catch (Exception e) {
            if (e.getMessage() != null &&
                    (e.getMessage().contains("Deadlock") ||
                            e.getMessage().contains("Row was updated or deleted by another transaction"))) {
                // 오류 발생 시 잠시 대기 후 재시도
                try {
                    Thread.sleep(100); // 100ms 대기
                    return doToggleLike(movieId, memberId);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("트랜잭션 재시도 중 인터럽트 발생", ie);
                }
            }
            throw e;
        }
    }

    // 실제 좋아요 토글 로직을 별도 메서드로 분리
    private Map<String, Object> doToggleLike(Long movieId, String memberId) {
        // 기존 toggleLike 메서드의 내용
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NoSuchElementException("ID가 " + movieId + "인 영화를 찾을 수 없습니다."));
        Member user = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new NoSuchElementException("ID가 " + memberId + "인 회원을 찾을 수 없습니다."));

        // 좋아요 조회 전에 영화와 사용자 정보를 다시 한번 확인
        if (movie == null || user == null) {
            throw new NoSuchElementException("영화 또는 사용자 정보가 유효하지 않습니다.");
        }

        Optional<Like> existingLike = likeRepository.findByMovieAndUser(movie, user);

        Map<String, Object> result = new HashMap<>();
        if (existingLike.isPresent()) {
            // 좋아요 취소 로직
            Like like = existingLike.get();
            likeRepository.delete(like);

            // 영화 좋아요 수 업데이트 전에 최신 정보 다시 조회
            movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new NoSuchElementException("영화를 찾을 수 없습니다."));

            movie.setLikesCount(Math.max(0, movie.getLikesCount() - 1)); // 음수 방지
            movieRepository.save(movie);

            result.put("liked", false);
            result.put("likesCount", movie.getLikesCount());
            result.put("message", "좋아요가 성공적으로 취소되었습니다.");
        } else {
            // 좋아요 추가 로직
            Like newLike = Like.builder()
                    .movie(movie)
                    .user(user)
                    .build();
            likeRepository.save(newLike);

            // 영화 좋아요 수 업데이트 전에 최신 정보 다시 조회
            movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new NoSuchElementException("영화를 찾을 수 없습니다."));

            movie.setLikesCount(movie.getLikesCount() + 1);
            movieRepository.save(movie);

            result.put("liked", true);
            result.put("likesCount", movie.getLikesCount());
            result.put("message", "좋아요가 성공적으로 추가되었습니다.");
        }
        return result;
    }

    // 사용자가 영화에 좋아요를 눌렀는지 확인
    @Override
    @Transactional(readOnly = true)
    public boolean isMovieLikedByMember(Long movieId, String memberId) {

        if (memberId == null || memberId.isEmpty()) { // 비로그인 사용자 처리
            System.out.println("memberId가 null이거나 비어 있습니다.");
            return false;
        }
        Optional<Movie> movieOpt = movieRepository.findById(movieId);
        Optional<Member> memberOpt = memberRepository.findByMemberId(memberId);

        System.out.println("영화 찾음: " + movieOpt.isPresent());
        System.out.println("회원 찾음: " + memberOpt.isPresent());

        if (movieOpt.isPresent() && memberOpt.isPresent()) {

            boolean exists = likeRepository.existsByMovieAndUser(movieOpt.get(), memberOpt.get());
            System.out.println("좋아요 존재 여부: " + exists);

            return likeRepository.existsByMovieAndUser(movieOpt.get(), memberOpt.get());
        }
        return false;
    }

    @Override
    public BigDecimal getReservationRate(Movie movie) {
        // 예: 특정 영화 예매율 = (영화 예매 수) / (전체 예매 수)
        long movieReservations = countReservationsByMovie(movie);
        long allReservations = countAllReservations();

        if (allReservations == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(movieReservations)
                .divide(BigDecimal.valueOf(allReservations), 4, RoundingMode.HALF_UP);
    }

    @Override
    public long countReservationsByMovie(Movie movie) {
        // 예매 DB에서 영화별 예약 수를 가져오는 로직 작성
        return reservationRepository.countByMovie(movie);
    }

    @Override
    public long countAllReservations() {
        // 전체 예매 수 조회
        return reservationRepository.countAll();
    }

    @Override
    public long countAudienceByMovie(Long movieId) {
        return reservationRepository.countConfirmedAudience(movieId, Payment.PaymentStatus.PAID);

    }

    @Override
    public List<MovieDto> getMoviesWithStats(String memberId) {
        List<Movie> movies = movieRepository.findAll();

        // 1. DTO로 변환 및 예매율/누적관객수 포함
        List<MovieDto> movieDtos = movies.stream().map(movie -> {
            MovieDto dto = convertToDtoWithLikeStatus(movie, memberId);
            dto.setReservationRate(getReservationRate(movie)); // 예매율 계산
            dto.setAudienceCount(BigInteger.valueOf(countAudienceByMovie(movie.getMovieId()))); // 누적관객수 계산
            return dto;
        }).collect(Collectors.toList());

        // 2. 예매율 기준으로 정렬하여 순위 부여
        movieDtos.sort(Comparator.comparing(MovieDto::getReservationRate).reversed());

        int rank = 1;
        for (MovieDto dto : movieDtos) {
            dto.setRank(rank++);
            System.out.println("Movie: " + dto.getTitle() + ", Rank: " + dto.getRank());
        }

        return movieDtos;
    }

    // 순위로 출력
    public Page<MovieDto> getMoviesByRank(Pageable pageable, String memberId, Movie.ScreeningStatus status) {
        // 1. 전체 영화 조회 + DTO 변환 + 예매율, 관객수 계산 + rank 부여 (상태 필터 포함)
        List<MovieDto> allMoviesWithStats = getMoviesWithStats(memberId).stream()
                .filter(movieDto -> status == null || movieDto.getScreeningStatus() == status)
                .collect(Collectors.toList());

        // 2. 페이징 처리용 start, end index 계산
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();

        int total = allMoviesWithStats.size();

        if (offset > total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        int toIndex = Math.min(offset + pageSize, total);

        // 3. 해당 페이지에 맞는 sublist 반환
        List<MovieDto> pageContent = allMoviesWithStats.subList(offset, toIndex);

        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public Page<MovieDto> getMoviesByRank(Pageable pageable, String memberId) {
        return getMoviesByRank(pageable, memberId, null);
    }


    // Entity -> DTO 변환, 좋아요 상태를 포함시키는 헬퍼 메서드
    private MovieDto convertToDtoWithLikeStatus(Movie movie, String memberId) {
        MovieDto movieDto = new MovieDto();
        BeanUtils.copyProperties(movie, movieDto);
        movieDto.setLikedByCurrentUser(isMovieLikedByMember(movie.getMovieId(), memberId));
        return movieDto;
    }

    // Entity -> DTO 변환
    private MovieDto convertToDto(Movie movie) {
        MovieDto movieDto = new MovieDto();
        BeanUtils.copyProperties(movie, movieDto);

        // 예매율 계산 추가
        BigDecimal reservationRate = getReservationRate(movie);
        movieDto.setReservationRate(reservationRate);

        return movieDto;
    }

    // DTO -> Entity 변환
    private Movie convertToEntity(MovieDto movieDto) {
        Movie movie = new Movie();
        BeanUtils.copyProperties(movieDto, movie);
        return movie;
    }


}