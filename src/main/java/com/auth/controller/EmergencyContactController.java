package com.auth.controller;

import com.auth.model.EmergencyContact;
import com.auth.model.User;
import com.auth.service.EmergencyContactService;
import com.auth.repository.UserRepository;
import com.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-contacts")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:3000", "https://tehrilocationmapping.onrender.com"}, allowCredentials = "true")
public class EmergencyContactController {

    @Autowired
    private EmergencyContactService emergencyContactService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<EmergencyContact> createContact(
            @RequestBody EmergencyContact contact,
            @RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username).orElseThrow();
        contact.setAddedBy(user.getId());
        return ResponseEntity.ok(emergencyContactService.createContact(contact));
    }

    @GetMapping
    public ResponseEntity<List<EmergencyContact>> getAllContacts(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String department) {
        
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username).orElseThrow();
        
        // If district is not specified, use user's district for non-superadmin users
        if (district == null && !user.isSuperAdmin()) {
            district = user.getDistrict();
        }
        
        // If both district and department are specified
        if (district != null && department != null) {
            return ResponseEntity.ok(emergencyContactService.getContactsByDistrictAndDepartment(district, department));
        }
        
        // If only district is specified
        if (district != null) {
            return ResponseEntity.ok(emergencyContactService.getContactsByDistrict(district));
        }
        
        // If only department is specified
        if (department != null) {
            return ResponseEntity.ok(emergencyContactService.getContactsByDepartment(department));
        }
        
        // If no filters are specified and user is not superadmin, return district contacts
        if (!user.isSuperAdmin()) {
            return ResponseEntity.ok(emergencyContactService.getContactsByDistrict(user.getDistrict()));
        }
        
        // For superadmin without filters, return all contacts
        return ResponseEntity.ok(emergencyContactService.getAllContacts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyContact> getContactById(@PathVariable String id) {
        return emergencyContactService.getContactById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyContact> updateContact(
            @PathVariable String id,
            @RequestBody EmergencyContact contact,
            @RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username).orElseThrow();
        
        // Only allow superadmin or admin of the same district to update
        EmergencyContact existing = emergencyContactService.getContactById(id).orElse(null);
        if (existing != null && (user.isSuperAdmin() || user.getDistrict().equals(existing.getDistrict()))) {
            EmergencyContact updated = emergencyContactService.updateContact(id, contact);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username).orElseThrow();
        
        // Only allow superadmin or admin of the same district to delete
        EmergencyContact existing = emergencyContactService.getContactById(id).orElse(null);
        if (existing != null && (user.isSuperAdmin() || user.getDistrict().equals(existing.getDistrict()))) {
            emergencyContactService.deleteContact(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<EmergencyContact> toggleContactStatus(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username).orElseThrow();
        
        // Only allow superadmin or admin of the same district to toggle status
        EmergencyContact existing = emergencyContactService.getContactById(id).orElse(null);
        if (existing != null && (user.isSuperAdmin() || user.getDistrict().equals(existing.getDistrict()))) {
            EmergencyContact toggled = emergencyContactService.toggleContactStatus(id);
            return toggled != null ? ResponseEntity.ok(toggled) : ResponseEntity.notFound().build();
        }
        return ResponseEntity.badRequest().build();
    }
} 