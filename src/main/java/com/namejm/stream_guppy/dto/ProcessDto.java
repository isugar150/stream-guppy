package com.namejm.stream_guppy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class ProcessDto {
    private final Process process;
    private final String streamKey;
    private final Path outputDirectory;
    private volatile Instant lastAccessedTime;

    public void updateLastAccessedTime() {
        this.lastAccessedTime = Instant.now();
    }
}