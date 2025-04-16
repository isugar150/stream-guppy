package com.namejm.stream_guppy.service;

import com.namejm.stream_guppy.config.ProcessInfo;
import com.namejm.stream_guppy.config.StreamConfig;
import com.namejm.stream_guppy.util.FileUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class StreamingService {
    private final ConcurrentHashMap<String, ProcessInfo> runningStreams = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${hls.output.basePath:./hls_output}")
    private String hlsOutputBasePath;

    @Value("${streaming.inactivityTimeoutSeconds:60}")
    private long inactivityTimeoutSeconds;

    @Value("${ffmpeg.waitForM3u8TimeoutSeconds:10}") // m3u8 파일 생성 대기 시간 (초)
    private long waitForM3u8TimeoutSeconds;


    /**
     * 특정 스트림 키에 대한 HLS 스트리밍을 시작하거나, 이미 실행 중이면 정보를 반환하고 마지막 접근 시간을 갱신.
     * @param streamKey 스트림 식별 키
     * @return 실행 중인 스트림 정보 Optional (DB에 없거나 비활성화 시 비어있음)
     * @throws IOException FFmpeg 프로세스 시작 실패 시
     */
    public Optional<ProcessInfo> startOrUpdateStream(String streamKey) throws IOException {
        // 1. 현재 실행 중인지 확인
        ProcessInfo existingProcessInfo = runningStreams.get(streamKey);
        if (existingProcessInfo != null && existingProcessInfo.getProcess().isAlive()) {
            log.debug("Stream '{}' is already running. Updating last accessed time.", streamKey);
            existingProcessInfo.updateLastAccessedTime();
            return Optional.of(existingProcessInfo);
        } else if (existingProcessInfo != null) {
            // 프로세스가 죽었지만 맵에 남아있는 경우 제거
            log.warn("Found dead process entry for stream '{}'. Cleaning up.", streamKey);
            cleanupStreamResources(existingProcessInfo);
            runningStreams.remove(streamKey);
        }

        // 2. DB에서 활성화된 스트림 정보 조회
        StreamConfig config = new StreamConfig();
        config.setId(1L);
        config.setStreamKey("CCTV1");
        config.setName("CCTV1");
        config.setRtspUrl("rtsp://210.99.70.120:1935/live/cctv007.stream");
        config.setUseYn(true);

        // 3. FFmpeg 프로세스 시작
        Path outputDir = Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString(), streamKey);
        Files.createDirectories(outputDir); // 개별 스트림 출력 디렉토리 생성

        String m3u8Path = outputDir.resolve("stream.m3u8").toString();
        String tsSegmentPattern = outputDir.resolve("stream%03d.ts").toString();

        int frameRate = 25;
        List<String> command = new ArrayList<>(List.of( // 수정 가능하도록 ArrayList로 생성
                ffmpegPath,

                // === 입력 관련 옵션 (RTSP 안정성 및 분석 강화) ===
                "-fflags", "+genpts",          // PTS (Presentation Timestamp)가 없을 경우 생성 시도
                "-rtsp_transport", "tcp",    // RTSP 전송 프로토콜을 TCP로 강제 (UDP보다 안정적)
                "-analyzeduration", "10M",   // 스트림 분석 시간 늘리기 (10초 = 10M)
                "-probesize", "10M",         // 스트림 분석을 위한 데이터 크기 늘리기 (10MB = 10M)
                "-timeout", "5000000",       // RTSP 연결 및 데이터 수신 타임아웃 설정 (5초 = 5000000 마이크로초)
                "-r", String.valueOf(frameRate), // <<< 입력 프레임 속도 강제 지정 (예: "25")

                // === 입력 소스 ===
                "-i", config.getRtspUrl(),

                // === 비디오 처리 (호환성 중심) ===
                "-c:v", "libx264",           // 비디오 코덱: H.264로 강제 재인코딩 (호환성 향상)
                "-preset", "veryfast",       // 인코딩 속도/압축률 조절
                "-profile:v", "baseline",    // H.264 프로파일 설정 (구형 기기 호환성)
                "-level:v", "3.0",           // H.264 레벨 설정
                "-pix_fmt", "yuv420p",       // 픽셀 포맷 지정 (보편적)
                "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", // <<< 해상도 짝수 맞춤 (Java List에서는 내부 따옴표 불필요)
                "-r", String.valueOf(frameRate), // <<< 출력 프레임 속도 강제 지정 (입력과 동일하게)

                // === 오디오 처리 비활성화 ===
                "-an",                       // <<< 오디오 비활성화 (Audio No)

                // === HLS 출력 설정 ===
                "-f", "hls",
                "-hls_time", "4",
                "-hls_list_size", "5",
                "-hls_flags", "delete_segments+independent_segments", // 독립 세그먼트 생성 및 오래된 세그먼트 삭제
                "-hls_segment_type", "mpegts", // 세그먼트 타입 명시
                "-hls_segment_filename", tsSegmentPattern, // TS 세그먼트 파일 이름 패턴
                m3u8Path                     // 메인 M3U8 플레이리스트 파일 경로
        ));

        // if (audioExists) {
        //     command.remove("-an"); // -an 제거
        //     // 필요한 오디오 옵션 추가
        //     command.add(command.indexOf("-f") -1, "-ac"); // -f 앞에 오디오 옵션 추가 (순서는 중요하지 않을 수 있음)
        //     command.add(command.indexOf("-f") -1, "2");
        //     command.add(command.indexOf("-f") -1, "-b:a");
        //     command.add(command.indexOf("-f") -1, "128k");
        //     command.add(command.indexOf("-f") -1, "-c:a");
        //     command.add(command.indexOf("-f") -1, "aac");
        // }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 에러 스트림을 표준 출력으로
        // processBuilder.directory(outputDir.toFile()); // 작업 디렉토리 설정 (선택 사항)

        log.info("Starting FFmpeg for stream '{}': {}", streamKey, String.join(" ", command));
        Process process = processBuilder.start();

        ProcessInfo newProcessInfo = new ProcessInfo(process, streamKey, outputDir, Instant.now());

        boolean m3u8Ready = waitForM3u8File(Path.of(m3u8Path), process);

        if (m3u8Ready) {
            log.info(".m3u8 file found for stream '{}'. Proceeding.", streamKey);
            runningStreams.put(streamKey, newProcessInfo);
            executorService.submit(() -> logProcessOutput(process, streamKey)); // 로그 처리 시작
            log.info("FFmpeg process started and .m3u8 confirmed for stream '{}'. PID: {}", streamKey, process.pid());
            return Optional.of(newProcessInfo);
        } else {
            log.error("Failed to detect .m3u8 file for stream '{}' within timeout or process died.", streamKey);
            // 프로세스 강제 종료 및 정리
            process.destroyForcibly();
            try {
                process.waitFor(1, TimeUnit.SECONDS); // 잠시 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for forced process termination.", e);
            }
            cleanupStreamResources(newProcessInfo); // 디렉토리 정리 시도
            return Optional.empty(); // 실패했으므로 빈 Optional 반환
        }
    }


    /**
     * 지정된 경로에 .m3u8 파일이 생성될 때까지 대기하거나 프로세스가 종료되면 중단합니다.
     * @param m3u8Path 확인할 .m3u8 파일 경로
     * @param process 감시할 FFmpeg 프로세스
     * @return 파일이 제한 시간 내에 생성되면 true, 아니면 false
     */
    private boolean waitForM3u8File(Path m3u8Path, Process process) {
        Instant startTime = Instant.now();
        Duration timeout = Duration.ofSeconds(waitForM3u8TimeoutSeconds);

        while (Duration.between(startTime, Instant.now()).compareTo(timeout) < 0) {
            // 1. 프로세스가 죽었는지 먼저 확인
            if (!process.isAlive()) {
                log.warn("FFmpeg process died while waiting for .m3u8 file: {}", m3u8Path);
                return false;
            }
            // 2. 파일이 존재하는지 확인
            if (Files.exists(m3u8Path) && Files.isReadable(m3u8Path)) {
                 // 추가 검사: 파일 크기가 0보다 큰지 (간혹 빈 파일이 먼저 생길 수 있음)
                 try {
                     if (Files.size(m3u8Path) > 0) {
                         return true;
                     }
                 } catch (IOException e) {
                     log.warn("Error checking size of .m3u8 file '{}': {}", m3u8Path, e.getMessage());
                     // 크기 확인 중 오류 발생 시 일단 존재한다고 간주하거나, 잠시 더 기다릴 수 있음
                 }
            }
            // 3. 잠시 대기 후 다시 시도
            try {
                Thread.sleep(200); // 200ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for .m3u8 file.", e);
                return false;
            }
        }
        // 타임아웃 도달
        log.warn("Timeout waiting for .m3u8 file: {}", m3u8Path);
        return false;
    }

    /**
     * 지정된 스트림 키의 마지막 접근 시간을 갱신.
     * @param streamKey 스트림 식별 키
     */
    public void updateStreamActivity(String streamKey) {
        ProcessInfo processInfo = runningStreams.get(streamKey);
        if (processInfo != null && processInfo.getProcess().isAlive()) {
            processInfo.updateLastAccessedTime();
            log.trace("Updated last accessed time for stream '{}'", streamKey);
        }
    }
    /**
     * 지정된 스트림을 중지하고 관련 리소스를 정리.
     * @param streamKey 중지할 스트림 키
     */
    public void stopStream(String streamKey) {
        ProcessInfo processInfo = runningStreams.remove(streamKey); // 맵에서 제거 먼저 시도
        if (processInfo != null) {
            Process process = processInfo.getProcess();
            if (process.isAlive()) {
                log.info("Stopping FFmpeg process for stream '{}' (PID: {})...", streamKey, process.pid());
                process.destroy(); // SIGTERM
                try {
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        log.warn("Forcefully stopping FFmpeg process for stream '{}'.", streamKey);
                        process.destroyForcibly(); // SIGKILL
                    }
                    log.info("FFmpeg process stopped for stream '{}'.", streamKey);
                } catch (InterruptedException e) {
                    log.error("Interrupted while stopping FFmpeg for stream '{}'. Forcing stop.", streamKey, e);
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
            cleanupStreamResources(processInfo);
        } else {
            log.debug("Stream '{}' was not running or already removed.", streamKey);
            // 맵에 없더라도 혹시 모를 파일 정리를 위해 시도할 수 있음
             cleanupStreamResources(new ProcessInfo(null, streamKey, Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString(), streamKey), Instant.now()));
        }
    }

    // 스트림 관련 파일/디렉토리 정리
    private void cleanupStreamResources(ProcessInfo processInfo) {
        if (processInfo != null && processInfo.getOutputDirectory() != null) {
            Path dir = processInfo.getOutputDirectory();
            try {
                if (Files.exists(dir)) {
                    log.info("Cleaning up HLS files for stream '{}' in directory: {}", processInfo.getStreamKey(), dir);
                    // FileSystemUtils.deleteRecursively(dir); // Spring 유틸리티 사용
                    // Java NIO Files 사용 (더 표준적)
                     Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .peek(System.out::println)
                        .forEach(File::delete);

                    // Files.deleteIfExists(dir); // 디렉토리 자체 삭제 (위 walk로 내용물 삭제 후 가능)

                    log.info("Successfully cleaned up directory for stream '{}'.", processInfo.getStreamKey());
                }
            } catch (IOException e) {
                log.error("Failed to cleanup HLS files for stream '{}' in directory: {}", processInfo.getStreamKey(), dir, e);
            }
        }
    }


    // 비활성 스트림 정리 (스케줄러에서 호출)
    public void cleanupInactiveStreams() {
        log.trace("Running inactivity check...");
        Instant cutoffTime = Instant.now().minusSeconds(inactivityTimeoutSeconds);
        runningStreams.forEach((key, info) -> {
            if (info.getLastAccessedTime().isBefore(cutoffTime)) {
                log.info("Stream '{}' inactive since {}. Stopping...", key, info.getLastAccessedTime());
                // 별도 스레드 또는 현재 스레드에서 stopStream 호출
                // ConcurrentModificationException 방지 위해 키 목록 복사 후 처리 권장
                stopStream(key);
            }
        });
         // 주기적으로 맵 크기 로깅 (디버깅용)
        if(runningStreams.size() > 0) {
             log.debug("Currently active streams: {}", runningStreams.size());
        }
    }

    // FFmpeg 로그 처리
    private void logProcessOutput(Process process, String streamKey) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.trace("[FFmpeg - {}]: {}", streamKey, line); // 로그 레벨 조절 (Trace or Debug)
            }
        } catch (IOException e) {
             ProcessInfo info = runningStreams.get(streamKey);
             // 프로세스가 정상 종료/중지 된 후 발생하는 Stream closed 에러는 무시
            if (info != null && info.getProcess() == process && process.isAlive()) {
               log.error("Error reading FFmpeg output for stream '{}'", streamKey, e);
             } else if (!e.getMessage().contains("Stream closed")) {
                 log.warn("IOException reading FFmpeg output after process stopped for stream '{}': {}", streamKey, e.getMessage());
             }
        } finally {
            // 프로세스 종료 시 맵에서 제거 (stopStream 호출 없이 비정상 종료된 경우 대비)
            ProcessInfo info = runningStreams.get(streamKey);
            if (info != null && info.getProcess() == process && !process.isAlive()) {
                log.info("FFmpeg process for stream '{}' exited unexpectedly or finished.", streamKey);
                runningStreams.remove(streamKey); // stopStream이 호출되지 않았을 수 있으므로 여기서 제거
                cleanupStreamResources(info); // 자원 정리
            }
        }
    }

    // 애플리케이션 종료 시 모든 스트림 중지
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down StreamingService. Stopping all streams...");
        // 키 목록 복사 후 처리 (ConcurrentModificationException 방지)
        List<String> keys = List.copyOf(runningStreams.keySet());
        keys.forEach(this::stopStream);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("StreamingService shutdown complete.");
    }
}
