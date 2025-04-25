package com.auth.controller;

import com.auth.model.MapMarker;
import com.auth.model.User;
import com.auth.repository.MapMarkerRepository;
import com.auth.repository.UserRepository;
import com.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markers")
@CrossOrigin(origins = "*")
public class MarkerController {

    @Autowired
    private MapMarkerRepository markerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAllMarkers(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                // Extract username from token
                String username = jwtUtil.extractUsername(token.substring(7));
                User user = userRepository.findByUsername(username).orElseThrow();

                // If user is an admin, return only their markers
                if (user.isAdmin()) {
                    return ResponseEntity.ok(markerRepository.findByCreatedByAndEnabled(user.getId(), true));
                }
                // If user is a superadmin, return all markers
                else if (user.isSuperAdmin()) {
                    return ResponseEntity.ok(markerRepository.findByEnabled(true));
                }
            }
            // For public access or non-admin users, return all enabled markers
            return ResponseEntity.ok(markerRepository.findByEnabled(true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching markers: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMarker(@RequestBody MapMarker marker, @RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username).orElseThrow();

            // Only ADMIN and SUPERADMIN can create markers
            if (!user.isAdmin() && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only admins can create markers"));
            }

            // Set the district from the admin's district
            marker.setDistrict(user.getDistrict());
            marker.setCreatedBy(user.getId());
            marker.setCreatedAt(LocalDateTime.now());
            marker.setEnabled(true);

            MapMarker savedMarker = markerRepository.save(marker);
            return ResponseEntity.ok(savedMarker);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error creating marker: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMarker(@PathVariable String id, @RequestBody MapMarker marker, 
                                        @RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username).orElseThrow();

            // Only ADMIN and SUPERADMIN can update markers
            if (!user.isAdmin() && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only admins can update markers"));
            }

            MapMarker existingMarker = markerRepository.findById(id).orElseThrow();
            
            // Only creator or superadmin can update
            if (!existingMarker.getCreatedBy().equals(user.getId()) && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "You can only update your own markers"));
            }

            // Verify the marker belongs to the admin's district
            if (!existingMarker.getDistrict().equals(user.getDistrict()) && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "You can only update markers in your district"));
            }

            existingMarker.setName(marker.getName());
            existingMarker.setDescription(marker.getDescription());
            existingMarker.setLatitude(marker.getLatitude());
            existingMarker.setLongitude(marker.getLongitude());
            existingMarker.setMarkerType(marker.getMarkerType());
            existingMarker.setLastModified(LocalDateTime.now());
            existingMarker.setLastModifiedBy(user.getId());
            // District remains unchanged

            MapMarker updatedMarker = markerRepository.save(existingMarker);
            return ResponseEntity.ok(updatedMarker);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error updating marker: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMarker(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username).orElseThrow();

            // Only ADMIN and SUPERADMIN can delete markers
            if (!user.isAdmin() && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only admins can delete markers"));
            }

            MapMarker marker = markerRepository.findById(id).orElseThrow();
            
            // Only creator or superadmin can delete
            if (!marker.getCreatedBy().equals(user.getId()) && !user.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "You can only delete your own markers"));
            }

            markerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Marker deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting marker: " + e.getMessage()));
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MapMarker>> getMarkersByType(@PathVariable String type) {
        return ResponseEntity.ok(markerRepository.findByMarkerType(type));
    }

    @GetMapping("/district/{district}")
    public ResponseEntity<?> getMarkersByDistrict(@PathVariable String district) {
        try {
            List<MapMarker> markers = markerRepository.findByDistrictAndEnabled(district, true);
            return ResponseEntity.ok(markers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching markers: " + e.getMessage()));
        }
    }
} 