package com.oliveconnect.repository;

import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRolesContainingAndActiveTrue(Role role);
}
