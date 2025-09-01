package com.anhto.keycloak.service;

import com.anhto.keycloak.dto.RegisterRequest;
import com.anhto.keycloak.entity.UserEntity;
import com.anhto.keycloak.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
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

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setFirstName(firstName);
        registerRequest.setLastName(lastName);
        registerRequest.setPassword(password);

        keycloakService.createKeycloakUser(registerRequest);
    }

    public UserEntity getUserFromToken(String token) {
        return (UserEntity) keycloakService.parseJwtPayload(token);
    }

}