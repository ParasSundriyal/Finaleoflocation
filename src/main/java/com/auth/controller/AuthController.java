package com.auth.controller;

import com.auth.model.User;
import com.auth.model.MapMarker;
import com.auth.repository.UserRepository;
import com.auth.repository.MapMarkerRepository;
import com.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:8080", 
    "https://tehrilocationmapping.onrender.com",
    "https://policelocationmapping.onrender.com"
}, allowCredentials = "true")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MapMarkerRepository markerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            logger.info("Received signup request for username: {}, email: {}", user.getUsername(), user.getEmail());

            // Validate required fields
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                logger.warn("Signup failed: Username is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
            }
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                logger.warn("Signup failed: Email is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.warn("Signup failed: Password is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }
            if (user.getDistrict() == null || user.getDistrict().trim().isEmpty()) {
                logger.warn("Signup failed: District is required");
                return ResponseEntity.badRequest().body(Map.of("message", "District is required"));
            }

            // Check if username already exists
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                logger.warn("Signup failed: Username {} is already taken", user.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken"));
            }

            // Check if email already exists
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                logger.warn("Signup failed: Email {} is already registered", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered"));
            }

            // Validate email format
            if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                logger.warn("Signup failed: Invalid email format for {}", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid email format"));
            }

            // Validate mobile number format (10 digits)
            if (user.getMobile() == null || !user.getMobile().matches("^\\d{10}$")) {
                logger.warn("Signup failed: Invalid mobile number format for user {}", user.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "Mobile number must be 10 digits"));
            }

            // Validate password strength (at least 6 characters)
            if (user.getPassword().length() < 6) {
                logger.warn("Signup failed: Password too short for user {}", user.getUsername());
                return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters long"));
            }

            // Set default values
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            
            // Encode password
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully: {}", savedUser.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", savedUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during registration for user: " + user.getUsername(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            
            logger.info("Received login request for username: {}", username);

            // Check if user exists and get stored details
            User storedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Login failed: User {} not found", username);
                    return new RuntimeException("Invalid username or password");
                });

            logger.info("Found user in database. Username: {}, Role: {}", storedUser.getUsername(), storedUser.getRole());

            try {
                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
                logger.info("Attempting authentication for user: {}", username);

                // Attempt authentication
                Authentication authentication = authenticationManager.authenticate(authToken);
                logger.info("Authentication successful for user: {}", username);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String token = jwtUtil.generateToken(userDetails.getUsername());

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("role", storedUser.getRole());
                
                logger.info("Login successful for user: {}, Role: {}", username, storedUser.getRole());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Authentication failed for user: {}, Error: {}", username, e.getMessage());
                logger.error("Authentication error details:", e);
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid username or password"));
            }
        } catch (Exception e) {
            logger.error("Error during login process: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody User admin, @RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can create admins
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can create admins"));
            }

            // Validate district
            if (admin.getDistrict() == null || admin.getDistrict().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "District is required for admin"));
            }

            // Set admin role and other details
            admin.setRole("ADMIN");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setCreatedBy(currentUser.getId());
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));

            // Set a default mobile number if not provided
            if (admin.getMobile() == null || admin.getMobile().trim().isEmpty()) {
                admin.setMobile("0000000000");
            }

            User savedAdmin = userRepository.save(admin);
            logger.info("Admin created successfully by superadmin: {} for district: {}", savedAdmin.getUsername(), savedAdmin.getDistrict());

            return ResponseEntity.ok(Map.of(
                "message", "Admin created successfully",
                "username", savedAdmin.getUsername(),
                "district", savedAdmin.getDistrict()
            ));
        } catch (Exception e) {
            logger.error("Error creating admin", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error creating admin: " + e.getMessage()));
        }
    }

    @GetMapping("/admins")
    public ResponseEntity<?> getAdmins(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can view admin list
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can view admin list"));
            }

            logger.info("Current superadmin details - Username: {}, District: {}, Role: {}", 
                currentUser.getUsername(), 
                currentUser.getDistrict(), 
                currentUser.getRole());

            // Get all admins instead of filtering by district
            List<Map<String, Object>> admins = userRepository.findByRole("ADMIN")
                .stream()
                .map(admin -> {
                    Map<String, Object> adminData = new HashMap<>();
                    adminData.put("id", admin.getId());
                    adminData.put("username", admin.getUsername());
                    adminData.put("email", admin.getEmail());
                    adminData.put("district", admin.getDistrict());
                    adminData.put("enabled", admin.isEnabled());
                    adminData.put("area", admin.getArea());
                    adminData.put("createdAt", admin.getCreatedAt());
                    logger.info("Found admin - Username: {}, District: {}, Enabled: {}", 
                        admin.getUsername(), 
                        admin.getDistrict(),
                        admin.isEnabled());
                    return adminData;
                })
                .collect(Collectors.toList());

            logger.info("Total admins found: {}", admins.size());
            return ResponseEntity.ok(admins);
        } catch (Exception e) {
            logger.error("Error fetching admin list", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching admin list: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable String id, @RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can delete admins
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can delete admins"));
            }

            User adminToDelete = userRepository.findById(id).orElseThrow();
            
            // Verify the user is an admin
            if (!"ADMIN".equals(adminToDelete.getRole())) {
                return ResponseEntity.badRequest().body(Map.of("message", "User is not an admin"));
            }

            userRepository.deleteById(id);
            logger.info("Admin deleted successfully by superadmin: {}", adminToDelete.getUsername());

            return ResponseEntity.ok(Map.of("message", "Admin deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting admin", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting admin: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String username = jwtUtil.extractUsername(token);
            logger.info("Fetching user details for username: {}", username);

            return userRepository.findByUsername(username)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("role", user.getRole());
                    response.put("district", user.getDistrict());
                    response.put("createdAt", user.getCreatedAt());
                    logger.info("Found user details - Username: {}, District: {}, Role: {}", 
                        user.getUsername(), 
                        user.getDistrict(), 
                        user.getRole());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching user details", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching user details: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can view statistics
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can view statistics"));
            }

            long totalUsers = userRepository.count();
            long totalAdmins = userRepository.countByRole("ADMIN");
            long totalMarkers = markerRepository.count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("totalAdmins", totalAdmins);
            statistics.put("totalMarkers", totalMarkers);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error fetching statistics", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/markers-by-admin")
    public ResponseEntity<?> getMarkersByAdmin(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can view markers by admin
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can view markers by admin"));
            }

            List<User> admins = userRepository.findByRole("ADMIN");
            List<Map<String, Object>> markerStats = new ArrayList<>();

            for (User admin : admins) {
                List<MapMarker> adminMarkers = markerRepository.findByCreatedBy(admin.getId());
                
                // Only include admins who have created markers
                if (!adminMarkers.isEmpty()) {
                    Map<String, Object> adminStat = new HashMap<>();
                    adminStat.put("adminName", admin.getUsername());
                    adminStat.put("adminEmail", admin.getEmail());
                    adminStat.put("totalMarkers", adminMarkers.size());
                    
                    // Get last marker date
                    LocalDateTime lastMarkerDate = adminMarkers.stream()
                            .map(MapMarker::getCreatedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    adminStat.put("lastMarkerDate", lastMarkerDate);

                    // Count marker types
                    Map<String, Long> markerTypes = adminMarkers.stream()
                            .collect(Collectors.groupingBy(
                                MapMarker::getMarkerType,
                                Collectors.counting()
                            ));
                    adminStat.put("markerTypes", markerTypes);

                    markerStats.add(adminStat);
                }
            }

            return ResponseEntity.ok(markerStats);
        } catch (Exception e) {
            logger.error("Error fetching markers by admin", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching markers by admin: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String token) {
        try {
            // Extract username from token
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Only superadmin can view all users
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only superadmin can view all users"));
            }

            List<Map<String, Object>> usersList = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("role", user.getRole());
                    userMap.put("createdAt", user.getCreatedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(usersList);
        } catch (Exception e) {
            logger.error("Error fetching users", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching users: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/district/{district}")
    public ResponseEntity<?> getDistrictStatistics(
            @PathVariable String district,
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Verify the admin belongs to the requested district
            if (!currentUser.isAdmin() || !currentUser.getDistrict().equals(district)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unauthorized to view this district's statistics"));
            }

            // Get district statistics
            long totalUsers = userRepository.countByDistrict(district);
            long totalMarkers = markerRepository.countByDistrict(district);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("totalMarkers", totalMarkers);
            statistics.put("area", "10,000 sq km"); // This should be fetched from a district database

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error fetching district statistics", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching district statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/users/district/{district}")
    public ResponseEntity<?> getDistrictUsers(
            @PathVariable String district,
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Verify the admin belongs to the requested district
            if (!currentUser.isAdmin() || !currentUser.getDistrict().equals(district)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unauthorized to view this district's users"));
            }

            List<Map<String, Object>> users = userRepository.findByDistrict(district).stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("area", user.getArea());
                    userMap.put("createdAt", user.getCreatedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching district users", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching district users: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics/district/{district}/reports")
    public ResponseEntity<?> getDistrictReports(
            @PathVariable String district,
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username).orElseThrow();

            // Verify the admin belongs to the requested district
            if (!currentUser.isAdmin() || !currentUser.getDistrict().equals(district)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unauthorized to view this district's reports"));
            }

            // Get activity data (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<MapMarker> recentMarkers = markerRepository.findByDistrictAndCreatedAtAfter(district, sevenDaysAgo);

            // Process activity data
            Map<String, Object> activityData = new HashMap<>();
            List<String> labels = new ArrayList<>();
            List<Long> markerCounts = new ArrayList<>();
            
            // Group markers by day
            Map<LocalDate, Long> dailyCounts = recentMarkers.stream()
                .collect(Collectors.groupingBy(
                    marker -> marker.getCreatedAt().toLocalDate(),
                    Collectors.counting()
                ));

            // Fill in the last 7 days
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                labels.add(date.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")));
                markerCounts.add(dailyCounts.getOrDefault(date, 0L));
            }

            activityData.put("labels", labels);
            activityData.put("markers", markerCounts);

            // Get marker distribution
            Map<String, Long> markerDistribution = markerRepository.findByDistrict(district).stream()
                .collect(Collectors.groupingBy(
                    MapMarker::getMarkerType,
                    Collectors.counting()
                ));

            Map<String, Object> response = new HashMap<>();
            response.put("activityData", activityData);
            response.put("markerDistribution", markerDistribution);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching district reports", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error fetching district reports: " + e.getMessage()));
        }
    }
} 