package com.anhto.keycloak.service;

import com.anhto.keycloak.entity.UserEntity;
import com.anhto.keycloak.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeycloakService keycloakService;

    @Transactional
    public void registerUser(String username, String email, String firstName,
                             String lastName, String password) {
        // Check if user already exists in local database
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Create user in Keycloak
        String keycloakUserId = keycloakService.createUserInKeycloak(
                username, email, firstName, lastName, password);

        // Save user to local database
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setKeycloakId(keycloakUserId);

        userRepository.save(user);
    }

    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserEntity getUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}