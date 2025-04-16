package com.namejm.stream_guppy.config;


import com.namejm.stream_guppy.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${hls.output.basePath:./hls_output}")
    private String hlsOutputBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // HLS 파일 요청 경로 (/hls/**) 와 실제 파일 시스템 경로 매핑
        // Path 객체를 사용하여 절대 경로를 얻는 것이 더 안정적일 수 있습니다.
        Path hlsPath = Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString()).toAbsolutePath();
        String resourceLocation = "file:" + hlsPath + "/"; // 경로 끝에 '/' 추가

        log.info("Mapping /hls/** to resource location: {}", resourceLocation);

        registry.addResourceHandler("/hls/**") // 웹 요청 경로 패턴
                .addResourceLocations(resourceLocation) // 실제 파일 위치
                .setCachePeriod(0); // HLS 파일은 자주 변경되므로 캐싱 비활성화

        log.info("Static resource handler for HLS configured.");
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/hls/**") // HLS 관련 경로(/hls/ 로 시작하는 모든 경로)에 CORS 적용
                .allowedOrigins("*") // 모든 출처 허용 (개발 시 편리하지만 보안에 유의)
                .allowedMethods("GET", "OPTIONS") // HLS 요청에 필요한 메소드 (OPTIONS는 Preflight 요청 처리용)
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(false) // HLS는 보통 인증 정보(쿠키 등) 불필요
                .maxAge(3600); // Preflight 요청 결과 캐시 시간 (초 단위, 1시간)

        log.info("CORS settings configured for /hls/** path.");
    }
}