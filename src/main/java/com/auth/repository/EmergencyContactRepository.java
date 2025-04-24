package com.auth.repository;

import com.auth.model.EmergencyContact;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EmergencyContactRepository extends MongoRepository<EmergencyContact, String> {
    List<EmergencyContact> findByDistrict(String district);
    List<EmergencyContact> findByDepartment(String department);
    List<EmergencyContact> findByDistrictAndDepartment(String district, String department);
    List<EmergencyContact> findByAddedBy(String adminId);
    List<EmergencyContact> findByIsActive(boolean isActive);
} 