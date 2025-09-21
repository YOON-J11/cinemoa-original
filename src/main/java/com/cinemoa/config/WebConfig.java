package com.cinemoa.config;

import com.cinemoa.interceptor.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//웹 설정 클래스 (인터셉터 등록, 리소스 핸들러, CORS 설정 등)
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/mypage/**", "/reservation/payment") // 마이페이지, 결제 페이지는 로그인 필요
                .excludePathPatterns(
                        "/member/**", // 회원가입/로그인 등은 예외
                        "/mypage/withdrawalSuccess",  // 탈퇴 성공 페이지는 예외
                        "/ticketing/**" // 비로그인 유저도 좌석선택까지는 가능
                        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스는 클래스패스에서 서빙
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "classpath:/static/images/",  // 기본 프로필 이미지가 들어있는 곳
                        "classpath:/static/"
                )
                .setCachePeriod(0);

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

    }


}
