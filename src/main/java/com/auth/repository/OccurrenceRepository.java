package com.auth.repository;

import com.auth.model.Occurrence;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface OccurrenceRepository extends MongoRepository<Occurrence, String> {
    List<Occurrence> findByStatus(String status);
    List<Occurrence> findByReporterId(String reporterId);
    List<Occurrence> findByStatusAndVerifiedBy(String status, String verifiedBy);
    List<Occurrence> findByStatusAndActiveOnMap(String status, boolean activeOnMap);
    List<Occurrence> findByDistrict(String district);
    List<Occurrence> findByStatusAndVerifiedAtGreaterThanEqual(String status, LocalDateTime verifiedAt);
} 