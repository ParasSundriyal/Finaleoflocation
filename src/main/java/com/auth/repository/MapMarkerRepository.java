package com.auth.repository;

import com.auth.model.MapMarker;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MapMarkerRepository extends MongoRepository<MapMarker, String> {
    List<MapMarker> findByEnabled(boolean enabled);
    List<MapMarker> findByMarkerType(String markerType);
    List<MapMarker> findByCreatedBy(String createdBy);
    List<MapMarker> findByCreatedByAndEnabled(String createdBy, boolean enabled);
    long countByDistrict(String district);
    List<MapMarker> findByDistrict(String district);
    List<MapMarker> findByDistrictAndCreatedAtAfter(String district, LocalDateTime date);
    List<MapMarker> findByDistrictAndEnabled(String district, boolean enabled);
} 