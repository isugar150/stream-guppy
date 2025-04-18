package com.namejm.stream_guppy.service;

import com.namejm.stream_guppy.dto.ProcessDto;
import com.namejm.stream_guppy.repository.StreamRepository;
import com.namejm.stream_guppy.vo.StreamVO;
import com.namejm.stream_guppy.util.FileUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Service
@RequiredArgsConstructor
public class StreamingService {
    private final ConcurrentHashMap<String, ProcessDto> runningStreams = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final StreamRepository streamRepository;

    @Value("${stream_guppy.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${stream_guppy.hls.output.basePath:./hls_output}")
    private String hlsOutputBasePath;

    @Value("${stream_guppy.streaming.inactivityTimeoutSeconds:60}")
    private long inactivityTimeoutSeconds;

    @Value("${stream_guppy.ffmpeg.waitForM3u8TimeoutSeconds:30}") // m3u8 파일 생성 대기 시간 (초)
    private long waitForM3u8TimeoutSeconds;

    /**
     * 특정 스트림 키에 대한 HLS 스트리밍을 시작하거나, 이미 실행 중이면 정보를 반환하고 마지막 접근 시간을 갱신.
     * @param streamKey 스트림 식별 키
     * @return 실행 중인 스트림 정보 Optional (DB에 없거나 비활성화 시 비어있음)
     * @throws IOException FFmpeg 프로세스 시작 실패 시
     */
    public Optional<ProcessDto> startOrUpdateStream(String streamKey) throws IOException {
        ProcessDto existingProcessDto = runningStreams.get(streamKey);
        if (existingProcessDto != null && existingProcessDto.getProcess().isAlive()) {
            log.info("Stream '{}' is already running. Updating last accessed time.", streamKey);
            existingProcessDto.updateLastAccessedTime();
            return Optional.of(existingProcessDto);
        } else if (existingProcessDto != null) {
            log.warn("Found dead process entry for stream '{}'. Cleaning up.", streamKey);
            cleanupStreamResources(existingProcessDto);
            runningStreams.remove(streamKey);
        }

        StreamVO config = streamRepository.findByStreamKey(streamKey).orElse(null);

        Path outputDir = Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString(), streamKey);
        Files.createDirectories(outputDir);

        String m3u8Path = outputDir.resolve("stream.m3u8").toString();
        String tsSegmentPattern = outputDir.resolve("stream%03d.ts").toString();

        int frameRate = 25;
        List<String> command = new ArrayList<>(List.of(
                FileUtils.getResolvedPath(ffmpegPath).toString(),

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
        processBuilder.redirectErrorStream(true);
        // processBuilder.directory(outputDir.toFile()); // 작업 디렉토리 설정

        log.info("Starting FFmpeg for stream '{}': {}", streamKey, String.join(" ", command));
        Process process = processBuilder.start();
        executorService.submit(() -> logProcessOutput(process, streamKey));

        ProcessDto newProcessDto = new ProcessDto(process, streamKey, outputDir, Instant.now());

        boolean m3u8Ready = waitForM3u8File(Path.of(m3u8Path), process);

        if (m3u8Ready) {
            log.info(".m3u8 file found for stream '{}'. Proceeding.", streamKey);
            runningStreams.put(streamKey, newProcessDto);
            log.info("FFmpeg process started and .m3u8 confirmed for stream '{}'. PID: {}", streamKey, process.pid());
            return Optional.of(newProcessDto);
        } else {
            log.error("Failed to detect .m3u8 file for stream '{}' within timeout or process died.", streamKey);
            process.destroyForcibly();
            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for forced process termination.", e);
            }
            cleanupStreamResources(newProcessDto);
            return Optional.empty();
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
            if (!process.isAlive()) {
                log.warn("FFmpeg process died while waiting for .m3u8 file: {}", m3u8Path);
                return false;
            }
            if (Files.exists(m3u8Path) && Files.isReadable(m3u8Path)) {
                 try {
                     if (Files.size(m3u8Path) > 0) {
                         return true;
                     }
                 } catch (IOException e) {
                     log.warn("Error checking size of .m3u8 file '{}': {}", m3u8Path, e.getMessage());
                 }
            }
            try {
                Thread.sleep(200); // 200ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for .m3u8 file.", e);
                return false;
            }
        }
        log.warn("Timeout waiting for .m3u8 file: {}", m3u8Path);
        return false;
    }

    /**
     * 지정된 스트림 키의 마지막 접근 시간을 갱신.
     * @param streamKey 스트림 식별 키
     */
    public void updateStreamActivity(String streamKey) {
        ProcessDto processDto = runningStreams.get(streamKey);
        if (processDto != null && processDto.getProcess().isAlive()) {
            processDto.updateLastAccessedTime();
            log.info("Updated last accessed time for stream '{}'", streamKey);
        }
    }
    /**
     * 지정된 스트림을 중지하고 관련 리소스를 정리.
     * @param streamKey 중지할 스트림 키
     */
    public void stopStream(String streamKey) {
        ProcessDto processDto = runningStreams.remove(streamKey);
        if (processDto != null) {
            Process process = processDto.getProcess();
            if (process.isAlive()) {
                log.info("Stopping FFmpeg process for stream '{}' (PID: {})...", streamKey, process.pid());
                process.destroy();
                try {
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        log.warn("Forcefully stopping FFmpeg process for stream '{}'.", streamKey);
                        process.destroyForcibly();
                    }
                    log.info("FFmpeg process stopped for stream '{}'.", streamKey);
                } catch (InterruptedException e) {
                    log.error("Interrupted while stopping FFmpeg for stream '{}'. Forcing stop.", streamKey, e);
                    process.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
            }
            cleanupStreamResources(processDto);
        } else {
            log.info("Stream '{}' was not running or already removed.", streamKey);
             cleanupStreamResources(new ProcessDto(null, streamKey, Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString(), streamKey), Instant.now()));
        }
    }

    private void cleanupStreamResources(ProcessDto processDto) {
        if (processDto != null && processDto.getOutputDirectory() != null) {
            Path dir = processDto.getOutputDirectory();
            try {
                if (Files.exists(dir)) {
                    log.info("Cleaning up HLS files for stream '{}' in directory: {}", processDto.getStreamKey(), dir);
                     Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
//                        .peek(System.out::println)
                        .forEach(File::delete);

                    log.info("Successfully cleaned up directory for stream '{}'.", processDto.getStreamKey());
                }
            } catch (IOException e) {
                log.error("Failed to cleanup HLS files for stream '{}' in directory: {}", processDto.getStreamKey(), dir, e);
            }
        }
    }

    public void cleanupInactiveStreams() {
        log.info("Running inactivity check...");
        Instant cutoffTime = Instant.now().minusSeconds(inactivityTimeoutSeconds);
        runningStreams.forEach((key, info) -> {
            if (info.getLastAccessedTime().isBefore(cutoffTime)) {
                log.info("Stream '{}' inactive since {}. Stopping...", key, info.getLastAccessedTime());
                stopStream(key);
            }
        });
        if(runningStreams.size() > 0) {
             log.info("Currently active streams: {}", runningStreams.size());
        }
    }

    private void logProcessOutput(Process process, String streamKey) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg - {}]: {}", streamKey, line);
            }
        } catch (IOException e) {
             ProcessDto info = runningStreams.get(streamKey);
            if (info != null && info.getProcess() == process && process.isAlive()) {
               log.error("Error reading FFmpeg output for stream '{}'", streamKey, e);
             } else if (!e.getMessage().contains("Stream closed")) {
                 log.warn("IOException reading FFmpeg output after process stopped for stream '{}': {}", streamKey, e.getMessage());
             }
        } finally {
            ProcessDto info = runningStreams.get(streamKey);
            if (info != null && info.getProcess() == process && !process.isAlive()) {
                log.info("FFmpeg process for stream '{}' exited unexpectedly or finished.", streamKey);
                runningStreams.remove(streamKey);
                cleanupStreamResources(info);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down StreamingService. Stopping all streams...");
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
