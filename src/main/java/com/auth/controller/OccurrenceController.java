package com.auth.controller;

import com.auth.model.Occurrence;
import com.auth.model.User;
import com.auth.service.OccurrenceService;
import com.auth.repository.UserRepository;
import com.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/occurrences")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:3000", "https://tehrilocationmapping.onrender.com"}, allowCredentials = "true")
public class OccurrenceController {
    private static final Logger logger = LoggerFactory.getLogger(OccurrenceController.class);

    @Autowired
    private OccurrenceService occurrenceService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> createOccurrence(
            @RequestPart(value = "occurrence") String occurrenceJson,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @RequestHeader("Authorization") String token) {
        try {
            logger.info("Received occurrence creation request");
            logger.debug("Occurrence JSON: {}", occurrenceJson);
            
            // Extract user ID from token
            String userId = null;
            if (token != null && token.startsWith("Bearer ")) {
                String username = jwtUtil.extractUsername(token.substring(7));
                User user = userRepository.findByUsername(username).orElseThrow();
                userId = user.getId();
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules(); // This helps with LocalDateTime serialization
            Occurrence occurrence = mapper.readValue(occurrenceJson, Occurrence.class);
            
            // Validate required fields
            if (occurrence.getTitle() == null || occurrence.getTitle().trim().isEmpty()) {
                logger.warn("Title is required");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Title is required"));
            }
            
            if (occurrence.getDescription() == null || occurrence.getDescription().trim().isEmpty()) {
                logger.warn("Description is required");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Description is required"));
            }
            
            if (occurrence.getLatitude() == 0 || occurrence.getLongitude() == 0) {
                logger.warn("Location coordinates are required");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Location coordinates are required"));
            }

            if (photos == null || photos.isEmpty()) {
                logger.warn("At least one photo is required");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "At least one photo is required"));
            }

            // Validate photo types
            for (MultipartFile photo : photos) {
                String contentType = photo.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    logger.warn("Invalid file type: {}", contentType);
                    return ResponseEntity.badRequest()
                        .body(Map.of("message", "Only image files are allowed"));
                }
            }
            
            // Create the occurrence
            logger.info("Creating occurrence with title: {}", occurrence.getTitle());
            Occurrence savedOccurrence = occurrenceService.createOccurrence(occurrence, userId, photos);
            logger.info("Occurrence created successfully with ID: {}", savedOccurrence.getId());
            
            return ResponseEntity.ok(savedOccurrence);
            
        } catch (Exception e) {
            logger.error("Error creating occurrence", e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred while creating the occurrence";
            }
            return ResponseEntity.badRequest()
                .body(Map.of("message", errorMessage));
        }
    }

    @GetMapping
    public ResponseEntity<List<Occurrence>> getAllOccurrences(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username).orElseThrow();
            return ResponseEntity.ok(occurrenceService.getAllOccurrences(user.getId()));
        } catch (Exception e) {
            logger.error("Error fetching occurrences", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Occurrence> getOccurrenceById(@PathVariable String id) {
        return occurrenceService.getOccurrenceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Occurrence>> getOccurrencesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(occurrenceService.getOccurrencesByStatus(status));
    }

    @GetMapping("/reporter/{reporterId}")
    public ResponseEntity<List<Occurrence>> getOccurrencesByReporter(@PathVariable String reporterId) {
        return ResponseEntity.ok(occurrenceService.getOccurrencesByReporterId(reporterId));
    }

    @GetMapping("/map/active")
    public ResponseEntity<List<Occurrence>> getActiveOccurrencesOnMap() {
        return ResponseEntity.ok(occurrenceService.getActiveOccurrencesOnMap());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Occurrence> updateOccurrenceStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam String verifiedBy,
            @RequestParam(required = false) String verificationNotes) {
        return ResponseEntity.ok(occurrenceService.updateOccurrenceStatus(id, status, verifiedBy, verificationNotes));
    }

    @PutMapping("/{id}/map-visibility")
    public ResponseEntity<Occurrence> toggleMapVisibility(@PathVariable String id) {
        return ResponseEntity.ok(occurrenceService.toggleMapVisibility(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOccurrence(@PathVariable String id) {
        occurrenceService.deleteOccurrence(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/photos/{photoIndex}")
    public ResponseEntity<byte[]> getOccurrencePhoto(
            @PathVariable String id,
            @PathVariable int photoIndex) {
        byte[] photoData = occurrenceService.getOccurrencePhoto(id, photoIndex);
        String contentType = occurrenceService.getOccurrencePhotoContentType(id, photoIndex);

        if (photoData != null && contentType != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(photoData);
        }
        return ResponseEntity.notFound().build();
    }
} 