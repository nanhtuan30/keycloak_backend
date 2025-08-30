package com.anhto.keycloak.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private String phone;
}
