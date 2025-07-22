package com.cinemoa.controller;

import com.cinemoa.dto.MovieDto;
import com.cinemoa.dto.ReviewDto;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Movie;
import com.cinemoa.service.MovieService;
import com.cinemoa.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Controller
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;
    private final ReviewService reviewService;

    @Autowired
    public MovieController(MovieService movieService, ReviewService reviewService) {
        this.movieService = movieService;
        this.reviewService = reviewService;
    }


    @GetMapping("")
    public String listMovies(@RequestParam(required = false) String status,
                             Model model,
                             @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, HttpSession session) {

        //Page<MovieDto> movies;

        // 세션에서 로그인된 사용자 정보 가져오기 (다른 메서드와 일치시킴)
        Object sessionAttribute = session.getAttribute("loginMember");
        String currentMemberId = null;

        if (sessionAttribute instanceof Member) {
            Member loginMember = (Member) sessionAttribute;
            currentMemberId = loginMember.getMemberId();
            System.out.println("초기 로드 - 로그인된 사용자 ID: " + currentMemberId);
        } else {
            currentMemberId = "test2"; // 임시 ID
            System.out.println("초기 로드 - 임시 ID 사용: " + currentMemberId);
        }

        Movie.ScreeningStatus screeningStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                screeningStatus = Movie.ScreeningStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                screeningStatus = null;  // 잘못된 status 무시
            }
        }

        Page<MovieDto> movies = movieService.getMoviesByRank(pageable, currentMemberId, screeningStatus);

        for (MovieDto movieDto : movies.getContent()) {
            handleNullValues(movieDto);
        }

        model.addAttribute("movies", movies);

        // 다음 페이지 번호 계산 (movies.number는 현재 페이지 번호, 0부터 시작)
        int nextPageNumber = movies.getNumber() + 1;
        model.addAttribute("nextPageNumber", nextPageNumber);
        for (MovieDto movieDto : movies.getContent()) {
            handleNullValues(movieDto);
        }

        model.addAttribute("currentStatus", status); // 현재 선택된 상태를 모델에 추가

        model.addAttribute("nextPageNumber", movies.getNumber() + 1);
        model.addAttribute("hasNext", movies.hasNext());
        model.addAttribute("hasPrevious", movies.hasPrevious());
        model.addAttribute("prevPage", Math.max(0, movies.getNumber() - 1));
        model.addAttribute("nextPage", Math.min(movies.getTotalPages() - 1, movies.getNumber() + 1));

        // 페이지 번호 목록 생성
        int totalPages = movies.getTotalPages();
        if (totalPages > 0) {
            List<PageItem> pageNumbers = new ArrayList<>();
            int start = Math.max(0, movies.getNumber() - 2);
            int end = Math.min(totalPages - 1, movies.getNumber() + 2);

            for (int i = start; i <= end; i++) {
                pageNumbers.add(new PageItem(i, movies.getNumber() == i));
            }
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "movies/list";
    }

    // "더보기" 버튼 클릭 시 호출될 AJAX 전용 엔드포인트 (HTML 조각 반환)
    @GetMapping("/loadMore")
    public String loadMoreMovies(@RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model, HttpSession session) {

        // 세션에서 로그인된 사용자 정보 가져오기
        Member loginMember = (Member) session.getAttribute("loginMember");
        String currentMemberId = loginMember != null ? loginMember.getMemberId() : null;

        Pageable pageable = PageRequest.of(page, 12, Sort.by("rank").descending());

        Movie.ScreeningStatus screeningStatus = null;

        if (status != null && !status.isEmpty()) {
            try {
                screeningStatus = Movie.ScreeningStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                screeningStatus = null; // 유효하지 않은 값일 경우 null로 처리
            }
        }

        Page<MovieDto> movies;

        if (screeningStatus != null) {
            // 상영 상태 필터가 있으면, 필터 포함된 rank 순 조회
            movies = movieService.getMoviesByRank(pageable, currentMemberId, screeningStatus);
        } else {
            // 필터 없으면 그냥 rank 순 조회
            movies = movieService.getMoviesByRank(pageable, currentMemberId, null);
        }

        model.addAttribute("movies", movies.getContent());
        model.addAttribute("hasNext", movies.hasNext());

        return "movies/movie-items-partial";
    }

    // 페이지 항목을 위한 내부 클래스
    @Data
    @AllArgsConstructor
    static class PageItem {
        private int number;
        private boolean active;
    }

    @GetMapping("/{id}")
    public String viewMovie(@PathVariable("id") Long id, Model model, HttpSession session) {

        // 세션에서 로그인된 사용자 정보 가져오기
        Member loginMember = (Member) session.getAttribute("loginMember");
        String currentMemberId = loginMember != null ? loginMember.getMemberId() : null;

        // currentMemberId 전달
        Optional<MovieDto> movieDtoOptional = movieService.getMovieById(id, currentMemberId);

        if (movieDtoOptional.isPresent()) {
            MovieDto movieDto = movieDtoOptional.get();

            // null 값 처리를 함수로 호출
            handleNullValues(movieDto);

            // videoUrl 처리
            if (movieDto.getVideoUrl() != null && !movieDto.getVideoUrl().isEmpty()) {
                String videoUrl = movieDto.getVideoUrl();
                // 이미 전체 URL인 경우 그대로 사용
                if (videoUrl.startsWith("https://www.youtube.com/embed/")) {
                    // 이미 올바른 형식이므로 그대로 사용
                }
                // youtube.com/watch?v= 형식인 경우 embed 형식으로 변환
                else if (videoUrl.contains("youtube.com/watch?v=")) {
                    String videoId = videoUrl.split("v=")[1];
                    // 추가 파라미터가 있는 경우 제거
                    int ampersandPosition = videoId.indexOf('&');
                    if (ampersandPosition != -1) {
                        videoId = videoId.substring(0, ampersandPosition);
                    }
                    movieDto.setVideoUrl("https://www.youtube.com/embed/" + videoId);
                }
                // ID만 있는 경우 embed URL로 변환
                else {
                    movieDto.setVideoUrl("https://www.youtube.com/embed/" + videoUrl);
                }
            }

            // 상세 이미지 URL 처리 (콤마로 구분된 문자열을 리스트로 변환)
            if (movieDto.getDetailImageUrls() != null && !movieDto.getDetailImageUrls().isEmpty()) {
                List<String> detailImageUrlList = Arrays.asList(movieDto.getDetailImageUrls().split(","));
                model.addAttribute("detailImageUrlList", detailImageUrlList);
            }

            // 서브 이미지 URL 처리 (콤마로 구분된 문자열을 리스트로 변환)
            if (movieDto.getSubImageUrls() != null && !movieDto.getSubImageUrls().isEmpty()) {
                List<String> subImageUrlList = Arrays.asList(movieDto.getSubImageUrls().split(","));
                model.addAttribute("subImageUrlList", subImageUrlList);
            }

            // 로그인된 사용자 정보 가져오기
            String currentUserId = loginMember != null ? loginMember.getMemberId() : null;
            model.addAttribute("loginMember", loginMember); // 뷰용..

            // 리뷰 목록 조회
            List<ReviewDto> reviews = reviewService.getReviewsByMovieId(id, currentUserId);
            model.addAttribute("reviews", reviews);

            // 긍정 평가 비율 계산
            int positivePercentage = reviewService.getPositivePercentage(id);
            model.addAttribute("positivePercentage", positivePercentage);

            // movieId 값을 모델에 추가
            model.addAttribute("movieId", id);

            // 영화 전체 리스트로 rank 포함 DTO 리스트 조회
            List<MovieDto> rankedMovies = movieService.getMoviesWithStats(currentMemberId);

            // 현재 영화의 rank 찾기
            int rank = rankedMovies.stream()
                    .filter(m -> m.getMovieId().equals(id))
                    .map(MovieDto::getRank)
                    .findFirst()
                    .orElse(0);

            movieDto.setRank(rank);

            model.addAttribute("movie", movieDto);

            return "movies/view";
        } else {
            return "redirect:/movies";
        }
    }

    @GetMapping("/new")
    public String showNewMovieForm(Model model) {
        MovieDto movieDto = new MovieDto();
        movieDto.setContent(""); // content 필드 초기화

        // 다른 필드들도 필요하다면 초기화
        model.addAttribute("movie", movieDto);
        return "movies/new";
    }

    @PostMapping("/new")
    public String saveNewMovie(@ModelAttribute("movie") MovieDto movieDto,
                               @RequestParam(value = "mainImageFile", required = false) MultipartFile mainImageFile,
                               @RequestParam(value = "subImageFile", required = false) MultipartFile subImageFile,
                               @RequestParam(value = "detailImageFiles", required = false) MultipartFile[] detailImageFiles,
                               RedirectAttributes redirectAttributes) {

        // content 필드 디버깅
        System.out.println("Content 필드 값: " + movieDto.getContent());

        // 빈 문자열이나 HTML 공백 태그만 있는 경우 null로 설정
        if (movieDto.getContent() != null &&
                (movieDto.getContent().isEmpty() ||
                        movieDto.getContent().equals("<p>&nbsp;</p>") ||
                        movieDto.getContent().equals("<p><br></p>"))) {
            movieDto.setContent(null);
        }

        // 1. 메인 이미지 처리
        if (mainImageFile != null && !mainImageFile.isEmpty()) {
            String savedFileName = saveFile(mainImageFile);
            movieDto.setMainImageUrl(savedFileName);
        } else {
            movieDto.setMainImageUrl(null); // 파일이 없으면 null
        }

        // 2. 서브 이미지 처리 (단일 파일로 변경)
        if (subImageFile != null && !subImageFile.isEmpty()) {
            String savedFileName = saveFile(subImageFile);
            movieDto.setSubImageUrls(savedFileName); // 단일 파일명 저장
        } else {
            movieDto.setSubImageUrls(null); // 파일이 없으면 null
        }

        // 3. 상세 이미지 처리
        if (detailImageFiles != null && detailImageFiles.length > 0) {
            List<String> savedDetailImagePaths = new ArrayList<>();
            for (MultipartFile file : detailImageFiles) {
                if (!file.isEmpty()) {
                    savedDetailImagePaths.add(saveFile(file));
                }
            }
            movieDto.setDetailImageUrls(String.join(",", savedDetailImagePaths));
        } else {
            movieDto.setDetailImageUrls(null); // 파일이 없으면 null
        }

        // null 값 처리 (반복되는 코드는 handleNullValues 메서드로 분리)
        handleNullValues(movieDto);

        MovieDto savedMovie = movieService.saveMovie(movieDto);
        redirectAttributes.addFlashAttribute("message", "영화가 성공적으로 등록되었습니다.");

        // 등록 완료 후 상세 화면으로 이동
        return "redirect:/movies/" + savedMovie.getMovieId();
    }

    // 영화 수정 폼
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<MovieDto> movieDtoOptional = movieService.getMovieById(id, null);

        if (movieDtoOptional.isPresent()) {
            MovieDto movieDto = movieDtoOptional.get();

            // null 값 처리 (뷰에서 오류 나지 않도록)
            handleNullValues(movieDto);

            // 상세 이미지 URL을 리스트로 변환하여 뷰에 전달
            if (movieDto.getDetailImageUrls() != null && !movieDto.getDetailImageUrls().isEmpty()) {
                String[] detailImageUrlArray = movieDto.getDetailImageUrls().split(",");
                List<String> detailImageUrlList = Arrays.asList(detailImageUrlArray);
                model.addAttribute("detailImageUrlList", detailImageUrlList);

                // 디버깅용 로그
                System.out.println("상세 이미지 목록: " + detailImageUrlList);
            }

            model.addAttribute("movie", movieDto);
            return "movies/edit";
        } else {
            return "redirect:/movies";
        }
    }

    // 영화 수정 처리
    @PostMapping("/{id}/edit")
    public String updateMovie(@PathVariable Long id,
                              @ModelAttribute("movie") MovieDto movieDto, // movieDto에는 hidden 필드의 기존 값들이 포함됨
                              @RequestParam(value = "mainImageFile", required = false) MultipartFile mainImageFile,
                              @RequestParam(value = "subImageFile", required = false) MultipartFile subImageFile,
                              @RequestParam(value = "detailImageFiles", required = false) MultipartFile[] detailImageFiles,
                              RedirectAttributes redirectAttributes) {

        // 현재 DTO에 있는 이미지 URL 값 (hidden 필드에서 넘어온 값)을 임시 저장
        String currentMainImageUrl = movieDto.getMainImageUrl();
        String currentSubImageUrls = movieDto.getSubImageUrls();
        String currentDetailImageUrls = movieDto.getDetailImageUrls();

        // 1. 메인 이미지 처리
        if (mainImageFile != null && !mainImageFile.isEmpty()) {
            // 새 파일이 업로드되면 기존 파일 교체
            movieDto.setMainImageUrl(saveFile(mainImageFile));
        } else if (currentMainImageUrl == null || currentMainImageUrl.isEmpty()) {
            // 새 파일 없고, hidden 필드 값이 비어있으면 (JS로 삭제된 경우) null로 설정
            movieDto.setMainImageUrl(null);
        }
        // else: 새 파일 없고, hidden 필드에 값이 있으면 (유지) movieDto.mainImageUrl은 그대로 유지됨

        // 2. 서브 이미지 처리
        if (subImageFile != null && !subImageFile.isEmpty()) {
            // 새 파일이 업로드되면 기존 파일 교체
            movieDto.setSubImageUrls(saveFile(subImageFile));
        } else if (currentSubImageUrls == null || currentSubImageUrls.isEmpty()) {
            // 새 파일 없고, hidden 필드 값이 비어있으면 (JS로 삭제된 경우) null로 설정
            movieDto.setSubImageUrls(null);
        }
        // else: 새 파일 없고, hidden 필드에 값이 있으면 (유지) movieDto.subImageUrls는 그대로 유지됨

        // 3. 상세 이미지 처리
        List<String> finalDetailImageUrls = new ArrayList<>();

        // 폼에서 넘어온 기존 상세 이미지 URL들 (JS에서 삭제된 것은 이미 제거되었어야 함)
        if (currentDetailImageUrls != null && !currentDetailImageUrls.isEmpty()) {
            finalDetailImageUrls.addAll(Arrays.asList(currentDetailImageUrls.split(",")));
        }

        // 새로 업로드된 상세 이미지들 추가
        if (detailImageFiles != null && detailImageFiles.length > 0) {
            for (MultipartFile file : detailImageFiles) {
                if (!file.isEmpty()) {
                    finalDetailImageUrls.add(saveFile(file));
                }
            }
        }
        // 최종 상세 이미지 URL 문자열 설정
        movieDto.setDetailImageUrls(finalDetailImageUrls.isEmpty() ? null : String.join(",", finalDetailImageUrls));

        // null 값 처리 (다른 숫자 필드 등)
        handleNullValues(movieDto);

        MovieDto updatedMovie = movieService.updateMovie(id, movieDto);

        if (updatedMovie != null) {
            redirectAttributes.addFlashAttribute("message", "영화가 성공적으로 수정되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "영화 수정에 실패했습니다.");
        }

        return "redirect:/movies/" + id;
    }

    // 영화 삭제
    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return "redirect:/movies";
    }

    // 영화 검색
    @GetMapping("/search")
    public String searchMovies(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(required = false) String status,
                               Model model,
                               @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, HttpSession session) {

        Page<MovieDto> movies;

        // 세션에서 로그인된 사용자 정보 가져오기 (다른 메서드와 일치시킴)
        Object sessionAttribute = session.getAttribute("loginMember");
        String currentMemberId = null;

        if (sessionAttribute instanceof Member) {
            Member loginMember = (Member) sessionAttribute;
            currentMemberId = loginMember.getMemberId();
            System.out.println("검색 - 로그인된 사용자 ID: " + currentMemberId);
        } else {
            currentMemberId = "test2"; // 임시 ID (다른 메서드와 일치시킴)
            System.out.println("검색 - 임시 ID 사용: " + currentMemberId);
        }

        // 키워드가 있는 경우 검색 수행
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드와 상영 상태를 함께 고려하여 검색
            if (status != null && !status.isEmpty()) {
                try {
                    Movie.ScreeningStatus screeningStatus = Movie.ScreeningStatus.valueOf(status);
                    movies = movieService.searchMoviesByKeywordAndStatus(keyword, screeningStatus, pageable, currentMemberId);
                } catch (IllegalArgumentException e) {
                    movies = movieService.searchMoviesByKeyword(keyword, pageable, currentMemberId);
                }
            } else {
                movies = movieService.searchMoviesByKeyword(keyword, pageable, currentMemberId);
            }
        } else {
            // 키워드가 없는 경우 상영 상태별 필터링만 수행
            if (status != null && !status.isEmpty()) {
                try {
                    Movie.ScreeningStatus screeningStatus = Movie.ScreeningStatus.valueOf(status);
                    movies = movieService.getMoviesByScreeningStatus(screeningStatus, pageable, currentMemberId);
                } catch (IllegalArgumentException e) {
                    movies = movieService.getMoviesPaginated(pageable, currentMemberId);
                }
            } else {
                movies = movieService.getMoviesPaginated(pageable, currentMemberId);
            }
        }

        // 다음 페이지 번호 계산
        int nextPageNumber = movies.getNumber() + 1;
        model.addAttribute("nextPageNumber", nextPageNumber);

        // 영화 목록의 각 영화에 대해 null 값 처리
        for (MovieDto movieDto : movies.getContent()) {
            handleNullValues(movieDto);
        }

        model.addAttribute("movies", movies);

        // 검색 키워드가 있는 경우에만 관련 정보 추가
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("keyword", keyword);
            model.addAttribute("title", "'" + keyword + "' 검색 결과");
        } else {
            model.addAttribute("title", "영화 목록");
        }

        model.addAttribute("timestamp", System.currentTimeMillis());
        model.addAttribute("currentStatus", status);

        // 페이지네이션을 위한 값들 추가
        model.addAttribute("hasPrevious", movies.hasPrevious());
        model.addAttribute("hasNext", movies.hasNext());
        model.addAttribute("prevPage", Math.max(0, movies.getNumber() - 1));
        model.addAttribute("nextPage", Math.min(movies.getTotalPages() - 1, movies.getNumber() + 1));

        // 페이지 번호 목록 생성
        int totalPages = movies.getTotalPages();
        if (totalPages > 0) {
            List<PageItem> pageNumbers = new ArrayList<>();
            int start = Math.max(0, movies.getNumber() - 2);
            int end = Math.min(totalPages - 1, movies.getNumber() + 2);
            for (int i = start; i <= end; i++) {
                pageNumbers.add(new PageItem(i, movies.getNumber() == i));
            }
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "movies/list"; // 검색 결과도 동일한 목록 템플릿 사용
    }

    private void handleNullValues(MovieDto movieDto) {
        // 숫자형 필드 null 처리
        if (movieDto.getRating() == null) {
            movieDto.setRating(BigDecimal.ZERO);
        }
        if (movieDto.getReservationRate() == null) {
            movieDto.setReservationRate(BigDecimal.ZERO);
        }
        if (movieDto.getAudienceCount() == null) {
            movieDto.setAudienceCount(BigInteger.ZERO);
        }
        if (movieDto.getLikesCount() == null) {
            movieDto.setLikesCount(0);
        }
        if (movieDto.getReviewCount() == null) {
            movieDto.setReviewCount(0);
        }
        if (movieDto.getRunningTime() == null) {
            movieDto.setRunningTime(0);
        }

        // 문자열 필드 null 처리
        if (movieDto.getTitle() == null) {
            movieDto.setTitle("");
        }
        if (movieDto.getContent() == null) {
            movieDto.setContent("");
        }
        if (movieDto.getDirector() == null) {
            movieDto.setDirector("");
        }
        if (movieDto.getActors() == null) {
            movieDto.setActors("");
        }
        if (movieDto.getGenre() == null) {
            movieDto.setGenre("");
        }
        if (movieDto.getAgeRating() == null) {
            movieDto.setAgeRating("");
        }
        if (movieDto.getMainImageUrl() == null) {
            movieDto.setMainImageUrl("");
        }
        if (movieDto.getSubImageUrls() == null) {
            movieDto.setSubImageUrls("");
        }
        if (movieDto.getDetailImageUrls() == null) {
            movieDto.setDetailImageUrls("");
        }
        if (movieDto.getVideoUrl() == null) {
            movieDto.setVideoUrl("");
        }

        // 날짜 필드 null 처리
        if (movieDto.getReleaseDate() == null) {
            movieDto.setReleaseDate(LocalDate.now());
        }
        if (movieDto.getCreatedAt() == null) {
            movieDto.setCreatedAt(LocalDateTime.now());
        }
        if (movieDto.getUpdatedAt() == null) {
            movieDto.setUpdatedAt(LocalDateTime.now());
        }
    }

    // 파일 저장 로직 (컨트롤러 내부에 private)
    private String saveFile(MultipartFile file) {
        // 허용할 이미지 MIME 타입 목록 (선택 사항: 유효성 검사 추가)
        List<String> allowedImageTypes = List.of(
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
        );

        if (file.isEmpty()) {
            throw new RuntimeException("업로드할 파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType.toLowerCase())) {
            throw new RuntimeException("이미지 파일만 업로드할 수 있습니다. (허용되지 않는 파일 형식: " + contentType + ")");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            String savedFileName = UUID.randomUUID().toString() + extension;

            // 실제 운영 환경에서는 외부 경로를 사용하거나 S3와 같은 클라우드 스토리지를 권장합니다.
            // 현재는 프로젝트 리소스 폴더에 저장합니다.
            String uploadDir = "src/main/resources/static/images/movie/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(savedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return savedFileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}