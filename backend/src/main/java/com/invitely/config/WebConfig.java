package com.invitely.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${invitely.asset-storage-path:uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve to absolute path and normalize for Windows/Linux
        String absolutePath = Paths.get(storagePath)
                .toAbsolutePath()
                .normalize()
                .toString()
                .replace("\\", "/");   // normalize Windows backslashes

        // Must end with /
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }

        String resourceLocation = "file:///" + absolutePath;

        log.info("Serving static uploads from: {}", resourceLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
