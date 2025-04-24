package com.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                logger.info("Creating upload directory at: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }
            logger.info("Upload directory is ready at: {}", uploadPath);
        } catch (IOException e) {
            logger.error("Could not create upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public List<String> uploadFiles(List<MultipartFile> files) {
        List<String> fileUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) {
                    logger.warn("Skipping empty file");
                    continue;
                }

                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                
                logger.info("Saving file to: {}", filePath);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                fileUrls.add("/uploads/" + fileName);
                logger.info("File saved successfully: {}", fileName);
            } catch (IOException e) {
                logger.error("Failed to store file: {}", e.getMessage());
                throw new RuntimeException("Failed to store file", e);
            }
        }
        return fileUrls;
    }

    public void deleteFiles(List<String> fileUrls) {
        for (String url : fileUrls) {
            try {
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                Path filePath = uploadPath.resolve(fileName);
                
                if (Files.deleteIfExists(filePath)) {
                    logger.info("File deleted successfully: {}", fileName);
                } else {
                    logger.warn("File not found for deletion: {}", fileName);
                }
            } catch (IOException e) {
                logger.error("Failed to delete file: {}", e.getMessage());
                throw new RuntimeException("Failed to delete file", e);
            }
        }
    }
} 