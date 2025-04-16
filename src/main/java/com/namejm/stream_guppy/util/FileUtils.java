package com.namejm.stream_guppy.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    public static Path getResolvedPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return Paths.get("").toAbsolutePath();
        }

        if (path.startsWith("/") || path.matches("^[A-Za-z]:[\\\\/].*")) {
            return Paths.get(path).toAbsolutePath();
        }

        if (path.startsWith("./")) {
            return Paths.get("").toAbsolutePath().resolve(path.substring(2));
        }

        return Paths.get("").toAbsolutePath().resolve(path);
    }
}
