package com.namejm.stream_guppy.filter;

import com.namejm.stream_guppy.service.StreamingService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
@Order(1)
public class HlsRequestFilter implements Filter {

    // 정규식: /hls/문자열/파일명 형태 매칭, streamKey 추출
    private static final Pattern HLS_PATH_PATTERN = Pattern.compile("^/hls/([^/]+)/.*$");

    private final StreamingService streamingService;

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
                     // DB에 없거나 비활성 스트림이면 404 처리하고 필터 체인 중단
                    // ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND, "Stream not found or not active: " + streamKey);
                     // return;
                     // 또는 그냥 통과시켜서 static handler가 404 처리하게 둘 수도 있음
                     log.warn("Stream {} is not available (not configured or inactive). Passing request.", streamKey);
                } else {
                    // 스트림이 준비되었거나 실행 중이면 마지막 접근 시간 갱신 (startOrUpdateStream 내부에서 처리됨)
                    // streamingService.updateStreamActivity(streamKey); // 명시적 호출도 가능
                     log.trace("Stream {} is active. Passing request to chain.", streamKey);
                }

            } catch (IOException e) {
                log.error("Error starting stream for key '{}': {}", streamKey, e.getMessage());
                // 에러 발생 시 500 처리
                 // ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process stream request");
                 // return;
                 // 또는 그냥 통과
            }
            // ★ 중요: FFmpeg이 파일을 생성할 시간을 벌기 위해 필터에서 대기하지 않음!
            // 첫 요청 시 m3u8 파일이 아직 없으면 static handler가 404를 반환할 수 있음.
            // HLS 플레이어는 보통 자동으로 재시도하므로 다음 요청에서는 파일이 존재할 가능성이 높음.
        }

        // 다음 필터 또는 핸들러로 요청 전달
        chain.doFilter(request, response);
    }
}