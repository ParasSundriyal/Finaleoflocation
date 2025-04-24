package com.auth.controller;

import com.auth.model.EmergencyContact;
import com.auth.service.EmergencyContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-contacts")
public class EmergencyContactController {

    @Autowired
    private EmergencyContactService emergencyContactService;

    @PostMapping
    public ResponseEntity<EmergencyContact> createContact(@RequestBody EmergencyContact contact) {
        return ResponseEntity.ok(emergencyContactService.createContact(contact));
    }

    @GetMapping
    public ResponseEntity<List<EmergencyContact>> getAllContacts() {
        return ResponseEntity.ok(emergencyContactService.getAllContacts());
    }

    @GetMapping("/district/{district}")
    public ResponseEntity<List<EmergencyContact>> getContactsByDistrict(@PathVariable String district) {
        return ResponseEntity.ok(emergencyContactService.getContactsByDistrict(district));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<EmergencyContact>> getContactsByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(emergencyContactService.getContactsByDepartment(department));
    }

    @GetMapping("/district/{district}/department/{department}")
    public ResponseEntity<List<EmergencyContact>> getContactsByDistrictAndDepartment(
            @PathVariable String district, @PathVariable String department) {
        return ResponseEntity.ok(emergencyContactService.getContactsByDistrictAndDepartment(district, department));
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<List<EmergencyContact>> getContactsByAdmin(@PathVariable String adminId) {
        return ResponseEntity.ok(emergencyContactService.getContactsByAdmin(adminId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyContact> getContactById(@PathVariable String id) {
        return emergencyContactService.getContactById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyContact> updateContact(@PathVariable String id, @RequestBody EmergencyContact contact) {
        EmergencyContact updated = emergencyContactService.updateContact(id, contact);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable String id) {
        emergencyContactService.deleteContact(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<EmergencyContact> toggleContactStatus(@PathVariable String id) {
        EmergencyContact toggled = emergencyContactService.toggleContactStatus(id);
        return toggled != null ? ResponseEntity.ok(toggled) : ResponseEntity.notFound().build();
    }
} 