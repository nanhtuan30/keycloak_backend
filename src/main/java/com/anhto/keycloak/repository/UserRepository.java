package com.anhto.keycloak.repository;

import com.anhto.keycloak.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public abstract class UserRepository implements JpaRepository<UserEntity, Long> {
    public Optional<UserEntity> findByUsername(String username) {
        return null;
    }

    public Optional<UserEntity> findByEmail(String email) {
        return null;
    }

    public Optional<UserEntity> findByKeycloakId(String keycloakId) {
        return null;
    }

    boolean existsByUsername(String username) {
        return false;
    }

    boolean existsByEmail(String email) {
        return false;
    }
}
