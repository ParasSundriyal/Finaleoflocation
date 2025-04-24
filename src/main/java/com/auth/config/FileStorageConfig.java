package com.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            // Use absolute path for container environments
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            // Create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                logger.info("Creating upload directory at: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }
            
            // Set appropriate permissions
            if (Files.exists(uploadPath)) {
                uploadPath.toFile().setWritable(true, false);
                uploadPath.toFile().setReadable(true, false);
            }
            
            logger.info("Upload directory is ready at: {}", uploadPath);
        } catch (IOException e) {
            logger.error("Could not create upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadAbsolutePath = uploadPath.toString().replace("\\", "/");
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/")
                .setCachePeriod(3600) // Cache for 1 hour
                .resourceChain(true);
    }
} 