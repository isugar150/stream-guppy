package com.namejm.stream_guppy.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.time.Instant;

@Getter
@AllArgsConstructor // Lombok 사용 시
public class ProcessInfo {
    private final Process process;
    private final String streamKey;
    private final Path outputDirectory;
    private volatile Instant lastAccessedTime;

    public void updateLastAccessedTime() {
        this.lastAccessedTime = Instant.now();
    }
}