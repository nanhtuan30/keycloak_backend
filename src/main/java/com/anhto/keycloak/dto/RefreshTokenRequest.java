package com.anhto.keycloak.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
