package com.namejm.stream_guppy.scheduler;


import com.namejm.stream_guppy.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
@Component
public class InactivityMonitor {

    private final StreamingService streamingService;

    // 예: 30초마다 실행 (fixedDelayString 사용 권장)
    // "PT30S" = 30초, "PT1M" = 1분
    @Scheduled(fixedDelayString = "PT30S")
    public void checkAndCleanupInactiveStreams() {
        log.trace("Scheduler triggered: Checking for inactive streams...");
        streamingService.cleanupInactiveStreams();
    }
}