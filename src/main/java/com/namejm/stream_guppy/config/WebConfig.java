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
        Path hlsPath = Paths.get(FileUtils.getResolvedPath(hlsOutputBasePath).toString()).toAbsolutePath();
        String resourceLocation = "file:" + hlsPath + "/";

        log.info("Mapping /hls/** to resource location: {}", resourceLocation);

        registry.addResourceHandler("/hls/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0);

        log.info("Static resource handler for HLS configured.");
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/hls/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);

        log.info("CORS settings configured for /hls/** path.");
    }
}