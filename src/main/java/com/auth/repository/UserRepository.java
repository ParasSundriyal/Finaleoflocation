package com.auth.repository;

import com.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(String role);
    List<User> findByRole(String role);
    List<User> findByRoleAndDistrict(String role, String district);
    long countByDistrict(String district);
    List<User> findByDistrict(String district);
} 