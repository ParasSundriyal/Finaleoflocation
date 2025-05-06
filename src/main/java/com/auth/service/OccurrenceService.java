package com.auth.service;

import com.auth.model.Occurrence;
import com.auth.model.User;
import com.auth.repository.OccurrenceRepository;
import com.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OccurrenceService {

    @Autowired
    private OccurrenceRepository occurrenceRepository;

    @Autowired
    private GridFSService gridFSService;

    @Autowired
    private UserService userService;

    public List<Occurrence> getAllOccurrences(String userId) {
        User currentUser = userService.getUserById(userId);
        if (currentUser.getRole().equals("ADMIN")) {
            return occurrenceRepository.findByDistrict(currentUser.getDistrict());
        }
        return occurrenceRepository.findAll();
    }

    public Occurrence createOccurrence(Occurrence occurrence, String userId, List<MultipartFile> photos) {
        User currentUser = userService.getUserById(userId);
        occurrence.setReporterId(userId);
        occurrence.setDistrict(currentUser.getDistrict());
        occurrence.setReportedAt(LocalDateTime.now());
        occurrence.setStatus("PENDING");
        occurrence.setActiveOnMap(false);

        // Handle photo uploads
        if (photos != null && !photos.isEmpty()) {
            List<String> photoIds = gridFSService.storeFiles(photos);
            occurrence.setPhotoIds(photoIds);
        }

        return occurrenceRepository.save(occurrence);
    }

    public Optional<Occurrence> getOccurrenceById(String id) {
        return occurrenceRepository.findById(id);
    }

    public List<Occurrence> getOccurrencesByStatus(String status) {
        return occurrenceRepository.findByStatus(status);
    }

    public List<Occurrence> getOccurrencesByReporterId(String reporterId) {
        return occurrenceRepository.findByReporterId(reporterId);
    }

    public List<Occurrence> getActiveOccurrencesOnMap() {
        return occurrenceRepository.findByStatusAndActiveOnMap("VERIFIED", true);
    }

    public Occurrence updateOccurrenceStatus(String id, String status, String verifiedBy, String verificationNotes) {
        Optional<Occurrence> occurrenceOpt = occurrenceRepository.findById(id);
        if (occurrenceOpt.isPresent()) {
            Occurrence occurrence = occurrenceOpt.get();
            occurrence.setStatus(status);
            occurrence.setVerifiedBy(verifiedBy);
            occurrence.setVerificationNotes(verificationNotes);
            occurrence.setVerifiedAt(LocalDateTime.now());
            
            // If verified, automatically show on map
            if ("VERIFIED".equals(status)) {
                occurrence.setActiveOnMap(true);
            }
            
            return occurrenceRepository.save(occurrence);
        }
        return null;
    }

    public Occurrence toggleMapVisibility(String id) {
        Optional<Occurrence> occurrenceOpt = occurrenceRepository.findById(id);
        if (occurrenceOpt.isPresent()) {
            Occurrence occurrence = occurrenceOpt.get();
            occurrence.setActiveOnMap(!occurrence.isActiveOnMap());
            return occurrenceRepository.save(occurrence);
        }
        return null;
    }

    public void deleteOccurrence(String id) {
        Optional<Occurrence> occurrenceOpt = occurrenceRepository.findById(id);
        if (occurrenceOpt.isPresent()) {
            Occurrence occurrence = occurrenceOpt.get();
            // Delete associated photos from GridFS
            if (occurrence.getPhotoIds() != null && !occurrence.getPhotoIds().isEmpty()) {
                gridFSService.deleteFiles(occurrence.getPhotoIds());
            }
            occurrenceRepository.deleteById(id);
        }
    }

    public byte[] getOccurrencePhoto(String occurrenceId, int photoIndex) {
        Optional<Occurrence> occurrenceOpt = occurrenceRepository.findById(occurrenceId);
        if (occurrenceOpt.isPresent()) {
            Occurrence occurrence = occurrenceOpt.get();
            if (occurrence.getPhotoIds() != null && photoIndex < occurrence.getPhotoIds().size()) {
                return gridFSService.getFile(occurrence.getPhotoIds().get(photoIndex));
            }
        }
        return null;
    }

    public String getOccurrencePhotoContentType(String occurrenceId, int photoIndex) {
        Optional<Occurrence> occurrenceOpt = occurrenceRepository.findById(occurrenceId);
        if (occurrenceOpt.isPresent()) {
            Occurrence occurrence = occurrenceOpt.get();
            if (occurrence.getPhotoIds() != null && photoIndex < occurrence.getPhotoIds().size()) {
                return gridFSService.getContentType(occurrence.getPhotoIds().get(photoIndex));
            }
        }
        return null;
    }

    public List<Occurrence> findRecentlyVerifiedOccurrences(LocalDateTime since) {
        return occurrenceRepository.findByStatusAndVerifiedAtGreaterThanEqual("VERIFIED", since);
    }

    public List<Occurrence> findByStatusAndReportedAtGreaterThanEqual(String status, java.time.LocalDateTime since) {
        return occurrenceRepository.findByStatusAndReportedAtGreaterThanEqual(status, since);
    }
} 