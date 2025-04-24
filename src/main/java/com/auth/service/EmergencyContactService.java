package com.auth.service;

import com.auth.model.EmergencyContact;
import com.auth.repository.EmergencyContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmergencyContactService {

    @Autowired
    private EmergencyContactRepository emergencyContactRepository;

    public EmergencyContact createContact(EmergencyContact contact) {
        contact.setActive(true);
        return emergencyContactRepository.save(contact);
    }

    public List<EmergencyContact> getAllContacts() {
        return emergencyContactRepository.findAll();
    }

    public List<EmergencyContact> getContactsByDistrict(String district) {
        return emergencyContactRepository.findByDistrict(district);
    }

    public List<EmergencyContact> getContactsByDepartment(String department) {
        return emergencyContactRepository.findByDepartment(department);
    }

    public List<EmergencyContact> getContactsByDistrictAndDepartment(String district, String department) {
        return emergencyContactRepository.findByDistrictAndDepartment(district, department);
    }

    public List<EmergencyContact> getContactsByAdmin(String adminId) {
        return emergencyContactRepository.findByAddedBy(adminId);
    }

    public Optional<EmergencyContact> getContactById(String id) {
        return emergencyContactRepository.findById(id);
    }

    public EmergencyContact updateContact(String id, EmergencyContact contact) {
        if (emergencyContactRepository.existsById(id)) {
            contact.setId(id);
            return emergencyContactRepository.save(contact);
        }
        return null;
    }

    public void deleteContact(String id) {
        emergencyContactRepository.deleteById(id);
    }

    public EmergencyContact toggleContactStatus(String id) {
        Optional<EmergencyContact> contactOpt = emergencyContactRepository.findById(id);
        if (contactOpt.isPresent()) {
            EmergencyContact contact = contactOpt.get();
            contact.setActive(!contact.isActive());
            return emergencyContactRepository.save(contact);
        }
        return null;
    }
} 