package com.anhto.keycloak.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("refresh_expires_in")
    private Integer refreshExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    private UserInfo user;

    @Data
    public static class UserInfo {
        private String sub;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
    }
}


