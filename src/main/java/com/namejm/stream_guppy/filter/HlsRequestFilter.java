package com.namejm.stream_guppy.filter;

import com.namejm.stream_guppy.service.StreamingService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(1)
public class HlsRequestFilter implements Filter {

    // 정규식: /hls/문자열/파일명 형태 매칭, streamKey 추출
    private static final Pattern HLS_PATH_PATTERN = Pattern.compile("^/hls/([^/]+)/.*$");

    @Autowired
    @Lazy
    private StreamingService streamingService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        Matcher matcher = HLS_PATH_PATTERN.matcher(path);

        if (matcher.matches()) {
            String streamKey = matcher.group(1); // 첫 번째 캡처 그룹 (streamKey)
            log.trace("HLS request detected for stream key: {}", streamKey);

            try {
                // 스트림 시작 또는 상태 업데이트 시도
                boolean streamReady = streamingService.startOrUpdateStream(streamKey).isPresent();

                if (!streamReady) {
                     log.warn("Stream {} is not available (not configured or inactive). Passing request.", streamKey);
                } else {
                     log.trace("Stream {} is active. Passing request to chain.", streamKey);
                }

            } catch (IOException e) {
                log.error("Error starting stream for key '{}': {}", streamKey, e.getMessage());
            }
        }

        // 다음 필터 또는 핸들러로 요청 전달
        chain.doFilter(request, response);
    }
}
