package com.anhto.keycloak.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> authenticateUser(String username, String password) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("username", username);
        requestBody.add("password", password);
        requestBody.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Authentication failed");
        }
    }

    public String createUserInKeycloak(String username, String email, String firstName,
                                       String lastName, String password) {
        String token = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enabled", true);
        body.put("emailVerified", true);
        body.put("credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        )));

        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Extract user ID from location header
                String location = response.getHeaders().getFirst("Location");
                if (location != null) {
                    return location.substring(location.lastIndexOf("/") + 1);
                }
                throw new RuntimeException("Failed to extract user ID from response");
            } else {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Keycloak user creation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Token refresh failed");
        }
    }

    public void logoutUser(String refreshToken) {
        String logoutUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Logout failed");
        }
    }

    public Map<String, Object> parseJwtPayload(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid JWT token");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT token: " + e.getMessage());
        }
    }

    public String createClient(String clientId, String redirectUri) {
        String token = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/clients";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("clientId", clientId);
        body.put("protocol", "openid-connect");
        body.put("publicClient", false);
        body.put("enabled", true);
        body.put("redirectUris", List.of(redirectUri));

        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return "Client created successfully";
            } else {
                throw new RuntimeException("Failed to create client: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client: " + e.getMessage());
        }
    }

    private String getAdminToken() {
        String url = serverUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", "admin-cli");
        requestBody.add("username", adminUsername);
        requestBody.add("password", adminPassword);
        requestBody.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            } else {
                throw new RuntimeException("Failed to get admin token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get admin token: " + e.getMessage());
        }
    }
}